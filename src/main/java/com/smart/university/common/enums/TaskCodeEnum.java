package com.smart.university.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 定时任务编码枚举，取值与 schedule_task_log.task_code 一致，任务清单见架构设计 §6.2
 */
@Getter
@AllArgsConstructor
public enum TaskCodeEnum {

    /**
     * 选课批次状态自动流转
     */
    BATCH_STATUS_TASK("BATCH_STATUS_TASK", "选课批次状态自动流转", "fixedDelay=60000（1 分钟）"),

    /**
     * 候补递补兜底扫描
     */
    WAITLIST_SCAN_TASK("WAITLIST_SCAN_TASK", "候补递补兜底扫描", "fixedDelay=300000（5 分钟）"),

    /**
     * 统计快照预计算
     */
    STAT_SNAPSHOT_TASK("STAT_SNAPSHOT_TASK", "统计快照预计算", "cron=0 0 2 * * ?（每日 02:00）"),

    /**
     * 看板缓存刷新
     */
    DASHBOARD_REFRESH_TASK("DASHBOARD_REFRESH_TASK", "看板缓存刷新", "fixedDelay=600000（10 分钟）"),

    /**
     * 学分不足 / 毕业风险预警
     */
    CREDIT_WARNING_TASK("CREDIT_WARNING_TASK", "学分不足与毕业风险预警", "cron=0 0 9 ? * MON（每周一 09:00）"),

    /**
     * 成绩发布后自动统计，事件触发
     */
    SCORE_STAT_EVENT("SCORE_STAT_EVENT", "成绩发布后自动统计", "成绩发布事务提交后触发"),

    /**
     * 异常数据巡检
     */
    DATA_AUDIT_TASK("DATA_AUDIT_TASK", "异常数据巡检", "cron=0 0 3 * * ?（每日 03:00）"),

    /**
     * 学期末数据归档，默认关闭
     */
    SEMESTER_ARCHIVE_TASK("SEMESTER_ARCHIVE_TASK", "学期末数据归档", "cron=0 0 4 * * ?（每日 04:00，默认关闭）"),

    /**
     * 缓存一致性对账
     */
    CACHE_RECONCILE_TASK("CACHE_RECONCILE_TASK", "缓存一致性对账", "fixedDelay=1800000（30 分钟）"),

    /**
     * 容量缓存预热
     */
    SELECTION_CACHE_WARMUP_TASK("SELECTION_CACHE_WARMUP_TASK", "容量缓存预热", "cron=0 0/30 * * * ?（每 30 分钟）");

    /**
     * 数据库存储值
     */
    private final String code;

    /**
     * 任务中文名
     */
    private final String taskName;

    /**
     * 默认周期说明，实际周期以配置项的 cron / fixed-delay 为准
     */
    private final String defaultPeriod;

    /**
     * 根据数据库存储值获取枚举
     *
     * @param code 数据库存储值
     * @return 匹配的枚举，未匹配返回 null
     */
    public static TaskCodeEnum getByCode(String code) {
        for (TaskCodeEnum each : values()) {
            if (each.code.equals(code)) {
                return each;
            }
        }
        return null;
    }
}
