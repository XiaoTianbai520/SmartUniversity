package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 教学班状态修改入参
 */
@Data
public class TeachingClassStatusUpdateReqDTO {

    /**
     * DRAFT / AVAILABLE / CLOSED / CANCELLED
     */
    @Schema(example = "AVAILABLE")
    @NotBlank(message = "状态不能为空")
    private String status;
}
