package com.smart.university.domain.dto.req;

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
    private Long id;

    /**
     * 专业编号
     */
    @NotBlank(message = "专业编号不能为空")
    private String majorCode;

    /**
     * 专业名称
     */
    @NotBlank(message = "专业名称不能为空")
    private String majorName;
}
