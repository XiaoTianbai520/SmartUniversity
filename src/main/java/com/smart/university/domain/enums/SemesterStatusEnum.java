package com.smart.university.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 学期状态枚举
 */
@Getter
@AllArgsConstructor
public enum SemesterStatusEnum {

    /**
     * 未开始
     */
    PLANNED("PLANNED", "未开始"),

    /**
     * 进行中
     */
    ACTIVE("ACTIVE", "进行中"),

    /**
     * 已结束
     */
    FINISHED("FINISHED", "已结束");

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
    public static SemesterStatusEnum getByCode(String code) {
        for (SemesterStatusEnum each : values()) {
            if (each.code.equals(code)) {
                return each;
            }
        }
        return null;
    }
}
