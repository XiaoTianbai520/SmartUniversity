package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 导入失败明细，逐行导入时记录行号、业务标识与失败原因
 */
@Data
public class ImportFailItemDTO {

    /**
     * 行号，从数据第一行计为 1，不含表头
     */
    private Integer rowIndex;

    /**
     * 业务标识，学号 / 教师编号
     */
    private String identifier;

    /**
     * 失败原因，面向教务与教师的中文描述
     */
    private String reason;
}
