package com.smart.university.mapper;

import com.smart.university.domain.entity.SelectionBatchClassDO;

import java.util.List;

/**
 * 选课批次开放教学班持久层
 */
public interface SelectionBatchClassMapper {

    /**
     * 保存批次与教学班绑定关系
     *
     * @param requestParam 绑定关系数据对象
     * @return 影响行数
     */
    int saveSelectionBatchClass(SelectionBatchClassDO requestParam);

    /**
     * 解除批次与教学班绑定关系
     *
     * @param requestParam 绑定关系数据对象，包含 batchId 与 teachingClassId
     * @return 影响行数
     */
    int removeSelectionBatchClass(SelectionBatchClassDO requestParam);

    /**
     * 统计批次与教学班绑定数量
     *
     * @param requestParam 绑定关系数据对象，包含 batchId 与 teachingClassId
     * @return 数量
     */
    long countSelectionBatchClass(SelectionBatchClassDO requestParam);

    /**
     * 查询批次已开放的教学班 ID 集合
     *
     * @param batchId 批次 ID
     * @return 教学班 ID 集合
     */
    List<Long> listTeachingClassIdByBatchId(Long batchId);
}
