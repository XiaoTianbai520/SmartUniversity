package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成绩分布异常预警出参（39.2 / 40.3 records）
 */
@Data
public class ScoreAnomalyRespDTO {

    /**
     * 教学班 ID
     */
    @Schema(example = "1")
    private Long teachingClassId;

    /**
     * 教学班编号
     */
    private String classCode;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 任课教师姓名，教务端返回
     */
    private String teacherName;

    /**
     * 学生主要专业名称，教务端返回
     */
    private String majorName;

    /**
     * 已选人数（DB 权威口径，C1）
     */
    private Integer selectedCount;

    /**
     * 已录入成绩人数
     */
    private Integer scoredCount;

    /**
     * 成绩录入完整率百分比，保留 1 位小数
     */
    private BigDecimal inputCompleteRate;

    /**
     * 及格率百分比，保留 1 位小数
     */
    private BigDecimal passRate;

    /**
     * 优秀率百分比，保留 1 位小数
     */
    private BigDecimal excellentRate;

    /**
     * 同课程历史及格率基线，保留 1 位小数
     */
    private BigDecimal baselinePassRate;

    /**
     * 与基线的偏差（百分点），保留 1 位小数
     */
    private BigDecimal deviation;

    /**
     * 异常类型集合：FAIL_RATE_HIGH / SCORE_MISSING / SCORE_TOO_HIGH
     */
    private List<String> anomalyTypes;

    /**
     * 异常原因说明
     */
    private String anomalyReason;

    /**
     * 成绩分段分布
     */
    private List<ScoreDistributionRespDTO> distribution;
}
