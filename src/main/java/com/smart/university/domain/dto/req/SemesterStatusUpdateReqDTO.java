package com.smart.university.domain.dto.req;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 学期状态修改入参
 */
@Data
public class SemesterStatusUpdateReqDTO {

    /**
     * PLANNED / ACTIVE / FINISHED
     */
    @NotBlank(message = "状态不能为空")
    private String status;
}
