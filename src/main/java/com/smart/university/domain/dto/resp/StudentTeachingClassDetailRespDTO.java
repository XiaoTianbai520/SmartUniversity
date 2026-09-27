package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.util.List;

/**
 * 学生课程详情出参
 */
@Data
public class StudentTeachingClassDetailRespDTO {

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
     * 课程信息
     */
    private CourseBriefRespDTO course;

    /**
     * 任课教师
     */
    private TeacherBriefRespDTO teacher;

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
     * 开放专业
     */
    private List<MajorRespDTO> allowedMajors;

    /**
     * 开放年级
     */
    private List<GradeRespDTO> allowedGrades;

    /**
     * 排课信息
     */
    private List<ScheduleRespDTO> schedules;
}
