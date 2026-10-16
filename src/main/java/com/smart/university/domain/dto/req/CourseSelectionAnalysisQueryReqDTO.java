package com.smart.university.domain.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 课程选课分析查询入参（37.3）
 */
@Data
public class CourseSelectionAnalysisQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学期 ID，缺省当前学期
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 课程 ID
     */
    @Schema(example = "1")
    private Long courseId;

    /**
     * 专业 ID，用于专业分布筛选
     */
    @Schema(example = "1")
    private Long majorId;

    /**
     * 年级 ID，用于年级分布筛选
     */
    @Schema(example = "1")
    private Long gradeId;

    /**
     * 只看满班教学班
     */
    @Schema(example = "false")
    private Boolean onlyFull;

    /**
     * 排序方式：SELECTED（默认，已选人数降序）/ FULL_RATE / WITHDRAW
     */
    @Schema(example = "SELECTED")
    private String sortBy;
}
