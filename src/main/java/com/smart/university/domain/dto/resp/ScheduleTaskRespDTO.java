package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务列表出参（41.1）
 */
@Data
public class ScheduleTaskRespDTO {

    /**
     * 任务编码
     */
    @Schema(example = "BATCH_STATUS_TASK")
    private String taskCode;

    /**
     * 任务中文名
     */
    private String taskName;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 调度表达式描述，cron 型为 cron 表达式，fixedDelay 型为 fixedDelay=毫秒
     */
    private String scheduleExpr;

    /**
     * 最近一次执行状态
     */
    private String lastStatus;

    /**
     * 最近一次开始时间
     */
    private LocalDateTime lastStartTime;

    /**
     * 最近一次结束时间
     */
    private LocalDateTime lastEndTime;

    /**
     * 最近一次耗时（毫秒）
     */
    private Long lastDurationMs;

    /**
     * 最近一次扫描条数
     */
    private Integer lastScanCount;

    /**
     * 最近一次成功条数
     */
    private Integer lastSuccessCount;

    /**
     * 最近一次失败条数
     */
    private Integer lastFailCount;

    /**
     * 最近一次执行实例标识
     */
    private String lastInstanceId;
}
