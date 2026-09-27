package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.smart.university.domain.enums.ScoreStatusEnum;

/**
 * 学生成绩表
 */
@Data
public class ScoreDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 成绩ID
     */
    private Long id;

    /**
     * 对应选课记录ID
     */
    private Long courseSelectionId;

    /**
     * 百分制成绩
     */
    private BigDecimal scoreValue;

    /**
     * UNPUBLISHED/PUBLISHED
     */
    private ScoreStatusEnum status;

    /**
     * 成绩发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
