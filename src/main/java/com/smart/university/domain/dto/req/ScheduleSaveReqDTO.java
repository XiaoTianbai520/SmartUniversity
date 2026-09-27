package com.smart.university.domain.dto.req;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 排课新增 / 修改入参
 */
@Data
public class ScheduleSaveReqDTO {

    /**
     * 排课 ID，修改时必传
     */
    private Long id;

    /**
     * 教室 ID
     */
    @NotNull(message = "教室 ID 不能为空")
    private Long classroomId;

    /**
     * 星期 1-7
     */
    @NotNull(message = "星期不能为空")
    private Integer weekday;

    /**
     * 开始节次
     */
    @NotNull(message = "开始节次不能为空")
    private Integer startSection;

    /**
     * 结束节次
     */
    @NotNull(message = "结束节次不能为空")
    private Integer endSection;

    /**
     * 开始周
     */
    @NotNull(message = "开始周不能为空")
    private Integer startWeek;

    /**
     * 结束周
     */
    @NotNull(message = "结束周不能为空")
    private Integer endWeek;
}
