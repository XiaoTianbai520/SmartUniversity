package com.smart.university.domain.dto.req;

import lombok.Data;

/**
 * 排课冲突检测查询入参
 */
@Data
public class ScheduleConflictQueryReqDTO {

    /**
     * 排除的排课 ID，修改排课时排除自身
     */
    private Long excludeScheduleId;

    /**
     * 教学班 ID
     */
    private Long teachingClassId;

    /**
     * 任课教师 ID
     */
    private Long teacherId;

    /**
     * 教室 ID
     */
    private Long classroomId;

    /**
     * 星期 1-7
     */
    private Integer weekday;

    /**
     * 开始节次
     */
    private Integer startSection;

    /**
     * 结束节次
     */
    private Integer endSection;

    /**
     * 开始周
     */
    private Integer startWeek;

    /**
     * 结束周
     */
    private Integer endWeek;
}
