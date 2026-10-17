package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.dto.req.ScheduleTaskLogQueryReqDTO;
import com.smart.university.domain.dto.resp.ScheduleTaskLogRespDTO;
import com.smart.university.domain.dto.resp.ScheduleTaskRespDTO;
import com.smart.university.domain.dto.resp.ScheduleTaskRunRespDTO;

import java.util.List;

/**
 * 定时任务管理服务，提供任务列表、执行日志与手动触发能力（41.1 ~ 41.3）
 */
public interface ScheduleTaskManageService {

    /**
     * 查询全部定时任务及其最近一次执行情况
     *
     * @return 定时任务集合
     */
    List<ScheduleTaskRespDTO> listScheduleTask();

    /**
     * 分页查询指定任务的执行日志
     *
     * @param taskCode     任务编码
     * @param requestParam 查询条件
     * @return 执行日志分页结果
     */
    PageResult<ScheduleTaskLogRespDTO> pageScheduleTaskLog(String taskCode, ScheduleTaskLogQueryReqDTO requestParam);

    /**
     * 手动触发指定任务，同样抢分布式锁
     *
     * @param taskCode 任务编码
     * @return 手动触发结果
     */
    ScheduleTaskRunRespDTO runScheduleTask(String taskCode);
}
