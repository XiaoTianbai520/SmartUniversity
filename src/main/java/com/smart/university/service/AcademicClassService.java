package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.dto.req.AcademicClassPageQueryReqDTO;
import com.smart.university.domain.dto.req.AcademicClassSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.AcademicClassRespDTO;

/**
 * 行政班服务
 */
public interface AcademicClassService {

    /**
     * 分页查询行政班
     *
     * @param requestParam 查询条件
     * @return 行政班分页结果
     */
    PageResult<AcademicClassRespDTO> pageAcademicClass(AcademicClassPageQueryReqDTO requestParam);

    /**
     * 查询行政班详情
     *
     * @param classId 班级 ID
     * @return 行政班信息
     */
    AcademicClassRespDTO getAcademicClassDetail(Long classId);

    /**
     * 保存行政班
     *
     * @param requestParam 行政班入参
     * @return 班级 ID
     */
    Long saveAcademicClass(AcademicClassSaveReqDTO requestParam);

    /**
     * 修改行政班状态
     *
     * @param classId      班级 ID
     * @param requestParam 状态入参
     */
    void updateAcademicClassStatus(Long classId, StatusUpdateReqDTO requestParam);
}
