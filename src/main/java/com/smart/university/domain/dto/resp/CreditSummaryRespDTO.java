package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 学分统计出参
 */
@Data
public class CreditSummaryRespDTO {

    /**
     * 学期 ID
     */
    private Long semesterId;

    /**
     * 本学期最大可选学分
     */
    private BigDecimal maxSelectionCredit;

    /**
     * 本学期已选学分
     */
    private BigDecimal currentSelectedCredit;

    /**
     * 已修得学分
     */
    private BigDecimal completedCredit;

    /**
     * 必修课学分
     */
    private BigDecimal requiredCredit;

    /**
     * 专业选修课学分
     */
    private BigDecimal majorElectiveCredit;

    /**
     * 公共选修课学分
     */
    private BigDecimal publicElectiveCredit;
}
