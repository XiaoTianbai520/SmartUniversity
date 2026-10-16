package com.smart.university.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 定时任务执行状态枚举，取值与 schedule_task_log.status 的 CHECK 约束一致
 */
@Getter
@AllArgsConstructor
public enum TaskExecuteStatusEnum {

    /**
     * 执行成功
     */
    SUCCESS("SUCCESS", "执行成功"),

    /**
     * 执行失败
     */
    FAILED("FAILED", "执行失败"),

    /**
     * 本轮跳过，通常为未抢到分布式锁或任务已停用
     */
    SKIPPED("SKIPPED", "本轮跳过"),

    /**
     * 执行中，任务开始时落库的状态
     */
    RUNNING("RUNNING", "执行中");

    /**
     * 数据库存储值
     */
    private final String code;

    /**
     * 执行状态描述
     */
    private final String description;

    /**
     * 根据数据库存储值获取枚举
     *
     * @param code 数据库存储值
     * @return 匹配的枚举，未匹配返回 null
     */
    public static TaskExecuteStatusEnum getByCode(String code) {
        for (TaskExecuteStatusEnum each : values()) {
            if (each.code.equals(code)) {
                return each;
            }
        }
        return null;
    }
}
