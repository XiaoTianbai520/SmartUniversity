package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.util.List;

/**
 * 教学班出参
 */
@Data
public class TeachingClassRespDTO {

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
     * 课程名称
     */
    private String courseName;

    /**
     * 学期 ID
     */
    private Long semesterId;

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
     * DRAFT / AVAILABLE / CLOSED / CANCELLED
     */
    private String status;

    /**
     * 排课信息
     */
    private List<ScheduleRespDTO> schedules;
}
