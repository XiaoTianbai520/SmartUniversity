package com.smart.university.service;

import com.smart.university.common.enums.RiskLevelEnum;
import com.smart.university.domain.dto.resp.CreditProgressRespDTO;

/**
 * 学生学分进度服务（S-02），跨学期累计口径，供 36.1 与 I-01 毕业风险复用同一份结果
 */
public interface CreditProgressService {

    /**
     * 查询当前登录学生的学分进度
     *
     * @param semesterId 统计截止学期 ID，为空取当前学期
     * @return 学分进度
     */
    CreditProgressRespDTO getCreditProgress(Long semesterId);

    /**
     * 查询指定学生的学分进度，供快照生成与毕业风险预测复用
     *
     * @param studentId  学生 ID
     * @param semesterId 统计截止学期 ID，为空取当前学期
     * @return 学分进度
     */
    CreditProgressRespDTO getCreditProgressByStudentId(Long studentId, Long semesterId);

    /**
     * 判定学生的毕业风险等级
     *
     * @param studentId      学生 ID
     * @param semesterId     统计截止学期 ID
     * @param creditProgress 学分进度，为空时内部重新计算
     * @return 风险等级
     */
    RiskLevelEnum getRiskLevel(Long studentId, Long semesterId, CreditProgressRespDTO creditProgress);
}
