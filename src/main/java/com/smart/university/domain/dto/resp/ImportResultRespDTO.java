package com.smart.university.domain.dto.resp;

import lombok.Data;

import java.util.List;

/**
 * 批量导入结果，逐行导入时无论单行成败都返回整体汇总
 */
@Data
public class ImportResultRespDTO {

    /**
     * 数据总行数
     */
    private Integer totalCount;

    /**
     * 成功行数
     */
    private Integer successCount;

    /**
     * 失败行数
     */
    private Integer failCount;

    /**
     * 失败明细
     */
    private List<ImportFailItemDTO> failList;
}
