package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 成绩发布出参
 */
@Data
public class ScorePublishRespDTO {

    /**
     * 本次发布成绩条数
     */
    private Integer publishedCount;
}
