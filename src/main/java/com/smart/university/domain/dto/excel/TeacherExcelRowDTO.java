package com.smart.university.domain.dto.excel;

import lombok.Data;

/**
 * 教师导入行模型，与 {@code ExcelWriteUtil.TEACHER_IMPORT_HEADERS} 列顺序一一对应
 */
@Data
public class TeacherExcelRowDTO {

    /**
     * 行号，从数据第一行计为 1
     */
    private Integer rowIndex;

    /**
     * 教师编号，同时作为登录账号
     */
    private String teacherNo;

    /**
     * 教师姓名
     */
    private String teacherName;

    /**
     * 职称，选填
     */
    private String title;

    /**
     * 初始密码，留空时取系统默认密码
     */
    private String password;
}
