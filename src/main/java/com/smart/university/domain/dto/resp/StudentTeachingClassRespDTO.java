package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 学生可选课程列表项出参
 */
@Data
public class StudentTeachingClassRespDTO {

    /**
     * 教学班 ID
     */
    private Long teachingClassId;

    /**
     * 教学班编号
     */
    private String classCode;

    /**
     * 教学班名称
     */
    private String className;

    /**
     * 课程 ID
     */
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
     * 课程性质
     */
    private String courseType;

    /**
     * 学分
     */
    private BigDecimal credit;

    /**
     * 教师 ID
     */
    private Long teacherId;

    /**
     * 教师姓名
     */
    private String teacherName;

    /**
     * 容量
     */
    private Integer capacity;

    /**
     * 已选人数
     */
    private Long selectedCount;

    /**
     * 剩余容量
     */
    private Long remainingCount;

    /**
     * 排课信息
     */
    private List<ScheduleRespDTO> schedules;
}
