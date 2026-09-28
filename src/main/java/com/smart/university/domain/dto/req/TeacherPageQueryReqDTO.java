package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * 教师分页查询入参
 */
@Data
public class TeacherPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 教师编号 / 姓名关键字
     */
    @Schema(example = "测试")
    private String keyword;

    /**
     * 状态：1 正常，0 停用
     */
    @Schema(example = "1")
    private Integer status;
}
