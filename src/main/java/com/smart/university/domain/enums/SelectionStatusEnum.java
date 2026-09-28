package com.smart.university.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 选课记录状态枚举
 */
@Getter
@AllArgsConstructor
public enum SelectionStatusEnum {

    /**
     * 已选
     */
    SELECTED("SELECTED", "已选"),

    /**
     * 已退课
     */
    WITHDRAWN("WITHDRAWN", "已退课"),

    /**
     * 候补中
     */
    WAITING("WAITING", "候补中");

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
    public static SelectionStatusEnum getByCode(String code) {
        for (SelectionStatusEnum each : values()) {
            if (each.code.equals(code)) {
                return each;
            }
        }
        return null;
    }
}
