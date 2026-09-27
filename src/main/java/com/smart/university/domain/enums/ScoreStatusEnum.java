package com.smart.university.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 成绩状态枚举
 */
@Getter
@AllArgsConstructor
public enum ScoreStatusEnum {

    /**
     * 未发布，学生不可见
     */
    UNPUBLISHED("UNPUBLISHED", "未发布"),

    /**
     * 已发布，学生可见且不允许再修改
     */
    PUBLISHED("PUBLISHED", "已发布");

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
    public static ScoreStatusEnum getByCode(String code) {
        for (ScoreStatusEnum each : values()) {
            if (each.code.equals(code)) {
                return each;
            }
        }
        return null;
    }
}
