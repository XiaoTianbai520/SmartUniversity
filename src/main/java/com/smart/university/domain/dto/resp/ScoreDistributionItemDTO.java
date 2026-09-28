package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 成绩分布区间出参
 */
@Data
public class ScoreDistributionItemDTO {

    /**
     * 区间名称，如 0-59
     */
    private String range;

    /**
     * 该区间人数
     */
    private Integer count;

    /**
     * 该区间占比，百分比保留一位小数
     */
    private BigDecimal rate;
}
