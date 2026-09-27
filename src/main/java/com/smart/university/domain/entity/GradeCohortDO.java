package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 年级表
 */
@Data
public class GradeCohortDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 年级ID
     */
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
