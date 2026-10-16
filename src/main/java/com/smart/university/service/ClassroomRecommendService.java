package com.smart.university.service;

import com.smart.university.domain.dto.req.ClassroomRecommendQueryReqDTO;
import com.smart.university.domain.dto.resp.ClassroomRecommendRespDTO;

/**
 * 排课冲突检测与教室推荐服务（I-04）
 */
public interface ClassroomRecommendService {

    /**
     * 推荐可用教室，只做推荐不做自动排课求解
     *
     * @param requestParam 查询条件
     * @return 教室推荐结果
     */
    ClassroomRecommendRespDTO listClassroomRecommend(ClassroomRecommendQueryReqDTO requestParam);
}
