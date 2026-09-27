package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * grade_cohort 表
 */
@Data
@TableName("grade_cohort")
public class GradeCohortDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 年级名称，例如2026级
     */
    private String gradeName;

    /**
     * 入学年份
     */
    private Integer entryYear;

    /**
     * 状态：1启用，0禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
