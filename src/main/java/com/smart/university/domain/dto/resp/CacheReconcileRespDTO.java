package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 缓存一致性对账结果出参（34.3）
 *
 * <p>三方对账：DB COUNT(course_selection WHERE status='SELECTED') / Redis 计数 Key /
 * teaching_class.selected_count；不一致时以 DB 为准回写，对账本身不产生超卖。
 */
@Data
public class CacheReconcileRespDTO {

    /**
     * 对账学期 ID
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 扫描教学班数
     */
    @Schema(example = "12")
    private Integer scanCount;

    /**
     * 不一致条数（Redis 或 selected_count 任一与 DB 不符）
     */
    @Schema(example = "2")
    private Integer mismatchCount;

    /**
     * 已修复条数（以 DB 为准回写 Redis 与 selected_count）
     */
    @Schema(example = "2")
    private Integer fixedCount;

    /**
     * 失败条数，单条失败不中断整轮
     */
    @Schema(example = "0")
    private Integer failCount;

    /**
     * 耗时（毫秒）
     */
    @Schema(example = "124")
    private Long durationMs;

    /**
     * 不一致明细
     */
    private List<CacheReconcileDetailDTO> details;
}
