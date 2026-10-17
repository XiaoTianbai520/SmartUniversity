package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 手动触发任务出参（41.3）
 */
@Data
public class ScheduleTaskRunRespDTO {

    /**
     * 任务编码
     */
    @Schema(example = "WAITLIST_SCAN_TASK")
    private String taskCode;

    /**
     * 本次执行日志 ID
     */
    private Long logId;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 扫描条数
     */
    private Integer scanCount;

    /**
     * 成功条数
     */
    private Integer successCount;

    /**
     * 失败条数
     */
    private Integer failCount;

    /**
     * 耗时（毫秒）
     */
    private Long durationMs;
}
