package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学分风险名单出参（42.1），学分口径为跨学期累计（C8），与 36.1 / 36.2 / 40.1 同源同值
 */
@Data
public class CreditWarningRespDTO {

    /**
     * 学生 ID
     */
    @Schema(example = "1")
    private Long studentId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 风险等级：LOW / MEDIUM / HIGH
     */
    private String riskLevel;

    /**
     * 已完成总学分（跨学期累计，已发布即计入）
     */
    private BigDecimal completedCredit;

    /**
     * 及格学分（附加展示字段）
     */
    private BigDecimal passedCredit;

    /**
     * 培养方案毕业总学分要求
     */
    private BigDecimal planTotalCredit;

    /**
     * 完成度百分比
     */
    private BigDecimal progressRate;

    /**
     * 剩余需修学分
     */
    private BigDecimal remainingCredit;

    /**
     * 剩余学期数
     */
    private Integer remainingSemesters;

    /**
     * 每学期需修学分
     */
    private BigDecimal requiredCreditPerSemester;

    /**
     * 需求速率
     */
    private BigDecimal requiredRate;

    /**
     * 按课程类型的学分缺口
     */
    private List<GraduationRiskGapRespDTO> gaps;

    /**
     * 是否已发送过预警通知
     */
    private Boolean warned;

    /**
     * 最近一次预警时间
     */
    private LocalDateTime warnedAt;
}
