package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 学分进度按课程类型的分项出参（36.1 items）
 */
@Data
public class CreditProgressItemRespDTO {

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
     * 该类型已完成学分（跨学期累计，已发布即计入）
     */
    private BigDecimal completedCredit;

    /**
     * 该类型及格学分（附加展示字段）
     */
    private BigDecimal passedCredit;

    /**
     * 该类型培养方案要求学分
     */
    private BigDecimal requiredCredit;

    /**
     * 该类型学分缺口，下限为 0
     */
    private BigDecimal gapCredit;

    /**
     * 该类型完成度百分比，0~100，保留 1 位小数
     */
    private BigDecimal progressRate;
}
