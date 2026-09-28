package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * 教学班分页查询入参
 */
@Data
public class TeachingClassPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学期 ID
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 课程 ID
     */
    @Schema(example = "1")
    private Long courseId;

    /**
     * 教师 ID
     */
    @Schema(example = "T10001")
    private Long teacherId;

    /**
     * DRAFT / AVAILABLE / CLOSED / CANCELLED
     */
    @Schema(example = "AVAILABLE")
    private String status;

    /**
     * 教学班编号 / 名称关键字
     */
    @Schema(example = "测试")
    private String keyword;
}
