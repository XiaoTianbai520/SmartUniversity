package com.smart.university.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用启用状态枚举
 */
@Getter
@AllArgsConstructor
public enum EnableStatusEnum {

    /**
     * 停用
     */
    DISABLED(0, "停用"),

    /**
     * 启用
     */
    ENABLED(1, "启用");

    /**
     * 数据库存储值
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String description;
}
