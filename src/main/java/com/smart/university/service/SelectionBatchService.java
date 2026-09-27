package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.entity.SelectionBatchDO;
import com.smart.university.domain.dto.req.BatchTeachingClassSaveReqDTO;
import com.smart.university.domain.dto.req.SelectionBatchPageQueryReqDTO;
import com.smart.university.domain.dto.req.SelectionBatchSaveReqDTO;
import com.smart.university.domain.dto.req.SelectionBatchStatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.SelectionBatchRespDTO;
import com.smart.university.domain.dto.resp.TeachingClassRespDTO;

import java.util.List;

/**
 * 选课批次服务
 */
public interface SelectionBatchService {

    /**
     * 分页查询选课批次
     *
     * @param requestParam 查询条件
     * @return 选课批次分页结果
     */
    PageResult<SelectionBatchRespDTO> pageSelectionBatch(SelectionBatchPageQueryReqDTO requestParam);

    /**
     * 查询选课批次详情
     *
     * @param batchId 批次 ID
     * @return 选课批次信息
     */
    SelectionBatchRespDTO getSelectionBatchDetail(Long batchId);

    /**
     * 保存选课批次
     *
     * @param requestParam 批次入参
     * @return 批次 ID
     */
    Long saveSelectionBatch(SelectionBatchSaveReqDTO requestParam);

    /**
     * 修改选课批次状态
     *
     * @param batchId      批次 ID
     * @param requestParam 状态入参
     */
    void updateSelectionBatchStatus(Long batchId, SelectionBatchStatusUpdateReqDTO requestParam);

    /**
     * 向批次添加开放教学班
     *
     * @param batchId      批次 ID
     * @param requestParam 教学班 ID 集合入参
     */
    void saveBatchTeachingClass(Long batchId, BatchTeachingClassSaveReqDTO requestParam);

    /**
     * 查询批次已开放的教学班
     *
     * @param batchId 批次 ID
     * @return 教学班信息集合
     */
    List<TeachingClassRespDTO> listBatchTeachingClass(Long batchId);

    /**
     * 从批次移除教学班，已存在有效选课记录时禁止移除
     *
     * @param batchId         批次 ID
     * @param teachingClassId 教学班 ID
     */
    void removeBatchTeachingClass(Long batchId, Long teachingClassId);

    /**
     * 查询指定学期当前处于选课时间内的批次
     *
     * @param semesterId 学期 ID
     * @return 选课批次数据对象
     */
    SelectionBatchDO getCurrentSelectionBatch(Long semesterId);

    /**
     * 根据 ID 查询选课批次
     *
     * @param batchId 批次 ID
     * @return 选课批次数据对象
     */
    SelectionBatchDO getSelectionBatchById(Long batchId);
}
