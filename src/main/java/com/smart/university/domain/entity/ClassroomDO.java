package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 教室表
 */
@Data
public class ClassroomDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 教室ID
     */
    private Long id;

    /**
     * 教学楼名称
     */
    private String buildingName;

    /**
     * 教室编号
     */
    private String roomNo;

    /**
     * 教室容量
     */
    private Integer capacity;

    /**
     * 状态：1可用，0停用
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
