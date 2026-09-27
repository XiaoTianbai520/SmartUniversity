package com.smart.university.domain.dto.req;

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
    private Long id;

    /**
     * 教学楼名称
     */
    @NotBlank(message = "教学楼名称不能为空")
    private String buildingName;

    /**
     * 教室编号
     */
    @NotBlank(message = "教室编号不能为空")
    private String roomNo;

    /**
     * 教室容量
     */
    @NotNull(message = "教室容量不能为空")
    private Integer capacity;
}
