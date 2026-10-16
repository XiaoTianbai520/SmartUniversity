package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.university.domain.enums.SelectionBatchStatusEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * stat_semester_overview 表，教务数据看板指标快照
 */
@Data
@TableName("stat_semester_overview")
public class StatSemesterOverviewDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 快照ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 学期编号快照
     */
    private String semesterCode;

    /**
     * 在册学生数（COUNT(student WHERE status=1)）
     */
    private Integer studentCount;

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
     * 当前选课人次（status=SELECTED）
     */
    private Integer selectionCount;

    /**
     * 当前候补人次（status=WAITING）
     */
    private Integer waitlistCount;

    /**
     * 本学期退课人次（status=WITHDRAWN）
     */
    private Integer withdrawCount;

    /**
     * 课程满班率百分比，0~100
     */
    private BigDecimal fullClassRate;

    /**
     * 教室利用率百分比，0~100
     */
    private BigDecimal classroomUsageRate;

    /**
     * 教师平均工作量（课时）
     */
    private BigDecimal avgTeacherWorkload;

    /**
     * 平均学分完成度百分比
     */
    private BigDecimal avgCreditProgressRate;

    /**
     * 待发布成绩教学班数
     */
    private Integer unpublishedClassCount;

    /**
     * 当前选课批次状态
     */
    private SelectionBatchStatusEnum batchStatus;

    /**
     * 快照日期
     */
    private LocalDate statDate;

    /**
     * 快照生成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
