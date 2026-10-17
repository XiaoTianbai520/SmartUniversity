package com.smart.university.service.impl;

import com.smart.university.common.context.TaskContext;
import com.smart.university.common.enums.TaskCodeEnum;
import com.smart.university.common.enums.TriggerTypeEnum;
import com.smart.university.common.util.ScheduleTaskLogUtil;
import com.smart.university.domain.dto.resp.SnapshotRefreshRespDTO;
import com.smart.university.service.SemesterService;
import com.smart.university.service.StatisticsSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 统计快照预计算定时任务（A-03），默认凌晨错峰执行
 *
 * <p>直接复用 W3 已交付的 {@link StatisticsSnapshotService#refreshSnapshot}（接口 37.4），
 * 覆盖写且幂等，失败保留上一版快照。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsSnapshotTask {

    private final StatisticsSnapshotService statisticsSnapshotService;

    private final SemesterService semesterService;

    private final ScheduleTaskLogUtil scheduleTaskLogUtil;

    /**
     * 任务开关
     */
    @Value("${smart-university.schedule.tasks.stat-snapshot.enabled:true}")
    private boolean enabled;

    /**
     * 单次扫描上限，用于日志与超限提示
     */
    @Value("${smart-university.snapshot.scan-limit:500}")
    private int scanLimit;

    /**
     * 预计算当前学期的全部统计快照，开关关闭时直接跳过
     */
    @Scheduled(cron = "${smart-university.schedule.tasks.stat-snapshot.cron:0 0 2 * * ?}")
    public void execute() {
        if (!enabled) {
            return;
        }
        scheduleTaskLogUtil.runWithLog(TaskCodeEnum.STAT_SNAPSHOT_TASK, TriggerTypeEnum.SCHEDULED, this::doExecute);
    }

    /**
     * 执行统计快照预计算
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
        Long semesterId = semesterService.getCurrentSemester().getId();
        SnapshotRefreshRespDTO refreshResult = statisticsSnapshotService.refreshSnapshot(semesterId, List.of());
        int successCount = refreshResult.getSuccessCount() == null ? 0 : refreshResult.getSuccessCount();
        int failCount = refreshResult.getFailCount() == null ? 0 : refreshResult.getFailCount();
        context.addScanCount(successCount + failCount);
        context.addSuccessCount(successCount);
        context.addFailCount(failCount);
        if (successCount + failCount > scanLimit) {
            context.appendError("本轮快照类型数超过单次扫描上限 " + scanLimit + "，请关注执行耗时");
        }
        log.info("统计快照预计算完成，学期ID：{}，成功类型数：{}，失败类型数：{}，耗时：{} 毫秒",
                semesterId, successCount, failCount, refreshResult.getDurationMs());
        return context;
    }
}
