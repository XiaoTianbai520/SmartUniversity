package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.util.List;

/**
 * 教师我的教学班出参
 */
@Data
public class TeacherTeachingClassRespDTO {

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
     * 课程名称
     */
    private String courseName;

    /**
     * 容量
     */
    private Integer capacity;

    /**
     * 已选人数
     */
    private Long selectedCount;

    /**
     * 排课信息
     */
    private List<ScheduleRespDTO> schedules;
}
