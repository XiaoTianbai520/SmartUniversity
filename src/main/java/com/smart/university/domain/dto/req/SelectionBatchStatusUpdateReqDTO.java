package com.smart.university.domain.dto.req;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 选课批次状态修改入参
 */
@Data
public class SelectionBatchStatusUpdateReqDTO {

    /**
     * NOT_STARTED / IN_PROGRESS / ENDED
     */
    @NotBlank(message = "状态不能为空")
    private String status;
}
