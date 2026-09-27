package com.smart.university.domain.dto.req;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * 年级新增 / 修改入参
 */
@Data
public class GradeSaveReqDTO {

    /**
     * 年级 ID，修改时必传
     */
    private Long id;

    /**
     * 年级名称，例如 2026级
     */
    @NotBlank(message = "年级名称不能为空")
    private String gradeName;

    /**
     * 入学年份
     */
    @NotNull(message = "入学年份不能为空")
    private Integer entryYear;
}
