package com.smart.university.service;

import com.smart.university.domain.dto.resp.DashboardOverviewRespDTO;

/**
 * 教务数据看板服务（S-06）
 */
public interface DashboardService {

    /**
     * 查询教务首页数据看板，优先命中快照
     *
     * @param semesterId 学期 ID，为空取当前学期
     * @return 看板聚合结果
     */
    DashboardOverviewRespDTO getDashboardOverview(Long semesterId);
}
