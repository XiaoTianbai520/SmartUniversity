package com.smart.university.domain.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 选课热度预测查询入参（40.2）
 */
@Data
public class EnrollmentForecastQueryReqDTO {

    /**
     * 学期 ID，缺省当前学期
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 课程 ID，传入表示显式要求该课程的预测，样本不足时返回 40944
     */
    @Schema(example = "1")
    private Long courseId;

    /**
     * 返回条数，默认 20
     */
    @Schema(example = "20")
    private Integer topN;
}
