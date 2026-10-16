package com.smart.university.service;

import com.smart.university.domain.entity.ScheduleTaskLogDO;

import java.util.List;

/**
 * 定时任务执行日志服务，负责日志落库与查询
 */
public interface ScheduleTaskLogService {

    /**
     * 根据 ID 查询任务执行日志
     *
     * @param logId 日志 ID
     * @return 任务执行日志
     */
    ScheduleTaskLogDO getScheduleTaskLogById(Long logId);

    /**
     * 查询指定任务最近一次执行日志
     *
     * @param taskCode 任务编码
     * @return 任务执行日志，无记录时返回 null
     */
    ScheduleTaskLogDO getLatestScheduleTaskLog(String taskCode);

    /**
     * 查询指定任务最近若干条执行日志
     *
     * @param taskCode 任务编码
     * @param limit    查询条数，为空时取默认值
     * @return 任务执行日志集合
     */
    List<ScheduleTaskLogDO> listScheduleTaskLogByTaskCode(String taskCode, Integer limit);

    /**
     * 统计指定任务的执行日志数量
     *
     * @param taskCode 任务编码
     * @return 数量
     */
    long countScheduleTaskLogByTaskCode(String taskCode);

    /**
     * 保存任务执行日志，保存后回填自增主键
     *
     * @param requestParam 任务执行日志数据对象
     * @return 影响行数
     */
    int saveScheduleTaskLog(ScheduleTaskLogDO requestParam);

    /**
     * 更新任务执行日志
     *
     * @param requestParam 任务执行日志数据对象
     * @return 影响行数
     */
    int updateScheduleTaskLog(ScheduleTaskLogDO requestParam);

    /**
     * 删除任务执行日志
     *
     * @param logId 日志 ID
     * @return 影响行数
     */
    int removeScheduleTaskLog(Long logId);
}
