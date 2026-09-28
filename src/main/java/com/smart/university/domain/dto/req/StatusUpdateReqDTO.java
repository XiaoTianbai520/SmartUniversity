package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 通用启停状态修改入参
 */
@Data
public class StatusUpdateReqDTO {

    /**
     * 状态：1 启用，0 禁用
     */
    @Schema(example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
