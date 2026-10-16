package com.smart.university.service.impl;

import com.smart.university.domain.entity.ScheduleTaskLogDO;
import com.smart.university.mapper.ScheduleTaskLogMapper;
import com.smart.university.service.ScheduleTaskLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 定时任务执行日志服务实现
 */
@Service
@RequiredArgsConstructor
public class ScheduleTaskLogServiceImpl implements ScheduleTaskLogService {

    private final ScheduleTaskLogMapper scheduleTaskLogMapper;

    @Override
    public ScheduleTaskLogDO getScheduleTaskLogById(Long logId) {
        return scheduleTaskLogMapper.getScheduleTaskLogById(logId);
    }

    @Override
    public ScheduleTaskLogDO getLatestScheduleTaskLog(String taskCode) {
        return scheduleTaskLogMapper.getLatestScheduleTaskLog(taskCode);
    }

    @Override
    public List<ScheduleTaskLogDO> listScheduleTaskLogByTaskCode(String taskCode, Integer limit) {
        return scheduleTaskLogMapper.listScheduleTaskLogByTaskCode(taskCode, limit);
    }

    @Override
    public long countScheduleTaskLogByTaskCode(String taskCode) {
        return scheduleTaskLogMapper.countScheduleTaskLogByTaskCode(taskCode);
    }

    @Override
    public int saveScheduleTaskLog(ScheduleTaskLogDO requestParam) {
        return scheduleTaskLogMapper.saveScheduleTaskLog(requestParam);
    }

    @Override
    public int updateScheduleTaskLog(ScheduleTaskLogDO requestParam) {
        return scheduleTaskLogMapper.updateScheduleTaskLog(requestParam);
    }

    @Override
    public int removeScheduleTaskLog(Long logId) {
        return scheduleTaskLogMapper.removeScheduleTaskLog(logId);
    }
}
