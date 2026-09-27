package com.smart.university.mapper;

import com.smart.university.domain.entity.SelectionBatchDO;
import com.smart.university.domain.dto.req.SelectionBatchPageQueryReqDTO;

import java.util.List;

/**
 * 选课批次持久层
 */
public interface SelectionBatchMapper {

    /**
     * 根据 ID 查询选课批次
     *
     * @param batchId 批次 ID
     * @return 选课批次信息
     */
    SelectionBatchDO getSelectionBatchById(Long batchId);

    /**
     * 查询指定学期下当前处于选课时间内的批次
     *
     * @param semesterId 学期 ID
     * @return 选课批次信息
     */
    SelectionBatchDO getCurrentSelectionBatch(Long semesterId);

    /**
     * 按条件统计选课批次数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    long countSelectionBatchByCondition(SelectionBatchPageQueryReqDTO requestParam);

    /**
     * 按条件分页查询选课批次
     *
     * @param requestParam 查询条件
     * @return 选课批次信息集合
     */
    List<SelectionBatchDO> listSelectionBatchByCondition(SelectionBatchPageQueryReqDTO requestParam);

    /**
     * 保存选课批次
     *
     * @param requestParam 选课批次数据对象
     * @return 影响行数
     */
    int saveSelectionBatch(SelectionBatchDO requestParam);

    /**
     * 更新选课批次
     *
     * @param requestParam 选课批次数据对象
     * @return 影响行数
     */
    int updateSelectionBatch(SelectionBatchDO requestParam);
}
