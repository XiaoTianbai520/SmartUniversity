package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 教学班成绩统计出参
 */
@Data
public class ScoreStatisticsRespDTO {

    /**
     * 教学班 ID
     */
    private Long teachingClassId;

    /**
     * 教学班名称
     */
    private String teachingClassName;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 任课教师姓名
     */
    private String teacherName;

    /**
     * 正式选课人数
     */
    private Integer selectedCount;

    /**
     * 已录入成绩人数
     */
    private Integer scoredCount;

    /**
     * 未录入成绩人数
     */
    private Integer unscoredCount;

    /**
     * 平均分，保留一位小数
     */
    private BigDecimal averageScore;

    /**
     * 最高分
     */
    private BigDecimal maxScore;

    /**
     * 最低分
     */
    private BigDecimal minScore;

    /**
     * 及格率，百分比保留一位小数
     */
    private BigDecimal passRate;

    /**
     * 优秀率，百分比保留一位小数
     */
    private BigDecimal excellentRate;

    /**
     * 成绩分布，固定返回五段
     */
    private List<ScoreDistributionItemDTO> distribution;
}
