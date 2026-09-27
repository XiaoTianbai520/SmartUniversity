package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 专业出参
 */
@Data
public class MajorRespDTO {

    /**
     * 专业 ID
     */
    private Long majorId;

    /**
     * 专业编号
     */
    private String majorCode;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 状态：1 启用，0 禁用
     */
    private Integer status;
}
