package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.university.common.enums.TaskExecuteStatusEnum;
import com.smart.university.common.enums.TriggerTypeEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * schedule_task_log 表，定时任务执行日志
 */
@Data
@TableName("schedule_task_log")
public class ScheduleTaskLogDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务编码，见 TaskCodeEnum
     */
    private String taskCode;

    /**
     * 任务中文名
     */
    private String taskName;

    /**
     * 触发方式：SCHEDULED/MANUAL
     */
    private TriggerTypeEnum triggerType;

    /**
     * 执行状态：RUNNING/SUCCESS/FAILED/SKIPPED
     */
    private TaskExecuteStatusEnum status;

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
     * 执行实例标识（主机名:端口）
     */
    private String instanceId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
