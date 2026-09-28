package com.smart.university.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 站内通知类型枚举，取值与 notification 表 CHECK 约束保持一致
 */
@Getter
@AllArgsConstructor
public enum NoticeTypeEnum {

    /**
     * 选课开始通知
     */
    SELECTION_START("SELECTION_START", "选课开始通知"),

    /**
     * 选课结束提醒
     */
    SELECTION_END("SELECTION_END", "选课结束提醒"),

    /**
     * 候补成功通知
     */
    WAITLIST_PROMOTED("WAITLIST_PROMOTED", "候补成功通知"),

    /**
     * 课程调整通知
     */
    COURSE_ADJUSTED("COURSE_ADJUSTED", "课程调整通知"),

    /**
     * 成绩发布通知
     */
    SCORE_PUBLISHED("SCORE_PUBLISHED", "成绩发布通知");

    /**
     * 数据库存储值
     */
    private final String code;

    /**
     * 通知类型描述，同时作为默认通知标题
     */
    private final String description;

    /**
     * 根据数据库存储值获取枚举
     *
     * @param code 数据库存储值
     * @return 匹配的枚举
     */
    public static NoticeTypeEnum getByCode(String code) {
        for (NoticeTypeEnum each : values()) {
            if (each.code.equals(code)) {
                return each;
            }
        }
        return null;
    }

    /**
     * 获取该通知类型的默认标题
     *
     * @return 默认标题
     */
    public String getDefaultTitle() {
        return description;
    }
}
