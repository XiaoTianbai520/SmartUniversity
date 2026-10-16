package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 教室推荐明细出参（40.4 records）
 */
@Data
public class ClassroomRecommendItemRespDTO {

    /**
     * 教室 ID
     */
    @Schema(example = "7")
    private Long classroomId;

    /**
     * 教学楼名称
     */
    private String buildingName;

    /**
     * 教室编号
     */
    private String roomNo;

    /**
     * 教室容量
     */
    private Integer capacity;

    /**
     * 容量冗余 = 教室容量 − 教学班需求容量
     */
    private Integer capacityMargin;

    /**
     * 推荐理由
     */
    private String reason;
}
