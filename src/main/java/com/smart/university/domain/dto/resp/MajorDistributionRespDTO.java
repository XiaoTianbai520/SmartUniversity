package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 专业分布出参（37.3 extra.majorDistribution）
 */
@Data
public class MajorDistributionRespDTO {

    /**
     * 专业 ID
     */
    @Schema(example = "1")
    private Long majorId;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 该专业的已选人次
     */
    private Integer count;
}
