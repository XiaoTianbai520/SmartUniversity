package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 行政班级出参
 */
@Data
public class AcademicClassRespDTO {

    /**
     * 班级 ID
     */
    private Long classId;

    /**
     * 班级编号
     */
    private String classCode;

    /**
     * 班级名称
     */
    private String className;

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
     * 状态：1 启用，0 禁用
     */
    private Integer status;
}
