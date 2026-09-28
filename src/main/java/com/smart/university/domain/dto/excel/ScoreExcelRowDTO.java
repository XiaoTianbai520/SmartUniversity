package com.smart.university.domain.dto.excel;

import lombok.Data;

/**
 * 成绩导入行模型，与 {@code ExcelWriteUtil.SCORE_IMPORT_HEADERS} 列顺序一一对应
 */
@Data
public class ScoreExcelRowDTO {

    /**
     * 行号，从数据第一行计为 1
     */
    private Integer rowIndex;

    /**
     * 学号，用于定位教学班内的选课记录
     */
    private String studentNo;

    /**
     * 学生姓名，仅用于与系统内姓名比对，防止填错行
     */
    private String studentName;

    /**
     * 成绩文本，解析后必须为 0 到 100 的数值
     */
    private String score;
}
