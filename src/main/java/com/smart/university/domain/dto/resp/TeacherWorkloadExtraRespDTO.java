package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教师工作量口径回显（37.2 extra）
 */
@Data
public class TeacherWorkloadExtraRespDTO {

    /**
     * 学期 ID
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 人均课时，保留 1 位小数
     */
    private BigDecimal avgWorkload;

    /**
     * 快照生成时间，无快照时为当前时间
     */
    private LocalDateTime snapshotTime;
}
