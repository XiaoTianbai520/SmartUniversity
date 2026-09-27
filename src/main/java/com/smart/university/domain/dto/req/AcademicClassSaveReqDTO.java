package com.smart.university.domain.dto.req;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * 行政班新增 / 修改入参
 */
@Data
public class AcademicClassSaveReqDTO {

    /**
     * 班级 ID，修改时必传
     */
    private Long id;

    /**
     * 班级编号
     */
    @NotBlank(message = "班级编号不能为空")
    private String classCode;

    /**
     * 班级名称
     */
    @NotBlank(message = "班级名称不能为空")
    private String className;

    /**
     * 所属专业 ID
     */
    @NotNull(message = "专业 ID 不能为空")
    private Long majorId;

    /**
     * 所属年级 ID
     */
    @NotNull(message = "年级 ID 不能为空")
    private Long gradeId;
}
