package com.smart.university.domain.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 定时任务执行日志查询入参（41.2）
 */
@Data
public class ScheduleTaskLogQueryReqDTO extends PageQueryReqDTO {

    /**
     * 执行状态筛选：RUNNING / SUCCESS / FAILED / SKIPPED
     */
    @Schema(example = "SUCCESS")
    private String status;
}
