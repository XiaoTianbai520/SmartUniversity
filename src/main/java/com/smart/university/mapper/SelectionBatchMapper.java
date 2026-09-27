package com.smart.university.mapper;

import com.smart.university.common.util.EnumParseUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.SelectionBatchPageQueryReqDTO;
import com.smart.university.domain.entity.SelectionBatchDO;
import com.smart.university.domain.enums.SelectionBatchStatusEnum;

import java.time.LocalDateTime;

/**
 * 选课批次持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface SelectionBatchMapper extends BaseMapper<SelectionBatchDO> {

    /**
     * 根据 ID 查询选课批次
     *
     * @param batchId 批次 ID
     * @return 选课批次信息
     */
    default SelectionBatchDO getSelectionBatchById(Long batchId) {
        return selectById(batchId);
    }

    /**
     * 查询指定学期下正在进行且处于时间区间内的选课批次
     *
     * @param semesterId 学期 ID
     * @return 选课批次信息
     */
    default SelectionBatchDO getCurrentSelectionBatch(Long semesterId) {
        LocalDateTime now = LocalDateTime.now();
        return selectOne(Wrappers.<SelectionBatchDO>lambdaQuery()
                .eq(SelectionBatchDO::getSemesterId, semesterId)
                .eq(SelectionBatchDO::getStatus, SelectionBatchStatusEnum.IN_PROGRESS)
                .le(SelectionBatchDO::getStartTime, now)
                .ge(SelectionBatchDO::getEndTime, now)
                .orderByAsc(SelectionBatchDO::getStartTime)
                .last("LIMIT 1"));
    }

    /**
     * 按条件统计选课批次数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    default long countSelectionBatchByCondition(SelectionBatchPageQueryReqDTO requestParam) {
        return selectCount(buildQueryWrapper(requestParam));
    }

    /**
     * 按条件分页查询选课批次
     *
     * @param page         分页参数
     * @param requestParam 查询条件
     * @return 分页结果
     */
    default IPage<SelectionBatchDO> listSelectionBatchByCondition(IPage<SelectionBatchDO> page,
                                                                  SelectionBatchPageQueryReqDTO requestParam) {
        return selectPage(page, buildQueryWrapper(requestParam));
    }

    /**
     * 保存选课批次
     *
     * @param requestParam 选课批次数据对象
     * @return 影响行数
     */
    default int saveSelectionBatch(SelectionBatchDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新选课批次
     *
     * @param requestParam 选课批次数据对象
     * @return 影响行数
     */
    default int updateSelectionBatch(SelectionBatchDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 构建选课批次查询条件
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<SelectionBatchDO> buildQueryWrapper(SelectionBatchPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<SelectionBatchDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(requestParam.getSemesterId() != null, SelectionBatchDO::getSemesterId,
                requestParam.getSemesterId());
        SelectionBatchStatusEnum status = EnumParseUtil.parseOrNull(SelectionBatchStatusEnum.class,
                requestParam.getStatus());
        queryWrapper.eq(status != null, SelectionBatchDO::getStatus, status);
        queryWrapper.orderByDesc(SelectionBatchDO::getStartTime);
        return queryWrapper;
    }
}
