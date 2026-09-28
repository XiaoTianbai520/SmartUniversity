package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * 排课冲突检测查询入参
 */
@Data
public class ScheduleConflictQueryReqDTO {

    /**
     * 排除的排课 ID，修改排课时排除自身
     */
    @Schema(example = "1")
    private Long excludeScheduleId;

    /**
     * 教学班 ID
     */
    @Schema(example = "1")
    private Long teachingClassId;

    /**
     * 任课教师 ID
     */
    @Schema(example = "T10001")
    private Long teacherId;

    /**
     * 教室 ID
     */
    @Schema(example = "1")
    private Long classroomId;

    /**
     * 星期 1-7
     */
    @Schema(example = "1")
    private Integer weekday;

    /**
     * 开始节次
     */
    @Schema(example = "1")
    private Integer startSection;

    /**
     * 结束节次
     */
    @Schema(example = "2")
    private Integer endSection;

    /**
     * 开始周
     */
    @Schema(example = "1")
    private Integer startWeek;

    /**
     * 结束周
     */
    @Schema(example = "16")
    private Integer endWeek;
}
