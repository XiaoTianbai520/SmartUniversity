package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 选课热度预测明细出参（40.2 records）
 */
@Data
public class EnrollmentForecastItemRespDTO {

    /**
     * 课程 ID
     */
    @Schema(example = "1")
    private Long courseId;

    /**
     * 课程编号
     */
    private String courseCode;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 本学期该课程的教学班数
     */
    private Integer teachingClassCount;

    /**
     * 本学期该课程的总容量
     */
    private Integer currentCapacity;

    /**
     * 已选人数（DB 权威口径，C1）
     */
    private Integer selectedCount;

    /**
     * 候补人数
     */
    private Integer waitingCount;

    /**
     * 历史同期已选人次均值，保留 1 位小数
     */
    private BigDecimal historyAvgSelected;

    /**
     * 历史样本期数
     */
    private Integer historySampleCount;

    /**
     * 历史样本是否不足
     */
    private Boolean sampleInsufficient;

    /**
     * 预测需求人数
     */
    private Integer forecastDemand;

    /**
     * 建议容量增量，仅大于 0 时给出建议
     */
    private Integer suggestedCapacityIncrement;

    /**
     * 是否存在爆满风险
     */
    private Boolean fullRisk;

    /**
     * 预测依据说明
     */
    private String reason;
}
