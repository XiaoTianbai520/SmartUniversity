package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 毕业风险按课程类型的学分缺口出参（36.2 / 40.1 gaps）
 */
@Data
public class GraduationRiskGapRespDTO {

    /**
     * 课程类型编码：REQUIRED / MAJOR_ELECTIVE / PUBLIC_ELECTIVE
     */
    @Schema(example = "REQUIRED")
    private String courseType;

    /**
     * 课程类型名称
     */
    private String courseTypeName;

    /**
     * 该类型学分缺口，下限为 0
     */
    private BigDecimal gapCredit;
}
