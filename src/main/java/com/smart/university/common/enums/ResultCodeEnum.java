package com.smart.university.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务状态码与错误码枚举，与接口文档 V1 保持一致
 */
@Getter
@AllArgsConstructor
public enum ResultCodeEnum {

    /**
     * 请求成功
     */
    SUCCESS(200, "success"),

    /**
     * 服务器内部异常
     */
    SYSTEM_ERROR(500, "服务器内部异常"),

    /**
     * 参数错误
     */
    PARAM_ERROR(40001, "参数错误"),

    /**
     * 请求数据格式错误
     */
    FORMAT_ERROR(40002, "请求数据格式错误"),

    /**
     * 未登录
     */
    NOT_LOGIN(40101, "未登录"),

    /**
     * Token 已失效
     */
    TOKEN_INVALID(40102, "Token 已失效"),

    /**
     * 无接口访问权限
     */
    NO_API_PERMISSION(40301, "无接口访问权限"),

    /**
     * 无课程查看权限
     */
    NO_COURSE_VIEW_PERMISSION(40302, "无课程查看权限"),

    /**
     * 无教学班操作权限
     */
    NO_TEACHING_CLASS_PERMISSION(40303, "无教学班操作权限"),

    /**
     * 用户不存在
     */
    USER_NOT_EXIST(40401, "用户不存在"),

    /**
     * 学生不存在
     */
    STUDENT_NOT_EXIST(40402, "学生不存在"),

    /**
     * 教师不存在
     */
    TEACHER_NOT_EXIST(40403, "教师不存在"),

    /**
     * 课程不存在
     */
    COURSE_NOT_EXIST(40404, "课程不存在"),

    /**
     * 教学班不存在
     */
    TEACHING_CLASS_NOT_EXIST(40405, "教学班不存在"),

    /**
     * 选课批次不存在
     */
    SELECTION_BATCH_NOT_EXIST(40406, "选课批次不存在"),

    /**
     * 选课记录不存在
     */
    SELECTION_NOT_EXIST(40407, "选课记录不存在"),

    /**
     * 用户名已存在
     */
    USERNAME_EXIST(40901, "用户名已存在"),

    /**
     * 学号已存在
     */
    STUDENT_NO_EXIST(40902, "学号已存在"),

    /**
     * 教师编号已存在
     */
    TEACHER_NO_EXIST(40903, "教师编号已存在"),

    /**
     * 课程编号已存在
     */
    COURSE_CODE_EXIST(40904, "课程编号已存在"),

    /**
     * 教学班编号已存在
     */
    TEACHING_CLASS_CODE_EXIST(40905, "教学班编号已存在"),

    /**
     * 教师排课冲突
     */
    TEACHER_SCHEDULE_CONFLICT(40906, "教师排课冲突"),

    /**
     * 教室排课冲突
     */
    CLASSROOM_SCHEDULE_CONFLICT(40907, "教室排课冲突"),

    /**
     * 学生课程时间冲突
     */
    STUDENT_SCHEDULE_CONFLICT(40908, "学生课程时间冲突"),

    /**
     * 重复选课
     */
    DUPLICATE_SELECTION(40909, "重复选课"),

    /**
     * 课程容量已满
     */
    CLASS_CAPACITY_FULL(40910, "课程容量已满"),

    /**
     * 超过最大学分限制
     */
    CREDIT_LIMIT_EXCEEDED(40911, "超过最大学分限制"),

    /**
     * 当前不在选课时间
     */
    NOT_IN_SELECTION_TIME(40912, "当前不在选课时间"),

    /**
     * 当前不允许退课
     */
    WITHDRAW_NOT_ALLOWED(40913, "当前不允许退课"),

    /**
     * 专业不符合选课条件
     */
    MAJOR_NOT_ALLOWED(40914, "专业不符合选课条件"),

    /**
     * 年级不符合选课条件
     */
    GRADE_NOT_ALLOWED(40915, "年级不符合选课条件"),

    /**
     * 成绩已发布，不允许修改
     */
    SCORE_PUBLISHED(40916, "成绩已发布，不允许修改"),

    /**
     * 学生不属于该教学班
     */
    STUDENT_NOT_IN_CLASS(40917, "学生不属于该教学班");

    /**
     * 业务错误码
     */
    private final int code;

    /**
     * 错误描述
     */
    private final String message;

    /**
     * 根据错误码获取枚举
     *
     * @param code 业务错误码
     * @return 匹配的枚举，未匹配返回 null
     */
    public static ResultCodeEnum getByCode(int code) {
        for (ResultCodeEnum each : values()) {
            if (each.code == code) {
                return each;
            }
        }
        return null;
    }
}
