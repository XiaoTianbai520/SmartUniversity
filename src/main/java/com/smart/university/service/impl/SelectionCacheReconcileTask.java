package com.smart.university.service.impl;

import com.smart.university.common.context.TaskContext;
import com.smart.university.common.enums.TaskCodeEnum;
import com.smart.university.common.enums.TriggerTypeEnum;
import com.smart.university.common.util.ScheduleTaskLogUtil;
import com.smart.university.domain.dto.resp.CacheReconcileRespDTO;
import com.smart.university.service.CacheReconcileService;
import com.smart.university.service.SemesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 缓存一致性对账定时任务（V1.2 W2）
 *
 * <p>每小时对当前学期教学班做三方对账，以 DB 为准回写 Redis 计数与 selected_count。
 * 任务开关与 cron 均带默认值兜底，不修改 application*.yml 冻结文件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelectionCacheReconcileTask {

    private final CacheReconcileService cacheReconcileService;

    private final SemesterService semesterService;

    private final ScheduleTaskLogUtil scheduleTaskLogUtil;

    /**
     * 任务开关
     */
    @Value("${smart-university.schedule.tasks.cache-reconcile.enabled:true}")
    private boolean enabled;

    /**
     * 定时对账，开关关闭时直接跳过
     */
    @Scheduled(cron = "${smart-university.schedule.tasks.cache-reconcile.cron:0 0 */1 * * ?}")
    public void execute() {
        if (!enabled) {
            return;
        }
        scheduleTaskLogUtil.runWithLog(TaskCodeEnum.CACHE_RECONCILE_TASK, TriggerTypeEnum.SCHEDULED, this::doExecute);
    }

    /**
     * 手工触发入口，供教务端手动执行任务复用
     *
     * @return 任务执行上下文
     */
    public TaskContext runManually() {
        return doExecute();
    }

    /**
     * 执行对账：定位当前学期后批量以 DB 为准校正
     *
     * @return 任务执行上下文
     */
    public TaskContext doExecute() {
        TaskContext context = TaskContext.empty();
        Long semesterId = semesterService.getCurrentSemester().getId();
        CacheReconcileRespDTO result = cacheReconcileService.reconcile(semesterId, null);
        int scanCount = result.getScanCount() == null ? 0 : result.getScanCount();
        int failCount = result.getFailCount() == null ? 0 : result.getFailCount();
        context.addScanCount(scanCount);
        context.addSuccessCount(Math.max(scanCount - failCount, 0));
        context.addFailCount(failCount);
        log.info("缓存一致性对账定时任务完成，学期={}，扫描={}，修复={}，失败={}，耗时={}ms",
                semesterId, scanCount, result.getFixedCount(), failCount, result.getDurationMs());
        return context;
    }
}
