package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * 学期分页查询入参
 */
@Data
public class SemesterPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学期编号关键字
     */
    @Schema(example = "测试")
    private String keyword;

    /**
     * PLANNED / ACTIVE / FINISHED
     */
    @Schema(example = "ACTIVE")
    private String status;
}
