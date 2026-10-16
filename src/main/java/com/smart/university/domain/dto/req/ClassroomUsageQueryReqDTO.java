package com.smart.university.domain.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 教室利用率统计查询入参（37.1）
 */
@Data
public class ClassroomUsageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学期 ID，缺省当前学期
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 教学楼名称，精确筛选
     */
    @Schema(example = "致知楼")
    private String buildingName;

    /**
     * 统计维度：CLASSROOM（默认）/ BUILDING / WEEKDAY_SECTION / FREE_SLOT
     */
    @Schema(example = "CLASSROOM")
    private String dimension;
}
