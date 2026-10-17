package com.smart.university.service.impl;

import com.smart.university.common.context.TaskContext;
import com.smart.university.common.enums.RiskLevelEnum;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.common.enums.TaskCodeEnum;
import com.smart.university.common.enums.TriggerTypeEnum;
import com.smart.university.common.util.ScheduleTaskLogUtil;
import com.smart.university.domain.dto.resp.CreditProgressRespDTO;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.enums.NoticeTypeEnum;
import com.smart.university.domain.event.NoticeEvent;
import com.smart.university.domain.event.NoticeEventPublisher;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.service.CreditProgressService;
import com.smart.university.service.SemesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 学分不足 / 毕业风险预警定时任务（A-05）
 *
 * <p>决策 2：学分预警通知的 `biz_id` 直接存 `semester_id`，学生维度由 `receiver_user_id` 承担，
 * 天然实现「同一学生同一学期只预警一次」（`uk_notice_dedup` 兜底）。
 * 学分口径复用 W3 的 {@link CreditProgressService}，为跨学期累计（C8）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditWarningTask {

    /**
     * 启用状态
     */
    private static final int ENABLED_STATUS = 1;

    private final StudentMapper studentMapper;

    private final SemesterService semesterService;

    private final CreditProgressService creditProgressService;

    private final NoticeEventPublisher noticeEventPublisher;

    private final ScheduleTaskLogUtil scheduleTaskLogUtil;

    /**
     * 任务开关
     */
    @Value("${smart-university.schedule.tasks.credit-warning.enabled:true}")
    private boolean enabled;

    /**
     * 扫描全部在册学生的学分进度并发送预警，开关关闭时直接跳过
     */
    @Scheduled(cron = "${smart-university.schedule.tasks.credit-warning.cron:0 0 9 ? * MON}")
    public void execute() {
        if (!enabled) {
            return;
        }
        scheduleTaskLogUtil.runWithLog(TaskCodeEnum.CREDIT_WARNING_TASK, TriggerTypeEnum.SCHEDULED, this::doExecute);
    }

    /**
     * 执行学分预警，单个学生异常跳过并记录异常摘要
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
        List<StudentDO> studentDOList = studentMapper.listStudentByMajorAndGrade(null, null).stream()
                .filter(each -> each.getStatus() == null || each.getStatus() == ENABLED_STATUS)
                .toList();
        context.addScanCount(studentDOList.size());
        for (StudentDO each : studentDOList) {
            try {
                if (warnIfRisk(each, semesterId)) {
                    context.addSuccessCount(1);
                }
            } catch (Exception ex) {
                context.addFailCount(1);
                context.appendError("学生 " + each.getId() + " 预警失败，" + ex.getMessage());
                log.error("学分预警失败，studentId：{}", each.getId(), ex);
            }
        }
        return context;
    }

    /**
     * 判定学生风险并发送预警通知
     *
     * @param studentDO  学生数据对象
     * @param semesterId 学期 ID
     * @return 已发送预警返回 true
     */
    private boolean warnIfRisk(StudentDO studentDO, Long semesterId) {
        if (studentDO.getUserId() == null) {
            return false;
        }
        CreditProgressRespDTO creditProgress = creditProgressService
                .getCreditProgressByStudentId(studentDO.getId(), semesterId);
        RiskLevelEnum riskLevel = creditProgressService.getRiskLevel(studentDO.getId(), semesterId, creditProgress);
        if (RiskLevelEnum.LOW == riskLevel) {
            return false;
        }
        publishWarningNotice(studentDO, semesterId, creditProgress, riskLevel);
        return true;
    }

    /**
     * 发布学分预警通知，业务 ID 直存学期 ID
     *
     * @param studentDO      学生数据对象
     * @param semesterId     学期 ID
     * @param creditProgress 学分进度
     * @param riskLevel      风险等级
     */
    private void publishWarningNotice(StudentDO studentDO, Long semesterId,
                                      CreditProgressRespDTO creditProgress, RiskLevelEnum riskLevel) {
        String riskText = RiskLevelEnum.HIGH == riskLevel ? "高风险" : "中风险";
        BigDecimal planTotalCredit = creditProgress.getPlanTotalCredit() == null
                ? BigDecimal.ZERO
                : creditProgress.getPlanTotalCredit();
        BigDecimal completedCredit = creditProgress.getCompletedCredit() == null
                ? BigDecimal.ZERO
                : creditProgress.getCompletedCredit();
        BigDecimal progressRate = creditProgress.getProgressRate() == null
                ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
                : creditProgress.getProgressRate();
        noticeEventPublisher.publish(NoticeEvent.builder()
                .noticeType(NoticeTypeEnum.CREDIT_WARNING)
                .title(NoticeTypeEnum.CREDIT_WARNING.getDefaultTitle())
                .content("本学段学分进度为 " + progressRate + "%，已修 " + completedCredit + " / 要求 "
                        + planTotalCredit + " 学分，毕业风险等级：" + riskText + "，请尽快调整选课计划。")
                .receiverUserIds(List.of(studentDO.getUserId()))
                .receiverRole(RoleEnum.STUDENT)
                .bizId(semesterId)
                .build());
    }
}
