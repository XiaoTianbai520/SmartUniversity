package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 教师出参
 */
@Data
public class TeacherRespDTO {

    /**
     * 教师 ID
     */
    private Long teacherId;

    /**
     * 教师编号
     */
    private String teacherNo;

    /**
     * 教师姓名
     */
    private String teacherName;

    /**
     * 职称
     */
    private String title;

    /**
     * 状态：1 正常，0 停用
     */
    private Integer status;
}
