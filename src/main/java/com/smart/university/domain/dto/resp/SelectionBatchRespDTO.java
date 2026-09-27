package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 选课批次出参
 */
@Data
public class SelectionBatchRespDTO {

    /**
     * 批次 ID
     */
    private Long batchId;

    /**
     * 批次名称
     */
    private String batchName;

    /**
     * 学期 ID
     */
    private Long semesterId;

    /**
     * 选课开始时间
     */
    private LocalDateTime startTime;

    /**
     * 选课结束时间
     */
    private LocalDateTime endTime;

    /**
     * 退课截止时间
     */
    private LocalDateTime dropDeadline;

    /**
     * NOT_STARTED / IN_PROGRESS / ENDED
     */
    private String status;
}
