package com.smart.university.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 课程性质枚举
 */
@Getter
@AllArgsConstructor
public enum CourseTypeEnum {

    /**
     * 必修课
     */
    REQUIRED("REQUIRED", "必修课"),

    /**
     * 专业选修课
     */
    MAJOR_ELECTIVE("MAJOR_ELECTIVE", "专业选修课"),

    /**
     * 公共选修课
     */
    PUBLIC_ELECTIVE("PUBLIC_ELECTIVE", "公共选修课");

    /**
     * 数据库存储值
     */
    private final String code;

    /**
     * 课程性质描述
     */
    private final String description;

    /**
     * 根据数据库存储值获取枚举
     *
     * @param code 数据库存储值
     * @return 匹配的枚举
     */
    public static CourseTypeEnum getByCode(String code) {
        for (CourseTypeEnum each : values()) {
            if (each.code.equals(code)) {
                return each;
            }
        }
        return null;
    }
}
