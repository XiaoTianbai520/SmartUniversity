package com.smart.university.domain.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 教室推荐查询入参（40.4）
 */
@Data
public class ClassroomRecommendQueryReqDTO {

    /**
     * 待排课教学班 ID，必填
     */
    @Schema(example = "12")
    private Long teachingClassId;

    /**
     * 目标星期：1周一...7周日
     */
    @Schema(example = "1")
    private Integer weekday;

    /**
     * 目标开始节次
     */
    @Schema(example = "1")
    private Integer startSection;

    /**
     * 目标结束节次
     */
    @Schema(example = "2")
    private Integer endSection;

    /**
     * 目标开始周
     */
    @Schema(example = "1")
    private Integer startWeek;

    /**
     * 目标结束周
     */
    @Schema(example = "16")
    private Integer endWeek;

    /**
     * 推荐条数，默认 5
     */
    @Schema(example = "5")
    private Integer topN;
}
