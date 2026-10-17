package com.smart.university.service.impl;

import com.smart.university.common.context.TaskContext;
import com.smart.university.common.enums.RiskLevelEnum;
import com.smart.university.common.enums.TaskCodeEnum;
import com.smart.university.common.enums.TriggerTypeEnum;
import com.smart.university.common.util.ScheduleConflictUtil;
import com.smart.university.common.util.ScheduleTaskLogUtil;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.DataAuditIssueDO;
import com.smart.university.domain.entity.ScoreDO;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.entity.TeachingClassScheduleDO;
import com.smart.university.domain.enums.ScoreStatusEnum;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.domain.enums.SemesterStatusEnum;
import com.smart.university.domain.enums.TeachingClassStatusEnum;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.DataAuditIssueMapper;
import com.smart.university.mapper.ScoreMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.mapper.TeachingClassScheduleMapper;
import com.smart.university.service.SemesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 异常数据巡检定时任务（A-08）
 *
 * <p>只读巡检，不修改任何业务数据；结果按「规则编码 + 业务 ID」覆盖写，
 * 每轮先清理当日旧结果，再写入本轮结果。
 *
 * <p>规则：`CAPACITY_ZERO`（容量为 0 或小于已选人数）、
 * `SCHEDULE_CONFLICT`（教师 / 教室排课冲突，判定复用 T0 的 {@link ScheduleConflictUtil}）、
 * `SCORE_UNPUBLISHED`（学期已结束但成绩未全部发布）、`NO_SCHEDULE`（教学班未编排排课）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataAuditTask {

    /**
     * 教学班业务类型
     */
    private static final String BIZ_TYPE_TEACHING_CLASS = "TEACHING_CLASS";

    /**
     * 教室业务类型
     */
    private static final String BIZ_TYPE_CLASSROOM = "CLASSROOM";

    /**
     * 教师业务类型
     */
    private static final String BIZ_TYPE_TEACHER = "TEACHER";

    /**
     * 规则：容量为 0 或小于已选人数
     */
    private static final String RULE_CAPACITY_ZERO = "CAPACITY_ZERO";

    /**
     * 规则：教师 / 教室排课冲突
     */
    private static final String RULE_SCHEDULE_CONFLICT = "SCHEDULE_CONFLICT";

    /**
     * 规则：成绩未全部发布
     */
    private static final String RULE_SCORE_UNPUBLISHED = "SCORE_UNPUBLISHED";

    /**
     * 规则：教学班未编排排课
     */
    private static final String RULE_NO_SCHEDULE = "NO_SCHEDULE";

    private final TeachingClassMapper teachingClassMapper;

    private final TeachingClassScheduleMapper teachingClassScheduleMapper;

    private final CourseSelectionMapper courseSelectionMapper;

    private final ScoreMapper scoreMapper;

    private final SemesterService semesterService;

    private final DataAuditIssueMapper dataAuditIssueMapper;

    private final ScheduleTaskLogUtil scheduleTaskLogUtil;

    /**
     * 任务开关
     */
    @Value("${smart-university.schedule.tasks.data-audit.enabled:true}")
    private boolean enabled;

    /**
     * 定时执行数据巡检，开关关闭时直接跳过
     */
    @Scheduled(cron = "${smart-university.schedule.tasks.data-audit.cron:0 0 3 * * ?}")
    public void execute() {
        if (!enabled) {
            return;
        }
        scheduleTaskLogUtil.runWithLog(TaskCodeEnum.DATA_AUDIT_TASK, TriggerTypeEnum.SCHEDULED, this::doExecute);
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
     * 执行一轮巡检，逐条扫描本学期教学班，单条失败跳过并记录异常摘要
     *
     * @return 任务执行上下文
     */
    public TaskContext doExecute() {
        TaskContext context = TaskContext.empty();
        SemesterDO semesterDO = semesterService.getCurrentSemester();
        if (semesterDO == null) {
            context.appendError("无生效学期，跳过本轮巡检");
            return context;
        }
        Long semesterId = semesterDO.getId();
        LocalDate statDate = LocalDate.now();
        boolean semesterEnded = SemesterStatusEnum.FINISHED == semesterDO.getStatus()
                || (semesterDO.getEndDate() != null && semesterDO.getEndDate().isBefore(statDate));
        List<TeachingClassDO> teachingClassDOList = listTeachingClass(semesterId);
        List<TeachingClassScheduleDO> scheduleDOList = teachingClassScheduleMapper
                .listScheduleBySemesterId(semesterId);
        Map<Long, List<CourseSelectionDO>> selectedMap = groupSelectionByClass(
                listSemesterSelection(semesterId, SelectionStatusEnum.SELECTED));
        Map<Long, Long> selectionClassMap = new HashMap<>();
        for (Map.Entry<Long, List<CourseSelectionDO>> entry : selectedMap.entrySet()) {
            for (CourseSelectionDO each : entry.getValue()) {
                selectionClassMap.put(each.getId(), entry.getKey());
            }
        }
        Map<Long, Integer> publishedCountMap = new HashMap<>();
        for (ScoreDO each : scoreMapper.listScoreBySelectionIds(new ArrayList<>(selectionClassMap.keySet()))) {
            if (ScoreStatusEnum.PUBLISHED != each.getStatus()) {
                continue;
            }
            Long teachingClassId = selectionClassMap.get(each.getCourseSelectionId());
            if (teachingClassId != null) {
                publishedCountMap.merge(teachingClassId, 1, Integer::sum);
            }
        }
        Map<Long, Long> teacherIdMap = new HashMap<>();
        for (TeachingClassDO each : teachingClassDOList) {
            if (each.getTeacherId() != null) {
                teacherIdMap.put(each.getId(), each.getTeacherId());
            }
        }
        List<DataAuditIssueDO> issueList = new ArrayList<>();
        for (TeachingClassDO each : teachingClassDOList) {
            context.addScanCount(1);
            try {
                int selectedCount = selectedMap.getOrDefault(each.getId(), Collections.emptyList()).size();
                Integer capacity = each.getCapacity();
                if (capacity == null || capacity <= 0 || capacity < selectedCount) {
                    String description = "教学班【" + each.getClassCode() + "】容量为 0 或小于已选人数 " + selectedCount;
                    issueList.add(buildIssue(RULE_CAPACITY_ZERO, BIZ_TYPE_TEACHING_CLASS, each.getId(),
                            RiskLevelEnum.HIGH, description));
                }
                if (!hasSchedule(each.getId(), scheduleDOList)) {
                    String description = "教学班【" + each.getClassCode() + "】尚未编排排课";
                    issueList.add(buildIssue(RULE_NO_SCHEDULE, BIZ_TYPE_TEACHING_CLASS, each.getId(),
                            RiskLevelEnum.LOW, description));
                }
                if (semesterEnded) {
                    int publishedCount = publishedCountMap.getOrDefault(each.getId(), 0);
                    if (publishedCount < selectedCount) {
                        String description = "教学班【" + each.getClassCode() + "】学期已结束，仍有 "
                                + (selectedCount - publishedCount) + " 条成绩未发布";
                        issueList.add(buildIssue(RULE_SCORE_UNPUBLISHED, BIZ_TYPE_TEACHING_CLASS, each.getId(),
                                RiskLevelEnum.MEDIUM, description));
                    }
                }
            } catch (Exception ex) {
                context.addFailCount(1);
                context.appendError("教学班巡检失败，teachingClassId：" + each.getId() + "，" + ex.getMessage());
            }
        }
        // 排课冲突：教师 / 教室两个维度，判定复用 T0 的 ScheduleConflictUtil
        for (int i = 0; i < scheduleDOList.size(); i++) {
            for (int j = i + 1; j < scheduleDOList.size(); j++) {
                TeachingClassScheduleDO source = scheduleDOList.get(i);
                TeachingClassScheduleDO target = scheduleDOList.get(j);
                try {
                    if (!ScheduleConflictUtil.isConflict(source, target)) {
                        continue;
                    }
                    if (Objects.equals(source.getTeachingClassId(), target.getTeachingClassId())) {
                        continue;
                    }
                    if (Objects.nonNull(source.getClassroomId())
                            && Objects.equals(source.getClassroomId(), target.getClassroomId())) {
                        issueList.add(buildIssue(RULE_SCHEDULE_CONFLICT, BIZ_TYPE_CLASSROOM, source.getClassroomId(),
                                RiskLevelEnum.MEDIUM,
                                "教室【" + source.getClassroomId() + "】在周"
                                        + source.getWeekday() + " 第" + source.getStartSection()
                                        + "-" + source.getEndSection() + "节存在排课冲突"));
                    }
                    Long sourceTeacherId = teacherIdMap.get(source.getTeachingClassId());
                    Long targetTeacherId = teacherIdMap.get(target.getTeachingClassId());
                    if (Objects.nonNull(sourceTeacherId) && Objects.equals(sourceTeacherId, targetTeacherId)) {
                        issueList.add(buildIssue(RULE_SCHEDULE_CONFLICT, BIZ_TYPE_TEACHER, sourceTeacherId,
                                RiskLevelEnum.MEDIUM,
                                "教师【" + sourceTeacherId + "】在周" + source.getWeekday() + " 第"
                                        + source.getStartSection() + "-" + source.getEndSection()
                                        + "节存在排课冲突"));
                    }
                } catch (Exception ex) {
                    context.addFailCount(1);
                    context.appendError("排课冲突巡检失败，scheduleId：" + source.getId() + "，" + ex.getMessage());
                }
            }
        }
        dataAuditIssueMapper.removeDataAuditIssueBySemesterAndDate(semesterId, statDate);
        for (DataAuditIssueDO each : issueList) {
            each.setSemesterId(semesterId);
            each.setStatDate(statDate);
            try {
                dataAuditIssueMapper.saveDataAuditIssue(each);
                context.addSuccessCount(1);
            } catch (Exception ex) {
                context.addFailCount(1);
                context.appendError("巡检结果落库失败，ruleCode：" + each.getRuleCode() + "，" + ex.getMessage());
                log.error("巡检结果落库失败", ex);
            }
        }
        return context;
    }

    /**
     * 查询本学期未取消的教学班
     *
     * @param semesterId 学期 ID
     * @return 教学班集合
     */
    private List<TeachingClassDO> listTeachingClass(Long semesterId) {
        return teachingClassMapper.listTeachingClassBySemesterId(semesterId).stream()
                .filter(each -> TeachingClassStatusEnum.CANCELLED != each.getStatus())
                .toList();
    }

    /**
     * 按教学班分组选课记录
     *
     * @param selectionDOList 选课记录集合
     * @return 教学班 ID 到选课记录的映射
     */
    private Map<Long, List<CourseSelectionDO>> groupSelectionByClass(List<CourseSelectionDO> selectionDOList) {
        Map<Long, List<CourseSelectionDO>> result = new HashMap<>();
        for (CourseSelectionDO each : selectionDOList) {
            if (each.getTeachingClassId() == null) {
                continue;
            }
            result.computeIfAbsent(each.getTeachingClassId(), key -> new ArrayList<>()).add(each);
        }
        return result;
    }

    /**
     * 查询本学期指定状态的选课记录
     *
     * @param semesterId 学期 ID
     * @param status     选课状态
     * @return 选课记录集合
     */
    private List<CourseSelectionDO> listSemesterSelection(Long semesterId, SelectionStatusEnum status) {
        StudentSelectionQueryReqDTO requestParam = new StudentSelectionQueryReqDTO();
        requestParam.setSemesterId(semesterId);
        requestParam.setStatus(status.name());
        return courseSelectionMapper.listSelectionByStudent(requestParam);
    }

    /**
     * 判断教学班是否已编排排课
     *
     * @param teachingClassId 教学班 ID
     * @param scheduleDOList  本学期排课集合
     * @return 已有排课返回 true
     */
    private boolean hasSchedule(Long teachingClassId, List<TeachingClassScheduleDO> scheduleDOList) {
        for (TeachingClassScheduleDO each : scheduleDOList) {
            if (Objects.equals(each.getTeachingClassId(), teachingClassId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建巡检结果数据对象
     *
     * @param ruleCode    规则编码
     * @param bizType     业务类型
     * @param bizId       业务 ID
     * @param severity    严重级别
     * @param description 问题描述
     * @return 巡检结果数据对象
     */
    private DataAuditIssueDO buildIssue(String ruleCode, String bizType, Long bizId,
                                        RiskLevelEnum severity, String description) {
        DataAuditIssueDO result = new DataAuditIssueDO();
        result.setRuleCode(ruleCode);
        result.setRuleName(resolveRuleName(ruleCode));
        result.setBizType(bizType);
        result.setBizId(bizId);
        result.setSeverity(severity);
        result.setDescription(description);
        return result;
    }

    /**
     * 取规则的中文名
     *
     * @param ruleCode 规则编码
     * @return 规则中文名
     */
    private String resolveRuleName(String ruleCode) {
        if (RULE_CAPACITY_ZERO.equals(ruleCode)) {
            return "教学班容量为 0 或小于已选人数";
        }
        if (RULE_SCHEDULE_CONFLICT.equals(ruleCode)) {
            return "排课时间冲突";
        }
        if (RULE_SCORE_UNPUBLISHED.equals(ruleCode)) {
            return "成绩长期未发布";
        }
        return "教学班未编排排课";
    }
}
