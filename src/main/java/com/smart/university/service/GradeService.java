package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.dto.req.GradePageQueryReqDTO;
import com.smart.university.domain.dto.req.GradeSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.GradeRespDTO;

import java.util.List;

/**
 * 年级服务
 */
public interface GradeService {

    /**
     * 分页查询年级
     *
     * @param requestParam 查询条件
     * @return 年级分页结果
     */
    PageResult<GradeRespDTO> pageGrade(GradePageQueryReqDTO requestParam);

    /**
     * 查询年级详情
     *
     * @param gradeId 年级 ID
     * @return 年级信息
     */
    GradeRespDTO getGradeDetail(Long gradeId);

    /**
     * 查询全部启用年级
     *
     * @return 年级信息集合
     */
    List<GradeRespDTO> listEnabledGrade();

    /**
     * 保存年级
     *
     * @param requestParam 年级入参
     * @return 年级 ID
     */
    Long saveGrade(GradeSaveReqDTO requestParam);

    /**
     * 修改年级状态
     *
     * @param gradeId      年级 ID
     * @param requestParam 状态入参
     */
    void updateGradeStatus(Long gradeId, StatusUpdateReqDTO requestParam);
}
