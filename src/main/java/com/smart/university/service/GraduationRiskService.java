package com.smart.university.service;

import com.smart.university.common.enums.RiskLevelEnum;
import com.smart.university.domain.dto.req.GraduationRiskQueryReqDTO;
import com.smart.university.domain.dto.resp.GraduationRiskPageRespDTO;
import com.smart.university.domain.dto.resp.GraduationRiskRespDTO;

/**
 * 毕业风险预测服务（I-01）
 */
public interface GraduationRiskService {

    /**
     * 查询当前登录学生的毕业风险，学生身份从登录态取
     *
     * @param semesterId 统计截止学期 ID，为空取当前学期
     * @return 毕业风险预测结果
     */
    GraduationRiskRespDTO getMyGraduationRisk(Long semesterId);

    /**
     * 查询指定学生的毕业风险
     *
     * @param studentId  学生 ID
     * @param semesterId 统计截止学期 ID，为空取当前学期
     * @return 毕业风险预测结果
     */
    GraduationRiskRespDTO getGraduationRiskByStudentId(Long studentId, Long semesterId);

    /**
     * 分页查询毕业风险名单
     *
     * @param requestParam 查询条件
     * @return 毕业风险名单分页结果与口径回显
     */
    GraduationRiskPageRespDTO pageGraduationRisk(GraduationRiskQueryReqDTO requestParam);

    /**
     * 判定风险等级，与 getGraduationRiskByStudentId 同源
     *
     * @param studentId  学生 ID
     * @param semesterId 统计截止学期 ID
     * @return 风险等级
     */
    RiskLevelEnum getRiskLevel(Long studentId, Long semesterId);
}
