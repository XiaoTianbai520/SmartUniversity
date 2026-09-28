package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * 选课批次分页查询入参
 */
@Data
public class SelectionBatchPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学期 ID
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * NOT_STARTED / IN_PROGRESS / ENDED
     */
    @Schema(example = "IN_PROGRESS")
    private String status;
}
