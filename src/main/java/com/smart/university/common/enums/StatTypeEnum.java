package com.smart.university.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统计快照类型枚举，取值与 stat_* 系列快照表一一对应
 */
@Getter
@AllArgsConstructor
public enum StatTypeEnum {

    /**
     * 教室利用率统计，对应 stat_classroom_usage
     */
    CLASSROOM_USAGE("CLASSROOM_USAGE", "教室利用率统计"),

    /**
     * 教师工作量统计，对应 stat_teacher_workload
     */
    TEACHER_WORKLOAD("TEACHER_WORKLOAD", "教师工作量统计"),

    /**
     * 课程选课分析，对应 stat_course_selection
     */
    COURSE_SELECTION("COURSE_SELECTION", "课程选课分析"),

    /**
     * 学生学分进度，对应 stat_credit_progress
     */
    CREDIT_PROGRESS("CREDIT_PROGRESS", "学生学分进度"),

    /**
     * 教务看板总览，对应 stat_semester_overview
     */
    OVERVIEW("OVERVIEW", "教务看板总览");

    /**
     * 数据库存储值
     */
    private final String code;

    /**
     * 统计类型描述
     */
    private final String description;

    /**
     * 根据数据库存储值获取枚举
     *
     * @param code 数据库存储值
     * @return 匹配的枚举，未匹配返回 null
     */
    public static StatTypeEnum getByCode(String code) {
        for (StatTypeEnum each : values()) {
            if (each.code.equals(code)) {
                return each;
            }
        }
        return null;
    }
}
