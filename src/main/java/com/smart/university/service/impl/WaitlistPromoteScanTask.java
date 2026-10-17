package com.smart.university.service.impl;

import com.smart.university.common.context.TaskContext;
import com.smart.university.common.enums.TaskCodeEnum;
import com.smart.university.common.enums.TriggerTypeEnum;
import com.smart.university.common.util.ScheduleTaskLogUtil;
import com.smart.university.domain.dto.resp.WaitlistPromoteRespDTO;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 候补递补兜底扫描定时任务（A-02）
 *
 * <p>只调用 {@link WaitlistService#tryPromote} 公开方法完成递补，
 * 校验与容量判定全部沿用候补服务既有实现，本任务不复制任何递补逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WaitlistPromoteScanTask {

    private final CourseSelectionMapper courseSelectionMapper;

    private final WaitlistService waitlistService;

    private final ScheduleTaskLogUtil scheduleTaskLogUtil;

    /**
     * 任务开关
     */
    @Value("${smart-university.schedule.tasks.waitlist-scan.enabled:true}")
    private boolean enabled;

    /**
     * 扫描存在候补记录的教学班并尝试递补，开关关闭时直接跳过
     */
    @Scheduled(fixedDelayString = "${smart-university.schedule.tasks.waitlist-scan.fixed-delay:300000}")
    public void execute() {
        if (!enabled) {
            return;
        }
        scheduleTaskLogUtil.runWithLog(TaskCodeEnum.WAITLIST_SCAN_TASK, TriggerTypeEnum.SCHEDULED, this::doExecute);
    }

    /**
     * 执行候补递补扫描，单个教学班失败跳过并记录异常摘要
     *
     * @return 任务执行上下文
     */
    /**
     * 手工触发入口，供教务端手动执行任务复用
     *
     * @return 任务执行上下文
     */
    public TaskContext runManually() {
        return doExecute();
    }

    public TaskContext doExecute() {
        TaskContext context = TaskContext.empty();
        List<Long> teachingClassIdList = courseSelectionMapper.listTeachingClassIdHavingWaiting();
        context.addScanCount(teachingClassIdList.size());
        for (Long each : teachingClassIdList) {
            try {
                WaitlistPromoteRespDTO promoteResult = waitlistService.tryPromote(each);
                int promotedCount = promoteResult.getPromotedCount() == null ? 0 : promoteResult.getPromotedCount();
                int invalidCount = promoteResult.getInvalidCount() == null ? 0 : promoteResult.getInvalidCount();
                context.addSuccessCount(promotedCount);
                if (invalidCount > 0) {
                    context.addFailCount(invalidCount);
                    context.appendError("教学班 " + each + " 有 " + invalidCount + " 条候补重新校验未通过");
                }
                if (promotedCount > 0) {
                    log.info("候补递补兜底扫描完成，teachingClassId：{}，递补人数：{}", each, promotedCount);
                }
            } catch (Exception ex) {
                context.addFailCount(1);
                context.appendError("教学班 " + each + " 递补失败，" + ex.getMessage());
                log.error("候补递补兜底扫描失败，teachingClassId：{}", each, ex);
            }
        }
        return context;
    }
}
