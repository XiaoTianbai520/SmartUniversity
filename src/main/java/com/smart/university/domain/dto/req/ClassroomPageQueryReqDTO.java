package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * 教室分页查询入参
 */
@Data
public class ClassroomPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 教学楼 / 教室编号关键字
     */
    @Schema(example = "测试")
    private String keyword;

    /**
     * 状态：1 可用，0 停用
     */
    @Schema(example = "1")
    private Integer status;
}
