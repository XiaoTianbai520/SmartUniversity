package com.smart.university.domain.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 毕业风险名单查询入参（40.1）
 */
@Data
public class GraduationRiskQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学期 ID，缺省当前学期
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 专业 ID
     */
    @Schema(example = "1")
    private Long majorId;

    /**
     * 年级 ID
     */
    @Schema(example = "1")
    private Long gradeId;

    /**
     * 风险等级筛选：LOW / MEDIUM / HIGH
     */
    @Schema(example = "HIGH")
    private String riskLevel;
}
