package com.smart.university.service;

import com.smart.university.domain.dto.resp.ScoreStatisticsRespDTO;

/**
 * 成绩统计服务
 */
public interface ScoreStatisticsService {

    /**
     * 查询教学班成绩统计
     *
     * @param teachingClassId 教学班 ID
     * @return 教学班成绩统计
     */
    ScoreStatisticsRespDTO getScoreStatistics(Long teachingClassId);
}
