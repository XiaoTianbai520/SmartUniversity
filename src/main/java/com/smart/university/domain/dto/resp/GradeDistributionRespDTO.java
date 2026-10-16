package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 年级分布出参（37.3 extra.gradeDistribution）
 */
@Data
public class GradeDistributionRespDTO {

    /**
     * 年级 ID
     */
    @Schema(example = "1")
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 该年级的已选人次
     */
    private Integer count;
}
