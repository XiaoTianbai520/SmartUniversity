package com.smart.university.domain.dto.excel;

import lombok.Data;

/**
 * 学生导入行模型，与 {@code ExcelWriteUtil.STUDENT_IMPORT_HEADERS} 列顺序一一对应
 */
@Data
public class StudentExcelRowDTO {

    /**
     * 行号，从数据第一行计为 1
     */
    private Integer rowIndex;

    /**
     * 学号，同时作为登录账号
     */
    private String studentNo;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 专业编号，关联 major.major_code
     */
    private String majorCode;

    /**
     * 年级名称，关联 grade_cohort.grade_name
     */
    private String gradeName;

    /**
     * 行政班级编号，关联 academic_class.class_code
     */
    private String classCode;

    /**
     * 初始密码，留空时取系统默认密码
     */
    private String password;
}
