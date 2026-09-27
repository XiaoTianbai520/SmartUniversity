package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 专业表
 */
@Data
public class MajorDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 专业ID
     */
    private Long id;

    /**
     * 专业编号
     */
    private String majorCode;

    /**
     * 专业名称
     */
    private String majorName;

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
