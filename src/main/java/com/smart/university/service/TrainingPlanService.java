package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.req.TrainingPlanPageQueryReqDTO;
import com.smart.university.domain.dto.req.TrainingPlanSaveReqDTO;
import com.smart.university.domain.dto.resp.TrainingPlanRespDTO;

import java.util.List;

/**
 * 培养方案服务
 */
public interface TrainingPlanService {

    /**
     * 分页查询培养方案
     *
     * @param requestParam 查询条件
     * @return 培养方案分页结果
     */
    PageResult<TrainingPlanRespDTO> pageTrainingPlan(TrainingPlanPageQueryReqDTO requestParam);

    /**
     * 查询培养方案详情
     *
     * @param planId 培养方案 ID
     * @return 培养方案信息
     */
    TrainingPlanRespDTO getTrainingPlanDetail(Long planId);

    /**
     * 保存培养方案，入参带 ID 时为修改
     *
     * @param requestParam 培养方案入参
     * @return 培养方案 ID
     */
    Long saveTrainingPlan(TrainingPlanSaveReqDTO requestParam);

    /**
     * 修改培养方案启停状态
     *
     * @param planId       培养方案 ID
     * @param requestParam 状态入参
     */
    void updateTrainingPlanStatus(Long planId, StatusUpdateReqDTO requestParam);

    /**
     * 查询专业与年级对应的启用中培养方案
     *
     * @param majorId 专业 ID
     * @param gradeId 年级 ID
     * @return 培养方案信息，无对应方案时返回 null
     */
    TrainingPlanRespDTO getEnabledTrainingPlan(Long majorId, Long gradeId);

    /**
     * 查询培养方案集合，供批量转换使用
     *
     * @param requestParam 查询条件
     * @return 培养方案集合
     */
    List<TrainingPlanRespDTO> listTrainingPlan(TrainingPlanPageQueryReqDTO requestParam);
}
