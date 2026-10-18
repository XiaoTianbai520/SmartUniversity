package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 容量缓存预热结果出参（34.1）
 */
@Data
public class CacheWarmupRespDTO {

    /**
     * 选课批次 ID
     */
    @Schema(example = "1")
    private Long batchId;

    /**
     * 批次内教学班总数
     */
    private Integer teachingClassCount;

    /**
     * 成功写入 Redis 的 Key 数
     */
    private Integer warmupCount;

    /**
     * 失败条数，单条失败不中断整轮
     */
    private Integer failCount;

    /**
     * 预热前已存在且与 DB 不一致、被覆盖的 Key 数
     */
    private Integer mismatchCount;

    /**
     * 耗时（毫秒）
     */
    private Long durationMs;
}
