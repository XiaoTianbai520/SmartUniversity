package com.smart.university.domain.dto.req;

import lombok.Data;

/**
 * 选课批次分页查询入参
 */
@Data
public class SelectionBatchPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学期 ID
     */
    private Long semesterId;

    /**
     * NOT_STARTED / IN_PROGRESS / ENDED
     */
    private String status;
}
