package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教务首页数据看板出参（38.1）
 */
@Data
public class DashboardOverviewRespDTO {

    /**
     * 学期 ID
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 学期编号
     */
    private String semesterCode;

    /**
     * 在册学生总数 = COUNT(student WHERE status=1)，非本学期已选课人数
     */
    private Integer studentCount;

    /**
     * 本学期已选课学生数 = COUNT(DISTINCT course_selection.student_id) 当前批次且 status='SELECTED'
     */
    private Integer selectedStudentCount;

    /**
     * 本学期开设课程数（DISTINCT course_id）
     */
    private Integer courseCount;

    /**
     * 本学期教学班数
     */
    private Integer teachingClassCount;

    /**
     * 本学期任课教师数（DISTINCT teacher_id）
     */
    private Integer teacherCount;

    /**
     * 当前批次已选人次
     */
    private Integer selectionCount;

    /**
     * 当前批次候补人次
     */
    private Integer waitlistCount;

    /**
     * 本学期退课人次
     */
    private Integer withdrawCount;

    /**
     * 满班率百分比，保留 1 位小数
     */
    private BigDecimal fullClassRate;

    /**
     * 教室利用率百分比，保留 1 位小数
     */
    private BigDecimal classroomUsageRate;

    /**
     * 教师平均工作量（课时），保留 1 位小数
     */
    private BigDecimal avgTeacherWorkload;

    /**
     * 平均学分完成度百分比，仅统计有培养方案的学生
     */
    private BigDecimal avgCreditProgressRate;

    /**
     * 待发布成绩教学班数
     */
    private Integer unpublishedClassCount;

    /**
     * 当前选课批次状态：NOT_STARTED / IN_PROGRESS / ENDED
     */
    private String batchStatus;

    /**
     * 快照生成时间，实时计算时为当前时间
     */
    private LocalDateTime snapshotTime;

    /**
     * 是否命中快照
     */
    private Boolean fromSnapshot;
}
