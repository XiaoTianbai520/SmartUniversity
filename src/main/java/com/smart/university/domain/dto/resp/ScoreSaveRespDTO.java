package com.smart.university.domain.dto.resp;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 成绩保存出参
 */
@Data
public class ScoreSaveRespDTO {

    /**
     * 成绩 ID
     */
    private Long scoreId;

    /**
     * 选课记录 ID
     */
    private Long selectionId;

    /**
     * 成绩
     */
    private BigDecimal score;

    /**
     * UNPUBLISHED / PUBLISHED
     */
    private String status;
}
