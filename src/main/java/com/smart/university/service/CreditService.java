package com.smart.university.service;

import com.smart.university.domain.dto.resp.CreditSummaryRespDTO;

/**
 * 学分统计服务
 */
public interface CreditService {

    /**
     * 查询我的学分统计
     *
     * @param semesterId 学期 ID，为空时使用当前学期
     * @return 学分统计
     */
    CreditSummaryRespDTO getCreditSummary(Long semesterId);
}
