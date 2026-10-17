package com.smart.university.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.university.common.constant.RedisCommonConstant;
import com.smart.university.common.context.TaskContext;
import com.smart.university.common.enums.TaskCodeEnum;
import com.smart.university.common.enums.TriggerTypeEnum;
import com.smart.university.common.util.RedisKeyUtil;
import com.smart.university.common.util.ScheduleTaskLogUtil;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.entity.StatSemesterOverviewDO;
import com.smart.university.service.SemesterService;
import com.smart.university.service.StatisticsSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 教务看板缓存刷新定时任务（A-04）
 *
 * <p>先覆盖写当日看板快照，再把看板指标写入 Redis 缓存 Key
 * {@link RedisCommonConstant#DASHBOARD_OVERVIEW_KEY}，缓存失效后首次访问由看板查询实时重建。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardCacheRefreshTask {

    private final StatisticsSnapshotService statisticsSnapshotService;

    private final SemesterService semesterService;

    private final ScheduleTaskLogUtil scheduleTaskLogUtil;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 任务开关
     */
    @Value("${smart-university.schedule.tasks.dashboard-refresh.enabled:true}")
    private boolean enabled;

    /**
     * 刷新看板快照与缓存，开关关闭时直接跳过
     */
    @Scheduled(fixedDelayString = "${smart-university.schedule.tasks.dashboard-refresh.fixed-delay:600000}")
    public void execute() {
        if (!enabled) {
            return;
        }
        scheduleTaskLogUtil.runWithLog(TaskCodeEnum.DASHBOARD_REFRESH_TASK, TriggerTypeEnum.SCHEDULED, this::doExecute);
    }

    /**
     * 执行看板缓存刷新
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
        SemesterDO semesterDO = semesterService.getCurrentSemester();
        Long semesterId = semesterDO.getId();
        context.addScanCount(1);
        StatSemesterOverviewDO overviewDO = statisticsSnapshotService.buildOverviewSnapshot(semesterId);
        String cacheKey = RedisKeyUtil.buildDashboardOverviewKey(semesterId);
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(overviewDO),
                    Duration.ofSeconds(RedisCommonConstant.DASHBOARD_OVERVIEW_TTL_SECONDS));
            context.addSuccessCount(1);
            log.info("教务看板缓存刷新完成，学期ID：{}，缓存Key：{}", semesterId, cacheKey);
        } catch (Exception ex) {
            context.addFailCount(1);
            context.appendError("看板缓存写入失败，" + ex.getMessage());
            log.error("教务看板缓存刷新失败，学期ID：{}", semesterId, ex);
        }
        return context;
    }
}
