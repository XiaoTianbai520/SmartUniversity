package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(example = "1")
    private Long id;

    /**
     * 教室 ID
     */
    @Schema(example = "1")
    @NotNull(message = "教室 ID 不能为空")
    private Long classroomId;

    /**
     * 星期 1-7
     */
    @Schema(example = "1")
    @NotNull(message = "星期不能为空")
    private Integer weekday;

    /**
     * 开始节次
     */
    @Schema(example = "1")
    @NotNull(message = "开始节次不能为空")
    private Integer startSection;

    /**
     * 结束节次
     */
    @Schema(example = "2")
    @NotNull(message = "结束节次不能为空")
    private Integer endSection;

    /**
     * 开始周
     */
    @Schema(example = "1")
    @NotNull(message = "开始周不能为空")
    private Integer startWeek;

    /**
     * 结束周
     */
    @Schema(example = "16")
    @NotNull(message = "结束周不能为空")
    private Integer endWeek;
}
