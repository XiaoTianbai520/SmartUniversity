package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * 年级分页查询入参
 */
@Data
public class GradePageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 年级名称关键字
     */
    @Schema(example = "测试")
    private String keyword;

    /**
     * 状态：1 启用，0 禁用
     */
    @Schema(example = "1")
    private Integer status;
}
