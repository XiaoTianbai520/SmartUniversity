package com.smart.university.service.impl;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.enums.TaskCodeEnum;
import com.smart.university.common.enums.TriggerTypeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.common.util.ScheduleTaskLogUtil;
import com.smart.university.domain.dto.req.ScheduleTaskLogQueryReqDTO;
import com.smart.university.domain.dto.resp.ScheduleTaskLogRespDTO;
import com.smart.university.domain.dto.resp.ScheduleTaskRespDTO;
import com.smart.university.domain.dto.resp.ScheduleTaskRunRespDTO;
import com.smart.university.domain.entity.ScheduleTaskLogDO;
import com.smart.university.mapper.ScheduleTaskLogMapper;
import com.smart.university.service.ScheduleTaskManageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 定时任务管理服务实现（41.1 ~ 41.3）
 *
 * <p>任务开关与调度表达式统一从 `smart-university.schedule.tasks.*` 读取，不在代码里写死；
 * 手动触发同样走 {@link ScheduleTaskLogUtil} 抢分布式锁与落日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleTaskManageServiceImpl implements ScheduleTaskManageService {

    /**
     * 配置项前缀
     */
    private static final String TASK_CONFIG_PREFIX = "smart-university.schedule.tasks.";

    /**
     * 日志扫描上限，用于在既有 Mapper 能力内完成状态筛选与内存分页
     */
    private static final int LOG_SCAN_LIMIT = 500;

    private final ScheduleTaskLogMapper scheduleTaskLogMapper;

    private final ScheduleTaskLogUtil scheduleTaskLogUtil;

    private final Environment environment;

    private final SelectionBatchStatusTask selectionBatchStatusTask;

    private final WaitlistPromoteScanTask waitlistPromoteScanTask;

    private final StatisticsSnapshotTask statisticsSnapshotTask;

    private final DashboardCacheRefreshTask dashboardCacheRefreshTask;

    private final CreditWarningTask creditWarningTask;

    private final DataAuditTask dataAuditTask;

    private final SemesterArchiveTask semesterArchiveTask;

    @Override
    public List<ScheduleTaskRespDTO> listScheduleTask() {
        List<ScheduleTaskRespDTO> result = new ArrayList<>();
        for (TaskCodeEnum each : TaskCodeEnum.values()) {
            ScheduleTaskRespDTO record = new ScheduleTaskRespDTO();
            record.setTaskCode(each.getCode());
            record.setTaskName(each.getTaskName());
            record.setEnabled(Boolean.TRUE.equals(readEnabled(each)));
            record.setScheduleExpr(resolveScheduleExpr(each));
            fillLatestLog(record, each);
            result.add(record);
        }
        return result;
    }

    @Override
    public PageResult<ScheduleTaskLogRespDTO> pageScheduleTaskLog(String taskCode,
                                                                  ScheduleTaskLogQueryReqDTO requestParam) {
        TaskCodeEnum taskCodeEnum = TaskCodeEnum.getByCode(taskCode);
        if (taskCodeEnum == null) {
            throw new BizException(ResultCodeEnum.SCHEDULE_TASK_NOT_EXIST);
        }
        List<ScheduleTaskLogRespDTO> records = scheduleTaskLogMapper
                .listScheduleTaskLogByTaskCode(taskCodeEnum.getCode(), LOG_SCAN_LIMIT).stream()
                .filter(each -> matchStatus(each, requestParam.getStatus()))
                .map(this::convertToLogRespDTO)
                .toList();
        int currentPage = requestParam.getCurrentPage();
        int limit = requestParam.getLimit();
        int fromIndex = Math.min((currentPage - 1) * limit, records.size());
        int toIndex = Math.min(fromIndex + limit, records.size());
        return new PageResult<>(new ArrayList<>(records.subList(fromIndex, toIndex)), currentPage, limit,
                records.size());
    }

    @Override
    public ScheduleTaskRunRespDTO runScheduleTask(String taskCode) {
        TaskCodeEnum taskCodeEnum = TaskCodeEnum.getByCode(taskCode);
        if (taskCodeEnum == null) {
            throw new BizException(ResultCodeEnum.SCHEDULE_TASK_NOT_EXIST);
        }
        if (!Boolean.TRUE.equals(readEnabled(taskCodeEnum))) {
            throw new BizException(ResultCodeEnum.SCHEDULE_TASK_DISABLED);
        }
        Supplier<com.smart.university.common.context.TaskContext> runner = resolveRunner(taskCodeEnum);
        if (runner == null) {
            throw new BizException(ResultCodeEnum.SCHEDULE_TASK_NOT_EXIST, "该任务暂不支持手动触发");
        }
        boolean started = scheduleTaskLogUtil.runWithLog(taskCodeEnum, TriggerTypeEnum.MANUAL, runner);
        if (!started) {
            throw new BizException(ResultCodeEnum.SCHEDULE_TASK_RUNNING);
        }
        return buildRunRespDTO(taskCodeEnum);
    }

    /**
     * 读取任务开关，未配置时默认启用
     *
     * @param taskCodeEnum 任务编码
     * @return 是否启用
     */
    private Boolean readEnabled(TaskCodeEnum taskCodeEnum) {
        return environment.getProperty(TASK_CONFIG_PREFIX + resolveConfigKey(taskCodeEnum) + ".enabled",
                Boolean.class, Boolean.TRUE);
    }

    /**
     * 解析任务的调度表达式描述：优先 cron，其次 fixedDelay，事件型任务返回说明文案
     *
     * @param taskCodeEnum 任务编码
     * @return 调度表达式描述
     */
    private String resolveScheduleExpr(TaskCodeEnum taskCodeEnum) {
        String configKey = resolveConfigKey(taskCodeEnum);
        String cron = environment.getProperty(TASK_CONFIG_PREFIX + configKey + ".cron");
        if (cron != null && !cron.isEmpty()) {
            return cron;
        }
        Long fixedDelay = environment.getProperty(TASK_CONFIG_PREFIX + configKey + ".fixed-delay", Long.class);
        if (fixedDelay != null) {
            return "fixedDelay=" + fixedDelay;
        }
        return taskCodeEnum.getDefaultPeriod();
    }

    /**
     * 任务编码到配置项键名的映射
     *
     * @param taskCodeEnum 任务编码
     * @return 配置项键名
     */
    private String resolveConfigKey(TaskCodeEnum taskCodeEnum) {
        Map<TaskCodeEnum, String> configKeyMap = new HashMap<>();
        configKeyMap.put(TaskCodeEnum.BATCH_STATUS_TASK, "batch-status");
        configKeyMap.put(TaskCodeEnum.WAITLIST_SCAN_TASK, "waitlist-scan");
        configKeyMap.put(TaskCodeEnum.STAT_SNAPSHOT_TASK, "stat-snapshot");
        configKeyMap.put(TaskCodeEnum.DASHBOARD_REFRESH_TASK, "dashboard-refresh");
        configKeyMap.put(TaskCodeEnum.CREDIT_WARNING_TASK, "credit-warning");
        configKeyMap.put(TaskCodeEnum.SCORE_STAT_EVENT, "score-stat-event");
        configKeyMap.put(TaskCodeEnum.DATA_AUDIT_TASK, "data-audit");
        configKeyMap.put(TaskCodeEnum.SEMESTER_ARCHIVE_TASK, "semester-archive");
        configKeyMap.put(TaskCodeEnum.CACHE_RECONCILE_TASK, "cache-reconcile");
        configKeyMap.put(TaskCodeEnum.SELECTION_CACHE_WARMUP_TASK, "selection-cache-warmup");
        return configKeyMap.getOrDefault(taskCodeEnum, taskCodeEnum.getCode().toLowerCase());
    }

    /**
     * 解析任务执行器，W1 / W2 归属的任务与事件型任务返回 null
     *
     * @param taskCodeEnum 任务编码
     * @return 任务执行器，不支持手动触发时返回 null
     */
    private Supplier<com.smart.university.common.context.TaskContext> resolveRunner(TaskCodeEnum taskCodeEnum) {
        return switch (taskCodeEnum) {
            case BATCH_STATUS_TASK -> selectionBatchStatusTask::runManually;
            case WAITLIST_SCAN_TASK -> waitlistPromoteScanTask::runManually;
            case STAT_SNAPSHOT_TASK -> statisticsSnapshotTask::runManually;
            case DASHBOARD_REFRESH_TASK -> dashboardCacheRefreshTask::runManually;
            case CREDIT_WARNING_TASK -> creditWarningTask::runManually;
            case DATA_AUDIT_TASK -> dataAuditTask::runManually;
            case SEMESTER_ARCHIVE_TASK -> semesterArchiveTask::runManually;
            default -> null;
        };
    }

    /**
     * 填充任务最近一次执行情况
     *
     * @param record        任务出参
     * @param taskCodeEnum  任务编码
     */
    private void fillLatestLog(ScheduleTaskRespDTO record, TaskCodeEnum taskCodeEnum) {
        ScheduleTaskLogDO latestLogDO = scheduleTaskLogMapper.getLatestScheduleTaskLog(taskCodeEnum.getCode());
        if (latestLogDO == null) {
            return;
        }
        record.setLastStatus(latestLogDO.getStatus() == null ? null : latestLogDO.getStatus().name());
        record.setLastStartTime(latestLogDO.getStartTime());
        record.setLastEndTime(latestLogDO.getEndTime());
        record.setLastDurationMs(latestLogDO.getDurationMs());
        record.setLastScanCount(latestLogDO.getScanCount());
        record.setLastSuccessCount(latestLogDO.getSuccessCount());
        record.setLastFailCount(latestLogDO.getFailCount());
        record.setLastInstanceId(latestLogDO.getInstanceId());
    }

    /**
     * 构建手动触发结果，读取刚刚落库的执行日志
     *
     * @param taskCodeEnum 任务编码
     * @return 手动触发结果
     */
    private ScheduleTaskRunRespDTO buildRunRespDTO(TaskCodeEnum taskCodeEnum) {
        ScheduleTaskLogDO latestLogDO = scheduleTaskLogMapper.getLatestScheduleTaskLog(taskCodeEnum.getCode());
        ScheduleTaskRunRespDTO result = new ScheduleTaskRunRespDTO();
        result.setTaskCode(taskCodeEnum.getCode());
        if (latestLogDO == null) {
            return result;
        }
        result.setLogId(latestLogDO.getId());
        result.setStatus(latestLogDO.getStatus() == null ? null : latestLogDO.getStatus().name());
        result.setScanCount(latestLogDO.getScanCount());
        result.setSuccessCount(latestLogDO.getSuccessCount());
        result.setFailCount(latestLogDO.getFailCount());
        result.setDurationMs(latestLogDO.getDurationMs());
        return result;
    }

    /**
     * 判断日志记录是否命中状态筛选
     *
     * @param logDO   执行日志
     * @param status  状态筛选，为空时全部命中
     * @return 命中返回 true
     */
    private boolean matchStatus(ScheduleTaskLogDO logDO, String status) {
        if (status == null || status.isEmpty()) {
            return true;
        }
        return logDO.getStatus() != null && status.equals(logDO.getStatus().name());
    }

    /**
     * 转换执行日志为出参
     *
     * @param logDO 执行日志数据对象
     * @return 执行日志出参
     */
    private ScheduleTaskLogRespDTO convertToLogRespDTO(ScheduleTaskLogDO logDO) {
        ScheduleTaskLogRespDTO result = new ScheduleTaskLogRespDTO();
        result.setLogId(logDO.getId());
        result.setTaskCode(logDO.getTaskCode());
        result.setTaskName(logDO.getTaskName());
        result.setTriggerType(logDO.getTriggerType() == null ? null : logDO.getTriggerType().name());
        result.setStatus(logDO.getStatus() == null ? null : logDO.getStatus().name());
        result.setStartTime(logDO.getStartTime());
        result.setEndTime(logDO.getEndTime());
        result.setDurationMs(logDO.getDurationMs());
        result.setScanCount(logDO.getScanCount());
        result.setSuccessCount(logDO.getSuccessCount());
        result.setFailCount(logDO.getFailCount());
        result.setErrorSummary(logDO.getErrorSummary());
        result.setInstanceId(logDO.getInstanceId());
        return result;
    }
}
