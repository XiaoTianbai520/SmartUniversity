package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.smart.university.domain.enums.ScoreStatusEnum;

/**
 * score 表
 */
@Data
@TableName("score")
public class ScoreDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
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
