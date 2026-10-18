package com.smart.university.service.impl;

import com.smart.university.common.context.TaskContext;
import com.smart.university.common.enums.TaskCodeEnum;
import com.smart.university.common.enums.TriggerTypeEnum;
import com.smart.university.common.util.ScheduleTaskLogUtil;
import com.smart.university.domain.dto.resp.CacheWarmupRespDTO;
import com.smart.university.domain.entity.SelectionBatchDO;
import com.smart.university.mapper.SelectionBatchMapper;
import com.smart.university.service.SelectionCapacityService;
import com.smart.university.service.SemesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 容量缓存预热定时任务（P-01）
 *
 * <p>仅预热当前学期「进行中」批次下的教学班，以 DB 权威已选人数覆盖写容量 Key，重复执行幂等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelectionCacheWarmupTask {

    private final SelectionCapacityService selectionCapacityService;

    private final SelectionBatchMapper selectionBatchMapper;

    private final SemesterService semesterService;

    private final ScheduleTaskLogUtil scheduleTaskLogUtil;

    /**
     * 任务开关
     */
    @Value("${smart-university.schedule.tasks.selection-cache-warmup.enabled:true}")
    private boolean enabled;

    /**
     * 定时预热当前批次的容量缓存，开关关闭时直接跳过
     */
    @Scheduled(cron = "${smart-university.schedule.tasks.selection-cache-warmup.cron:0 0/30 * * * ?}")
    public void execute() {
        if (!enabled) {
            return;
        }
        scheduleTaskLogUtil.runWithLog(TaskCodeEnum.SELECTION_CACHE_WARMUP_TASK, TriggerTypeEnum.SCHEDULED,
                this::doExecute);
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
     * 执行预热：定位当前批次后批量覆盖写容量 Key
     *
     * @return 任务执行上下文
     */
    public TaskContext doExecute() {
        TaskContext context = TaskContext.empty();
        Long semesterId = semesterService.getCurrentSemester().getId();
        SelectionBatchDO batchDO = selectionBatchMapper.getCurrentSelectionBatch(semesterId);
        if (batchDO == null) {
            context.appendError("当前学期无进行中的选课批次，跳过预热");
            return context;
        }
        CacheWarmupRespDTO warmupResult = selectionCapacityService.warmupByBatchId(batchDO.getId());
        context.addScanCount(warmupResult.getTeachingClassCount() == null ? 0 : warmupResult.getTeachingClassCount());
        context.addSuccessCount(warmupResult.getWarmupCount() == null ? 0 : warmupResult.getWarmupCount());
        context.addFailCount(warmupResult.getFailCount() == null ? 0 : warmupResult.getFailCount());
        log.info("容量缓存预热完成，批次ID：{}，教学班数：{}，预热数：{}，失败数：{}，耗时：{} 毫秒",
                batchDO.getId(), warmupResult.getTeachingClassCount(), warmupResult.getWarmupCount(),
                warmupResult.getFailCount(), warmupResult.getDurationMs());
        return context;
    }
}
