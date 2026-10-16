package com.smart.university.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 定时任务触发方式枚举，取值与 schedule_task_log.trigger_type 的 CHECK 约束一致
 */
@Getter
@AllArgsConstructor
public enum TriggerTypeEnum {

    /**
     * 定时触发
     */
    SCHEDULED("SCHEDULED", "定时触发"),

    /**
     * 教务端手动触发
     */
    MANUAL("MANUAL", "手动触发");

    /**
     * 数据库存储值
     */
    private final String code;

    /**
     * 触发方式描述
     */
    private final String description;

    /**
     * 根据数据库存储值获取枚举
     *
     * @param code 数据库存储值
     * @return 匹配的枚举，未匹配返回 null
     */
    public static TriggerTypeEnum getByCode(String code) {
        for (TriggerTypeEnum each : values()) {
            if (each.code.equals(code)) {
                return each;
            }
        }
        return null;
    }
}
