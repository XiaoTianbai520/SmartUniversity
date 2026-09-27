package com.smart.university.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统角色枚举
 */
@Getter
@AllArgsConstructor
public enum RoleEnum {

    /**
     * 学生
     */
    STUDENT("STUDENT", "学生"),

    /**
     * 教师
     */
    TEACHER("TEACHER", "教师"),

    /**
     * 教学管理员
     */
    ADMIN("ADMIN", "教学管理员");

    /**
     * 数据库存储值
     */
    private final String code;

    /**
     * 角色描述
     */
    private final String description;

    /**
     * 根据数据库存储值获取枚举
     *
     * @param code 数据库存储值
     * @return 匹配的枚举
     */
    public static RoleEnum getByCode(String code) {
        for (RoleEnum each : values()) {
            if (each.code.equals(code)) {
                return each;
            }
        }
        return null;
    }
}
