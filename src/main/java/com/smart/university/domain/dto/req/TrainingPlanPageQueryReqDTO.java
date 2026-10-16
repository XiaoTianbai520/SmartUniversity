package com.smart.university.domain.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 培养方案分页查询入参
 */
@Data
public class TrainingPlanPageQueryReqDTO extends PageQueryReqDTO {

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
     * 状态：1 启用，0 停用
     */
    @Schema(example = "1")
    private Integer status;

    /**
     * 培养方案名称关键字
     */
    @Schema(example = "软件工程")
    private String keyword;
}
