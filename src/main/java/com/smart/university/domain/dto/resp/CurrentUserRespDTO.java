package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 当前登录用户出参
 */
@Data
public class CurrentUserRespDTO {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 角色
     */
    private String role;

    /**
     * 学生 ID，教师和管理员为空
     */
    private Long studentId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 姓名
     */
    private String name;

    /**
     * 专业 ID
     */
    private Long majorId;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 年级 ID
     */
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 行政班级 ID
     */
    private Long classId;

    /**
     * 行政班级名称
     */
    private String className;
}
