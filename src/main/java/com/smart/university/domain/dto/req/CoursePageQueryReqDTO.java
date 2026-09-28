package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * 课程分页查询入参
 */
@Data
public class CoursePageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 课程编号 / 名称关键字
     */
    @Schema(example = "测试")
    private String keyword;

    /**
     * 课程性质
     */
    @Schema(example = "REQUIRED")
    private String courseType;

    /**
     * 状态：1 启用，0 禁用
     */
    @Schema(example = "1")
    private Integer status;
}
