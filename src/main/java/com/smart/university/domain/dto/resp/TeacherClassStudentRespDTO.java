package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 教学班学生名单出参
 */
@Data
public class TeacherClassStudentRespDTO {

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
     * 专业名称
     */
    private String majorName;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 行政班级名称
     */
    private String className;
}
