package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * 行政班分页查询入参
 */
@Data
public class AcademicClassPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 班级编号 / 名称关键字
     */
    @Schema(example = "测试")
    private String keyword;

    /**
     * 专业 ID
     */
    @Schema(example = "1")
    private Long majorId;

    /**
     * 年级 ID
     */
    @Schema(example = "1")
    private Long gradeId;

    /**
     * 状态：1 启用，0 禁用
     */
    @Schema(example = "1")
    private Integer status;
}
