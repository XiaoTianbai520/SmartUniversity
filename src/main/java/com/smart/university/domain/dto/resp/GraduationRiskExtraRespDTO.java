package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 毕业风险名单口径回显（40.1 extra）
 */
@Data
public class GraduationRiskExtraRespDTO {

    /**
     * 学期 ID
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 高风险人数
     */
    private Integer highCount;

    /**
     * 中风险人数
     */
    private Integer mediumCount;

    /**
     * 低风险人数
     */
    private Integer lowCount;

    /**
     * 无培养方案人数
     */
    private Integer noPlanCount;
}
