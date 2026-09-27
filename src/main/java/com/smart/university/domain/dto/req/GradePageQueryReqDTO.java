package com.smart.university.domain.dto.req;

import lombok.Data;

/**
 * 年级分页查询入参
 */
@Data
public class GradePageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 年级名称关键字
     */
    private String keyword;

    /**
     * 状态：1 启用，0 禁用
     */
    private Integer status;
}
