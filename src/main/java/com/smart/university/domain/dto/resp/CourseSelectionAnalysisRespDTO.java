package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 课程选课分析出参（37.3）
 */
@Data
public class CourseSelectionAnalysisRespDTO {

    /**
     * 课程 ID
     */
    @Schema(example = "1")
    private Long courseId;

    /**
     * 课程编号
     */
    private String courseCode;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 教学班 ID
     */
    private Long teachingClassId;

    /**
     * 教学班编号
     */
    private String classCode;

    /**
     * 任课教师 ID
     */
    private Long teacherId;

    /**
     * 任课教师姓名
     */
    private String teacherName;

    /**
     * 教学班容量
     */
    private Integer capacity;

    /**
     * 已选人数（DB 权威口径 COUNT(course_selection WHERE status='SELECTED')，不含候补）
     */
    private Integer selectedCount;

    /**
     * 候补人数
     */
    private Integer waitingCount;

    /**
     * 退课人数，仅统计 WITHDRAWN
     */
    private Integer withdrawCount;

    /**
     * 是否满班：1 满班，0 未满（不含候补）
     */
    private Integer fullFlag;

    /**
     * 满班率百分比，0~100，保留 1 位小数
     */
    private BigDecimal fullRate;
}
