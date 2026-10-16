package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.dto.req.ScoreAnomalyQueryReqDTO;
import com.smart.university.domain.dto.resp.ScoreAnomalyPageRespDTO;
import com.smart.university.domain.dto.resp.ScoreAnomalyRespDTO;

import java.util.List;

/**
 * 成绩分布异常预警服务（I-03）
 */
public interface ScoreAnomalyService {

    /**
     * 查询教师本人任教教学班的成绩异常预警，教师身份从登录态取
     *
     * @param requestParam 查询条件
     * @return 成绩异常预警分页结果
     */
    PageResult<ScoreAnomalyRespDTO> pageMyScoreAnomaly(ScoreAnomalyQueryReqDTO requestParam);

    /**
     * 查询全部教学班的成绩异常预警，教务端使用
     *
     * @param requestParam 查询条件
     * @return 成绩异常预警分页结果与口径回显
     */
    ScoreAnomalyPageRespDTO pageScoreAnomaly(ScoreAnomalyQueryReqDTO requestParam);

    /**
     * 查询指定教学班的成绩异常预警
     *
     * @param semesterId      学期 ID
     * @param teachingClassId 教学班 ID
     * @return 成绩异常预警，无异常时返回 null
     */
    ScoreAnomalyRespDTO getScoreAnomaly(Long semesterId, Long teachingClassId);

    /**
     * 查询本学期全部教学班的成绩异常明细，不做分页与筛选
     *
     * @param semesterId 学期 ID
     * @return 成绩异常明细集合
     */
    List<ScoreAnomalyRespDTO> listScoreAnomaly(Long semesterId);
}
