package com.smart.university.domain.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 成绩分布异常查询入参（39.2 / 40.3）
 */
@Data
public class ScoreAnomalyQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学期 ID，缺省当前学期
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 课程 ID
     */
    @Schema(example = "1")
    private Long courseId;

    /**
     * 异常类型：FAIL_RATE_HIGH / SCORE_MISSING / SCORE_TOO_HIGH
     */
    @Schema(example = "FAIL_RATE_HIGH")
    private String anomalyType;
}
