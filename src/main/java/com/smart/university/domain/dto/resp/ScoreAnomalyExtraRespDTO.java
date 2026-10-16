package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 成绩分布异常口径回显（40.3 extra）
 */
@Data
public class ScoreAnomalyExtraRespDTO {

    /**
     * 学期 ID
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 及格率偏低班级数
     */
    private Integer failRateHighCount;

    /**
     * 成绩缺失班级数
     */
    private Integer scoreMissingCount;

    /**
     * 优秀率偏高班级数
     */
    private Integer scoreTooHighCount;
}
