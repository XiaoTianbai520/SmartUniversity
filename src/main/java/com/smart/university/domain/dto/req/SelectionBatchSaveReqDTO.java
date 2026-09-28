package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * 选课批次新增 / 修改入参
 */
@Data
public class SelectionBatchSaveReqDTO {

    /**
     * 批次 ID，修改时必传
     */
    @Schema(example = "1")
    private Long id;

    /**
     * 批次名称
     */
    @Schema(example = "2026秋季选课批次")
    @NotBlank(message = "批次名称不能为空")
    private String batchName;

    /**
     * 所属学期 ID
     */
    @Schema(example = "1")
    @NotNull(message = "学期 ID 不能为空")
    private Long semesterId;

    /**
     * 选课开始时间
     */
    @Schema(example = "2026-09-01 08:00:00")
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 选课结束时间
     */
    @Schema(example = "2026-09-01 10:00:00")
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 退课截止时间
     */
    @Schema(example = "2026-09-30 23:59:59")
    private LocalDateTime dropDeadline;
}
