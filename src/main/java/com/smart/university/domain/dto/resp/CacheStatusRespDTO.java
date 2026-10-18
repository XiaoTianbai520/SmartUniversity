package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 容量缓存状态出参（34.2）
 */
@Data
public class CacheStatusRespDTO {

    /**
     * 学期 ID
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 是否处于 Redis 降级状态
     */
    private Boolean degraded;

    /**
     * 最近一次降级发生时间，未降级为 null
     */
    private LocalDateTime degradedAt;

    /**
     * 降级原因摘要
     */
    private String degradeReason;

    /**
     * 本学期教学班数
     */
    private Integer teachingClassCount;

    /**
     * 当前已缓存的容量 Key 数
     */
    private Integer cachedKeyCount;

    /**
     * 最近一次对账时间，由 W2 对账任务回填
     */
    private LocalDateTime lastReconcileTime;

    /**
     * 抽样教学班的 DB 权威已选人数，用于人工核对
     */
    private Integer selectedCountSample;
}
