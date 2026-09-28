package com.smart.university.common.util;

import cn.hutool.core.util.StrUtil;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import lombok.Getter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 读取工具，把上传的 xlsx 解析为按行组织的单元格文本，统一承担文件类型、文件空、解析异常与表头校验
 */
public final class ExcelParseUtil {

    /**
     * 允许上传的文件后缀，只支持 Office 2007 之后的 xlsx
     */
    private static final String XLSX_SUFFIX = ".xlsx";

    /**
     * 整数单元格按原值输出的最大绝对值，超过该值仍按 Excel 展示值读取
     */
    private static final double MAX_INTEGER_CELL_VALUE = 1E15;

    private ExcelParseUtil() {
    }

    /**
     * 读取导入文件的数据行，首行按表头处理并与期望列定义逐个比对
     *
     * @param file          上传的导入文件
     * @param expectHeaders 期望的表头列定义，与模板下载共用同一份常量
     * @return 数据行集合，行号从数据第一行计为 1
     */
    public static List<ExcelRow> readDataRows(MultipartFile file, String[] expectHeaders) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCodeEnum.IMPORT_FILE_EMPTY);
        }
        if (!StrUtil.endWithIgnoreCase(file.getOriginalFilename(), XLSX_SUFFIX)) {
            throw new BizException(ResultCodeEnum.IMPORT_FILE_TYPE_ERROR);
        }
        List<ExcelRow> result = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 0) {
                throw new BizException(ResultCodeEnum.IMPORT_FILE_EMPTY);
            }
            DataFormatter dataFormatter = new DataFormatter();
            checkHeader(sheet, expectHeaders, dataFormatter);
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                List<String> cells = listCellText(sheet.getRow(rowNum), expectHeaders.length, dataFormatter);
                if (isBlankRow(cells)) {
                    continue;
                }
                result.add(new ExcelRow(rowNum, cells));
            }
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ResultCodeEnum.IMPORT_FILE_PARSE_ERROR);
        }
        return result;
    }

    /**
     * 校验表头与模板列定义完全一致，不一致时提示用户重新下载模板
     *
     * @param sheet          工作表
     * @param expectHeaders  期望的表头列定义
     * @param dataFormatter 单元格格式化器
     */
    private static void checkHeader(Sheet sheet, String[] expectHeaders, DataFormatter dataFormatter) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new BizException(ResultCodeEnum.IMPORT_FILE_TYPE_ERROR, "导入文件缺少表头，请下载最新模板填写");
        }
        for (int columnIndex = 0; columnIndex < expectHeaders.length; columnIndex++) {
            Cell cell = headerRow.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            String actualHeader = getCellText(cell, dataFormatter);
            if (!expectHeaders[columnIndex].equals(actualHeader)) {
                throw new BizException(ResultCodeEnum.IMPORT_FILE_TYPE_ERROR,
                        StrUtil.format("第 {} 列表头应为「{}」，实际为「{}」，请下载最新模板填写",
                                columnIndex + 1, expectHeaders[columnIndex], actualHeader));
            }
        }
    }

    /**
     * 读取一行中的指定列数，缺失单元格按空串补齐，兼容合并单元格与跳列
     *
     * @param row           数据行，允许为 null
     * @param columnCount   需要读取的列数
     * @param dataFormatter 单元格格式化器
     * @return 单元格文本集合
     */
    private static List<String> listCellText(Row row, int columnCount, DataFormatter dataFormatter) {
        List<String> result = new ArrayList<>(columnCount);
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            Cell cell = row == null ? null : row.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            result.add(getCellText(cell, dataFormatter));
        }
        return result;
    }

    /**
     * 取单元格文本，数值、日期、布尔、公式统一按展示值转字符串。
     * 数值单元格额外处理整数形态：Excel 对超过 11 位的数字默认按科学计数法展示，
     * 直接取展示值会把学号 1234567890123 读成 1.23457E+12，因此整数一律按原值输出
     *
     * @param cell          单元格，允许为 null
     * @param dataFormatter 单元格格式化器
     * @return 单元格文本，不存在时返回空串
     */
    private static String getCellText(Cell cell, DataFormatter dataFormatter) {
        if (cell == null) {
            return StrUtil.EMPTY;
        }
        if (CellType.NUMERIC == cell.getCellType() && !DateUtil.isCellDateFormatted(cell)) {
            double numericValue = cell.getNumericCellValue();
            if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)
                    && Math.abs(numericValue) < MAX_INTEGER_CELL_VALUE) {
                return String.valueOf((long) numericValue);
            }
        }
        return dataFormatter.formatCellValue(cell).trim();
    }

    /**
     * 判断整行是否全为空，用于跳过 Excel 中常见的尾部空行
     *
     * @param cells 单元格文本集合
     * @return 全部为空返回 true
     */
    private static boolean isBlankRow(List<String> cells) {
        return cells.stream().allMatch(StrUtil::isBlank);
    }

    /**
     * Excel 数据行，携带行号便于导入失败时定位
     */
    @Getter
    public static class ExcelRow {

        /**
         * 行号，从数据第一行计为 1，不含表头
         */
        private final int rowIndex;

        /**
         * 单元格文本集合，下标与模板列定义一致
         */
        private final List<String> cells;

        ExcelRow(int rowIndex, List<String> cells) {
            this.rowIndex = rowIndex;
            this.cells = cells;
        }

        /**
         * 按下标取单元格文本
         *
         * @param columnIndex 单元格下标，从 0 开始
         * @return 单元格文本，越界时返回空串
         */
        public String getCell(int columnIndex) {
            return columnIndex >= 0 && columnIndex < cells.size() ? cells.get(columnIndex) : StrUtil.EMPTY;
        }
    }
}
