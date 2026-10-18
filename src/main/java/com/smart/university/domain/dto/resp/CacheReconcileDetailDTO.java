package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 缓存一致性对账明细项（34.3 details[]）
 */
@Data
public class CacheReconcileDetailDTO {

    /**
     * 教学班 ID
     */
    @Schema(example = "3")
    private Long teachingClassId;

    /**
     * DB 权威已选人数
     */
    @Schema(example = "40")
    private Long dbCount;

    /**
     * Redis 计数 Key 当前值，Key 缺失或解析失败时为空
     */
    @Schema(example = "38")
    private Long redisCount;

    /**
     * 是否已以 DB 为准完成回写
     */
    @Schema(example = "true")
    private Boolean fixed;
}
