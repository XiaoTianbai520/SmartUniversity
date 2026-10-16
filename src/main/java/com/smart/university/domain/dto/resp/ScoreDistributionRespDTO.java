package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 成绩分段分布出参（39.2 / 40.3 distribution）
 */
@Data
public class ScoreDistributionRespDTO {

    /**
     * 分数段，例如 0-59 / 60-69 / 70-79 / 80-84 / 85-100
     */
    @Schema(example = "0-59")
    private String segment;

    /**
     * 该分数段人数
     */
    private Integer count;
}
