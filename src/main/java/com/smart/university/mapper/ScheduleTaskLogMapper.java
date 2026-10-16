package com.smart.university.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.university.domain.entity.ScheduleTaskLogDO;

import java.util.List;

/**
 * 定时任务执行日志持久层，对外只暴露 default 方法，
 * 方法名遵循 get / list / count / save / update / remove 前缀规范
 */
public interface ScheduleTaskLogMapper extends BaseMapper<ScheduleTaskLogDO> {

    /**
     * 默认查询条数，调用方未指定条数时使用
     */
    int DEFAULT_LIMIT = 20;

    /**
     * 根据 ID 查询任务执行日志
     *
     * @param logId 日志 ID
     * @return 任务执行日志
     */
    default ScheduleTaskLogDO getScheduleTaskLogById(Long logId) {
        return selectById(logId);
    }

    /**
     * 查询指定任务最近一次执行日志
     *
     * @param taskCode 任务编码
     * @return 任务执行日志，无记录时返回 null
     */
    default ScheduleTaskLogDO getLatestScheduleTaskLog(String taskCode) {
        return selectOne(buildTaskCodeWrapper(taskCode)
                .orderByDesc(ScheduleTaskLogDO::getStartTime)
                .last("LIMIT 1"));
    }

    /**
     * 查询指定任务最近若干条执行日志，按开始时间倒序
     *
     * @param taskCode 任务编码
     * @param limit    查询条数，为空时取默认值
     * @return 任务执行日志集合
     */
    default List<ScheduleTaskLogDO> listScheduleTaskLogByTaskCode(String taskCode, Integer limit) {
        int queryLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : limit;
        // 关闭 count 查询，分页由 PaginationInnerInterceptor 完成，不手写 LIMIT
        Page<ScheduleTaskLogDO> page = new Page<>(1, queryLimit, false);
        IPage<ScheduleTaskLogDO> pageResult = selectPage(page, buildTaskCodeWrapper(taskCode)
                .orderByDesc(ScheduleTaskLogDO::getStartTime)
                .orderByDesc(ScheduleTaskLogDO::getId));
        return pageResult.getRecords();
    }

    /**
     * 统计指定任务的执行日志数量
     *
     * @param taskCode 任务编码
     * @return 数量
     */
    default long countScheduleTaskLogByTaskCode(String taskCode) {
        return selectCount(buildTaskCodeWrapper(taskCode));
    }

    /**
     * 保存任务执行日志
     *
     * @param requestParam 任务执行日志数据对象
     * @return 影响行数
     */
    default int saveScheduleTaskLog(ScheduleTaskLogDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新任务执行日志
     *
     * @param requestParam 任务执行日志数据对象
     * @return 影响行数
     */
    default int updateScheduleTaskLog(ScheduleTaskLogDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 删除任务执行日志
     *
     * @param logId 日志 ID
     * @return 影响行数
     */
    default int removeScheduleTaskLog(Long logId) {
        return deleteById(logId);
    }

    /**
     * 构建任务编码查询条件，任务编码为空时查询全部任务
     *
     * @param taskCode 任务编码
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<ScheduleTaskLogDO> buildTaskCodeWrapper(String taskCode) {
        LambdaQueryWrapper<ScheduleTaskLogDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(taskCode != null && !taskCode.isEmpty(), ScheduleTaskLogDO::getTaskCode, taskCode);
        return queryWrapper;
    }
}
