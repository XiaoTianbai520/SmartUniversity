package com.smart.university.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 教学班状态枚举
 */
@Getter
@AllArgsConstructor
public enum TeachingClassStatusEnum {

    /**
     * 草稿，学生不可见
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 开放选课
     */
    AVAILABLE("AVAILABLE", "开放选课"),

    /**
     * 已关闭，停止选课
     */
    CLOSED("CLOSED", "已关闭"),

    /**
     * 已取消
     */
    CANCELLED("CANCELLED", "已取消");

    /**
     * 数据库存储值
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据数据库存储值获取枚举
     *
     * @param code 数据库存储值
     * @return 匹配的枚举
     */
    public static TeachingClassStatusEnum getByCode(String code) {
        for (TeachingClassStatusEnum each : values()) {
            if (each.code.equals(code)) {
                return each;
            }
        }
        return null;
    }

    /**
     * 判断当前状态是否允许学生选课
     *
     * @return 允许选课返回 true
     */
    public boolean allowSelect() {
        return this == AVAILABLE;
    }
}
