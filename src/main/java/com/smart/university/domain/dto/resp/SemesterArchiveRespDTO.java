package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学期数据归档结果出参（42.3），对齐接口清单 §42.3 响应结构
 */
@Data
public class SemesterArchiveRespDTO {

    /**
     * 学期 ID
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 实际归档的归档类型列表
     */
    private List<String> archiveTypes;

    /**
     * 成功归档的类型数
     */
    private int successCount;

    /**
     * 失败归档的类型数
     */
    private int failCount;

    /**
     * 归档完成时间
     */
    private LocalDateTime archivedAt;
}
