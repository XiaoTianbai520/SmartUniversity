package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * 学生分页查询入参
 */
@Data
public class StudentPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学号 / 姓名关键字
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
     * 行政班级 ID
     */
    @Schema(example = "1")
    private Long classId;

    /**
     * 状态：1 正常，0 停用
     */
    @Schema(example = "1")
    private Integer status;
}
