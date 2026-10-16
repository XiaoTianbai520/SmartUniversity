package com.smart.university.domain.dto.resp;

import lombok.Data;

import java.util.List;

/**
 * 选课热度预测出参（40.2）
 */
@Data
public class EnrollmentForecastRespDTO {

    /**
     * 学期 ID
     */
    private Long semesterId;

    /**
     * 预测明细集合，按预测需求降序
     */
    private List<EnrollmentForecastItemRespDTO> records;
}
