package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 学生出参
 */
@Data
public class StudentRespDTO {

    /**
     * 学生 ID
     */
    private Long studentId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 学生姓名
     */
    private String studentName;

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

    /**
     * 状态：1 正常，0 停用
     */
    private Integer status;
}
