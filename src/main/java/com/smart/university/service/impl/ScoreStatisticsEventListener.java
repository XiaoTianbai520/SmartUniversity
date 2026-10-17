package com.smart.university.service.impl;

import com.smart.university.common.enums.StatTypeEnum;
import com.smart.university.domain.event.ScorePublishedEvent;
import com.smart.university.service.StatisticsSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 成绩发布事件监听器（A-06）
 *
 * <p>在成绩发布事务提交后触发统计覆盖写；监听异常一律只记录日志，不影响成绩发布结果。
 * 与 V1.1 的 {@code NoticeEvent} 监听风格保持一致（`AFTER_COMMIT` + `fallbackExecution`）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreStatisticsEventListener {

    private final StatisticsSnapshotService statisticsSnapshotService;

    /**
     * 处理成绩发布事件，刷新学分进度与课程选课分析快照
     *
     * @param event 成绩发布事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleScorePublished(ScorePublishedEvent event) {
        if (event == null || event.getTeachingClassId() == null) {
            return;
        }
        try {
            statisticsSnapshotService.refreshSnapshot(event.getSemesterId(),
                    List.of(StatTypeEnum.CREDIT_PROGRESS, StatTypeEnum.COURSE_SELECTION));
            log.info("成绩发布后自动统计完成，teachingClassId：{}，发布条数：{}",
                    event.getTeachingClassId(), event.getPublishedCount());
        } catch (Exception ex) {
            log.error("成绩发布后自动统计失败，teachingClassId：{}，异常摘要：{}",
                    event.getTeachingClassId(), ex.getMessage());
        }
    }
}
