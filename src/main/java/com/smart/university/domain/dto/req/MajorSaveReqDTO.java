package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 专业新增 / 修改入参
 */
@Data
public class MajorSaveReqDTO {

    /**
     * 专业 ID，修改时必传
     */
    @Schema(example = "1")
    private Long id;

    /**
     * 专业编号
     */
    @Schema(example = "CS001")
    @NotBlank(message = "专业编号不能为空")
    private String majorCode;

    /**
     * 专业名称
     */
    @Schema(example = "计算机科学与技术")
    @NotBlank(message = "专业名称不能为空")
    private String majorName;
}
