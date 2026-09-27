package com.smart.university.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 选课批次状态枚举
 */
@Getter
@AllArgsConstructor
public enum SelectionBatchStatusEnum {

    /**
     * 未开始
     */
    NOT_STARTED("NOT_STARTED", "未开始"),

    /**
     * 进行中
     */
    IN_PROGRESS("IN_PROGRESS", "进行中"),

    /**
     * 已结束
     */
    ENDED("ENDED", "已结束");

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
    public static SelectionBatchStatusEnum getByCode(String code) {
        for (SelectionBatchStatusEnum each : values()) {
            if (each.code.equals(code)) {
                return each;
            }
        }
        return null;
    }
}
