package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * 教室新增 / 修改入参
 */
@Data
public class ClassroomSaveReqDTO {

    /**
     * 教室 ID，修改时必传
     */
    @Schema(example = "1")
    private Long id;

    /**
     * 教学楼名称
     */
    @Schema(example = "教学楼A")
    @NotBlank(message = "教学楼名称不能为空")
    private String buildingName;

    /**
     * 教室编号
     */
    @Schema(example = "A101")
    @NotBlank(message = "教室编号不能为空")
    private String roomNo;

    /**
     * 教室容量
     */
    @Schema(example = "50")
    @NotNull(message = "教室容量不能为空")
    private Integer capacity;
}
