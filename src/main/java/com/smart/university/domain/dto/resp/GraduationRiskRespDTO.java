package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 毕业风险预测出参（36.2），completedCredit 为跨学期累计口径（C8），与 36.1 同源同值
 */
@Data
public class GraduationRiskRespDTO {

    /**
     * 统计截止学期 ID
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 学生 ID，教务端名单接口返回，学生端为空
     */
    private Long studentId;

    /**
     * 学号，教务端名单接口返回
     */
    private String studentNo;

    /**
     * 学生姓名，教务端名单接口返回
     */
    private String studentName;

    /**
     * 专业名称，教务端名单接口返回
     */
    private String majorName;

    /**
     * 年级名称，教务端名单接口返回
     */
    private String gradeName;

    /**
     * 是否存在启用中的培养方案
     */
    private Boolean hasPlan;

    /**
     * 无法评估时的说明文案，可评估时为空串
     */
    private String message;

    /**
     * 风险等级：LOW / MEDIUM / HIGH
     */
    @Schema(example = "MEDIUM")
    private String riskLevel;

    /**
     * 风险原因说明
     */
    private String riskReason;

    /**
     * 已完成总学分（跨学期累计，已发布即计入）
     */
    private BigDecimal completedCredit;

    /**
     * 及格学分（附加展示字段，不参与风险计算）
     */
    private BigDecimal passedCredit;

    /**
     * 培养方案毕业总学分要求
     */
    private BigDecimal planTotalCredit;

    /**
     * 总体完成度百分比，保留 1 位小数
     */
    private BigDecimal progressRate;

    /**
     * 剩余需修学分，下限为 0
     */
    private BigDecimal remainingCredit;

    /**
     * 剩余学期数，最小为 1
     */
    private Integer remainingSemesters;

    /**
     * 剩余学期内每学期需修学分
     */
    private BigDecimal requiredCreditPerSemester;

    /**
     * 学期最大学分上限
     */
    private BigDecimal maxCreditPerSemester;

    /**
     * 需求速率 = 每学期需修学分 / 学期最大学分上限
     */
    private BigDecimal requiredRate;

    /**
     * 按课程类型的学分缺口
     */
    private List<GraduationRiskGapRespDTO> gaps;
}
