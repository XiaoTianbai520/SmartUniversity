package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学生学分进度出参（36.1），completedCredit 为跨学期累计口径（C8）
 */
@Data
public class CreditProgressRespDTO {

    /**
     * 统计截止学期 ID，不参与学分累计过滤
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 是否存在本专业本年级的启用中培养方案
     */
    private Boolean hasPlan;

    /**
     * 无培养方案时的提示文案，有方案时为空串
     */
    private String message;

    /**
     * 已完成总学分（跨学期累计，已发布即计入，不判及格）
     */
    private BigDecimal completedCredit;

    /**
     * 及格学分（跨学期累计，成绩不低于及格线，仅作展示，不参与完成度与风险计算）
     */
    private BigDecimal passedCredit;

    /**
     * 培养方案毕业总学分要求，无方案时为 0
     */
    private BigDecimal planTotalCredit;

    /**
     * 总体完成度百分比，0~100，保留 1 位小数
     */
    private BigDecimal progressRate;

    /**
     * 分课程类型的完成情况
     */
    private List<CreditProgressItemRespDTO> items;
}
