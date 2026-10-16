package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 培养方案出参
 */
@Data
public class TrainingPlanRespDTO {

    /**
     * 培养方案 ID
     */
    @Schema(example = "1")
    private Long planId;

    /**
     * 培养方案名称
     */
    private String planName;

    /**
     * 专业 ID
     */
    private Long majorId;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 年级 ID
     */
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 入学年份
     */
    private Integer entryYear;

    /**
     * 毕业总学分要求
     */
    private BigDecimal totalCredit;

    /**
     * 必修学分要求
     */
    private BigDecimal requiredCredit;

    /**
     * 专业选修学分要求
     */
    private BigDecimal majorElectiveCredit;

    /**
     * 公共选修学分要求
     */
    private BigDecimal publicElectiveCredit;

    /**
     * 状态：1 启用，0 停用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
