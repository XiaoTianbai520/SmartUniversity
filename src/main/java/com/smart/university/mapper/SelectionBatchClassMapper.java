package com.smart.university.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.entity.SelectionBatchClassDO;

import java.util.List;

/**
 * 选课批次开放教学班持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface SelectionBatchClassMapper extends BaseMapper<SelectionBatchClassDO> {

    /**
     * 保存批次与教学班绑定关系
     *
     * @param requestParam 绑定关系数据对象
     * @return 影响行数
     */
    default int saveSelectionBatchClass(SelectionBatchClassDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 解除批次与教学班绑定关系
     *
     * @param requestParam 绑定关系数据对象，包含 batchId 与 teachingClassId
     * @return 影响行数
     */
    default int removeSelectionBatchClass(SelectionBatchClassDO requestParam) {
        return delete(buildQueryWrapper(requestParam));
    }

    /**
     * 统计批次与教学班绑定数量
     *
     * @param requestParam 绑定关系数据对象，包含 batchId 与 teachingClassId
     * @return 数量
     */
    default long countSelectionBatchClass(SelectionBatchClassDO requestParam) {
        return selectCount(buildQueryWrapper(requestParam));
    }

    /**
     * 查询批次已开放的教学班 ID 集合
     *
     * @param batchId 批次 ID
     * @return 教学班 ID 集合
     */
    default List<Long> listTeachingClassIdByBatchId(Long batchId) {
        return selectObjs(Wrappers.<SelectionBatchClassDO>lambdaQuery()
                        .select(SelectionBatchClassDO::getTeachingClassId)
                        .eq(SelectionBatchClassDO::getBatchId, batchId))
                .stream()
                .map(each -> each == null ? null : Long.valueOf(each.toString()))
                .toList();
    }

    /**
     * 构建批次与教学班绑定关系查询条件
     *
     * @param requestParam 绑定关系数据对象
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<SelectionBatchClassDO> buildQueryWrapper(SelectionBatchClassDO requestParam) {
        LambdaQueryWrapper<SelectionBatchClassDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(requestParam.getBatchId() != null, SelectionBatchClassDO::getBatchId,
                requestParam.getBatchId());
        queryWrapper.eq(requestParam.getTeachingClassId() != null, SelectionBatchClassDO::getTeachingClassId,
                requestParam.getTeachingClassId());
        return queryWrapper;
    }
}
