package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统计快照刷新出参（37.4）
 */
@Data
public class SnapshotRefreshRespDTO {

    /**
     * 学期 ID
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 本次刷新的统计类型
     */
    private List<String> statTypes;

    /**
     * 成功类型数
     */
    private Integer successCount;

    /**
     * 失败类型数
     */
    private Integer failCount;

    /**
     * 耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 生成时间
     */
    private LocalDateTime generatedAt;
}
