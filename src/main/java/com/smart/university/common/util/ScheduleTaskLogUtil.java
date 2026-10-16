package com.smart.university.common.util;

import com.smart.university.common.context.TaskContext;
import com.smart.university.common.enums.TaskCodeEnum;
import com.smart.university.common.enums.TaskExecuteStatusEnum;
import com.smart.university.common.enums.TriggerTypeEnum;
import com.smart.university.domain.entity.ScheduleTaskLogDO;
import com.smart.university.service.ScheduleTaskLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * 定时任务执行模板，统一完成「抢锁 → 落 RUNNING 日志 → 执行 → 落 SUCCESS/FAILED 日志 → 释放锁」。
 * 方法体整体 try-catch，异常不逃逸到调度线程池
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleTaskLogUtil {

    /**
     * 异常摘要最大长度
     */
    private static final int MAX_ERROR_SUMMARY_LENGTH = 1000;

    private final ScheduleTaskLogService scheduleTaskLogService;

    private final TaskLockUtil taskLockUtil;

    /**
     * 分布式锁租约时长（秒）
     */
    @Value("${smart-university.schedule.lock-lease-seconds:300}")
    private long lockLeaseSeconds;

    /**
     * 执行无统计口径的任务，日志计数全部记 0
     *
     * @param taskCode    任务编码
     * @param triggerType 触发方式
     * @param executable  任务执行体
     * @return 抢到锁并执行返回 true，未抢到锁跳过返回 false
     */
    public boolean runWithLog(TaskCodeEnum taskCode, TriggerTypeEnum triggerType, Runnable executable) {
        if (executable == null) {
            return false;
        }
        return runWithLog(taskCode, triggerType, () -> {
            executable.run();
            return TaskContext.empty();
        });
    }

    /**
     * 执行任务并记录执行日志
     *
     * @param taskCode    任务编码
     * @param triggerType 触发方式
     * @param executable  任务执行体，返回本轮统计上下文
     * @return 抢到锁并执行返回 true，未抢到锁跳过返回 false
     */
    public boolean runWithLog(TaskCodeEnum taskCode, TriggerTypeEnum triggerType,
                              Supplier<TaskContext> executable) {
        if (taskCode == null) {
            return false;
        }
        TriggerTypeEnum trigger = triggerType == null ? TriggerTypeEnum.SCHEDULED : triggerType;
        if (!taskLockUtil.tryLock(taskCode, lockLeaseSeconds)) {
            log.debug("任务未抢到分布式锁，本轮跳过，任务编码：{}", taskCode.getCode());
            return false;
        }
        LocalDateTime startTime = LocalDateTime.now();
        try {
            Long logId = saveRunningLog(taskCode, trigger, startTime);
            TaskContext context = TaskContext.empty();
            Throwable error = null;
            if (executable == null) {
                context.appendError("任务执行体为空");
            } else {
                try {
                    TaskContext result = executable.get();
                    if (result != null) {
                        context = result;
                    }
                } catch (Throwable ex) {
                    error = ex;
                    context.appendError(buildErrorSummary(ex));
                    log.error("定时任务执行异常，任务编码：{}", taskCode.getCode(), ex);
                }
            }
            finishLog(logId, taskCode, trigger, startTime, context, error);
            return true;
        } catch (Throwable ex) {
            log.error("定时任务日志落库异常，任务编码：{}", taskCode.getCode(), ex);
            return true;
        } finally {
            taskLockUtil.unlock(taskCode);
        }
    }

    /**
     * 落一条执行中日志，落库失败不影响任务执行
     *
     * @param taskCode   任务编码
     * @param trigger    触发方式
     * @param startTime  开始时间
     * @return 日志 ID，落库失败返回 null
     */
    private Long saveRunningLog(TaskCodeEnum taskCode, TriggerTypeEnum trigger, LocalDateTime startTime) {
        ScheduleTaskLogDO requestParam = new ScheduleTaskLogDO();
        requestParam.setTaskCode(taskCode.getCode());
        requestParam.setTaskName(taskCode.getTaskName());
        requestParam.setTriggerType(trigger);
        requestParam.setStatus(TaskExecuteStatusEnum.RUNNING);
        requestParam.setStartTime(startTime);
        requestParam.setScanCount(0);
        requestParam.setSuccessCount(0);
        requestParam.setFailCount(0);
        requestParam.setInstanceId(taskLockUtil.getInstanceId());
        try {
            scheduleTaskLogService.saveScheduleTaskLog(requestParam);
            return requestParam.getId();
        } catch (Exception ex) {
            log.warn("定时任务执行日志落库失败，任务编码：{}，异常摘要：{}", taskCode.getCode(), ex.getMessage());
            return null;
        }
    }

    /**
     * 回填执行结果与耗时，标记最终状态
     *
     * @param logId     日志 ID，为空时跳过回填
     * @param taskCode  任务编码
     * @param trigger   触发方式
     * @param startTime 开始时间
     * @param context   执行上下文
     * @param error     执行异常，无异常时为 null
     */
    private void finishLog(Long logId, TaskCodeEnum taskCode, TriggerTypeEnum trigger,
                           LocalDateTime startTime, TaskContext context, Throwable error) {
        if (logId == null) {
            return;
        }
        LocalDateTime endTime = LocalDateTime.now();
        ScheduleTaskLogDO requestParam = new ScheduleTaskLogDO();
        requestParam.setId(logId);
        requestParam.setTaskCode(taskCode.getCode());
        requestParam.setTaskName(taskCode.getTaskName());
        requestParam.setTriggerType(trigger);
        requestParam.setStatus(resolveFinalStatus(context, error));
        requestParam.setStartTime(startTime);
        requestParam.setEndTime(endTime);
        requestParam.setDurationMs(Duration.between(startTime, endTime).toMillis());
        requestParam.setScanCount(context.getScanCountOrDefault());
        requestParam.setSuccessCount(context.getSuccessCountOrDefault());
        requestParam.setFailCount(context.getFailCountOrDefault());
        requestParam.setErrorSummary(context.getErrorSummary());
        requestParam.setInstanceId(taskLockUtil.getInstanceId());
        try {
            scheduleTaskLogService.updateScheduleTaskLog(requestParam);
        } catch (Exception ex) {
            log.warn("定时任务执行日志回填失败，任务编码：{}，日志ID：{}，异常摘要：{}",
                    taskCode.getCode(), logId, ex.getMessage());
        }
    }

    /**
     * 计算任务的最终状态：执行异常或全部失败记为 FAILED，其余记为 SUCCESS
     *
     * @param context 执行上下文
     * @param error   执行异常，无异常时为 null
     * @return 最终执行状态
     */
    private TaskExecuteStatusEnum resolveFinalStatus(TaskContext context, Throwable error) {
        if (error != null) {
            return TaskExecuteStatusEnum.FAILED;
        }
        if (context.getFailCountOrDefault() > 0 && context.getSuccessCountOrDefault() == 0) {
            return TaskExecuteStatusEnum.FAILED;
        }
        return TaskExecuteStatusEnum.SUCCESS;
    }

    /**
     * 构建异常摘要，超出长度时截断
     *
     * @param ex 异常
     * @return 异常摘要
     */
    private String buildErrorSummary(Throwable ex) {
        String summary = ex.getClass().getSimpleName() + "：" + ex.getMessage();
        return summary.length() > MAX_ERROR_SUMMARY_LENGTH
                ? summary.substring(0, MAX_ERROR_SUMMARY_LENGTH)
                : summary;
    }
}
