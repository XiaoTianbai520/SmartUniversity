package com.smart.university.service;

import com.smart.university.domain.dto.req.EnrollmentForecastQueryReqDTO;
import com.smart.university.domain.dto.resp.EnrollmentForecastRespDTO;

/**
 * 选课热度预测与扩容建议服务（I-02）
 */
public interface EnrollmentForecastService {

    /**
     * 查询选课热度预测与扩容建议
     *
     * @param requestParam 查询条件
     * @return 预测结果集合
     */
    EnrollmentForecastRespDTO listEnrollmentForecast(EnrollmentForecastQueryReqDTO requestParam);
}
