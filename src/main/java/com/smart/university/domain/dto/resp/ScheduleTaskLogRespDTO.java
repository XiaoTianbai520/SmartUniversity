package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务执行日志出参（41.2）
 */
@Data
public class ScheduleTaskLogRespDTO {

    /**
     * 日志 ID
     */
    @Schema(example = "1024")
    private Long logId;

    /**
     * 任务编码
     */
    private String taskCode;

    /**
     * 任务中文名
     */
    private String taskName;

    /**
     * 触发方式：SCHEDULED / MANUAL
     */
    private String triggerType;

    /**
     * 执行状态：RUNNING / SUCCESS / FAILED / SKIPPED
     */
    private String status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 耗时（毫秒）
     */
    private Long durationMs;

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
     * 异常摘要
     */
    private String errorSummary;

    /**
     * 执行实例标识
     */
    private String instanceId;
}
