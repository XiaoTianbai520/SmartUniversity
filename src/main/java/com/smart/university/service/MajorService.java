package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.dto.req.MajorPageQueryReqDTO;
import com.smart.university.domain.dto.req.MajorSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.MajorRespDTO;

import java.util.List;

/**
 * 专业服务
 */
public interface MajorService {

    /**
     * 分页查询专业
     *
     * @param requestParam 查询条件
     * @return 专业分页结果
     */
    PageResult<MajorRespDTO> pageMajor(MajorPageQueryReqDTO requestParam);

    /**
     * 查询专业详情
     *
     * @param majorId 专业 ID
     * @return 专业信息
     */
    MajorRespDTO getMajorDetail(Long majorId);

    /**
     * 查询全部启用专业
     *
     * @return 专业信息集合
     */
    List<MajorRespDTO> listEnabledMajor();

    /**
     * 保存专业
     *
     * @param requestParam 专业入参
     * @return 专业 ID
     */
    Long saveMajor(MajorSaveReqDTO requestParam);

    /**
     * 修改专业状态
     *
     * @param majorId      专业 ID
     * @param requestParam 状态入参
     */
    void updateMajorStatus(Long majorId, StatusUpdateReqDTO requestParam);
}
