package com.smart.university.common.util;

import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Excel 写出工具，同时承载模板与导入解析共用的列定义常量，避免模板与解析两边漂移
 */
public final class ExcelWriteUtil {

    /**
     * 学生导入模板表头
     */
    public static final String[] STUDENT_IMPORT_HEADERS = {"学号", "姓名", "专业编号", "年级名称", "班级编号", "初始密码"};

    /**
     * 学生信息导出表头
     */
    public static final String[] STUDENT_EXPORT_HEADERS = {"学号", "姓名", "专业", "年级", "班级", "学生状态", "账号状态"};

    /**
     * 教师导入模板表头
     */
    public static final String[] TEACHER_IMPORT_HEADERS = {"教师编号", "姓名", "职称", "初始密码"};

    /**
     * 教师信息导出表头
     */
    public static final String[] TEACHER_EXPORT_HEADERS = {"教师编号", "姓名", "职称", "教师状态", "账号状态"};

    /**
     * 成绩导入模板表头
     */
    public static final String[] SCORE_IMPORT_HEADERS = {"学号", "姓名", "成绩"};

    /**
     * 教学班学生名单导出表头
     */
    public static final String[] TEACHING_CLASS_STUDENT_HEADERS = {"学号", "姓名", "专业", "班级"};

    /**
     * 学生导入列下标
     */
    public static final int STUDENT_NO_COLUMN_INDEX = 0;

    /**
     * 学生导入姓名列下标
     */
    public static final int STUDENT_NAME_COLUMN_INDEX = 1;

    /**
     * 学生导入专业编号列下标
     */
    public static final int MAJOR_CODE_COLUMN_INDEX = 2;

    /**
     * 学生导入年级名称列下标
     */
    public static final int GRADE_NAME_COLUMN_INDEX = 3;

    /**
     * 学生导入班级编号列下标
     */
    public static final int CLASS_CODE_COLUMN_INDEX = 4;

    /**
     * 学生导入初始密码列下标
     */
    public static final int STUDENT_PASSWORD_COLUMN_INDEX = 5;

    /**
     * 教师导入教师编号列下标
     */
    public static final int TEACHER_NO_COLUMN_INDEX = 0;

    /**
     * 教师导入姓名列下标
     */
    public static final int TEACHER_NAME_COLUMN_INDEX = 1;

    /**
     * 教师导入职称列下标
     */
    public static final int TEACHER_TITLE_COLUMN_INDEX = 2;

    /**
     * 教师导入初始密码列下标
     */
    public static final int TEACHER_PASSWORD_COLUMN_INDEX = 3;

    /**
     * 成绩导入学号列下标
     */
    public static final int SCORE_STUDENT_NO_COLUMN_INDEX = 0;

    /**
     * 成绩导入姓名列下标
     */
    public static final int SCORE_STUDENT_NAME_COLUMN_INDEX = 1;

    /**
     * 成绩导入成绩列下标
     */
    public static final int SCORE_VALUE_COLUMN_INDEX = 2;

    private ExcelWriteUtil() {
    }

    /**
     * 生成单页 xlsx 字节数组，首行为加粗表头，其余为数据行
     *
     * @param sheetName 工作表名称
     * @param headers   表头列定义
     * @param rows      数据行，每行元素个数与表头一致
     * @return xlsx 文件字节数组
     */
    public static byte[] writeSheet(String sheetName, String[] headers, List<List<String>> rows) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = buildHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int columnIndex = 0; columnIndex < headers.length; columnIndex++) {
                Cell cell = headerRow.createCell(columnIndex);
                cell.setCellValue(headers[columnIndex]);
                cell.setCellStyle(headerStyle);
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                List<String> rowData = rows.get(rowIndex);
                Row dataRow = sheet.createRow(rowIndex + 1);
                for (int columnIndex = 0; columnIndex < headers.length; columnIndex++) {
                    dataRow.createCell(columnIndex)
                            .setCellValue(columnIndex < rowData.size() ? rowData.get(columnIndex) : "");
                }
            }
            for (int columnIndex = 0; columnIndex < headers.length; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BizException(ResultCodeEnum.SYSTEM_ERROR, "Excel 生成失败");
        }
    }

    /**
     * 构建表头样式，仅加粗不做其他装饰
     *
     * @param workbook 工作簿
     * @return 表头单元格样式
     */
    private static CellStyle buildHeaderStyle(Workbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(boldFont);
        return headerStyle;
    }
}
