package com.smart.university.service;

import com.smart.university.domain.entity.TeachingClassScheduleDO;
import com.smart.university.domain.dto.req.ScheduleSaveReqDTO;
import com.smart.university.domain.dto.resp.ScheduleRespDTO;

import java.util.List;

/**
 * 教学班排课服务
 */
public interface ScheduleService {

    /**
     * 查询教学班排课列表
     *
     * @param teachingClassId 教学班 ID
     * @return 排课出参集合
     */
    List<ScheduleRespDTO> listSchedule(Long teachingClassId);

    /**
     * 保存排课，保存前执行教师与教室时间冲突检测
     *
     * @param teachingClassId 教学班 ID
     * @param requestParam    排课入参
     * @return 排课 ID
     */
    Long saveSchedule(Long teachingClassId, ScheduleSaveReqDTO requestParam);

    /**
     * 修改排课，修改时重新检测冲突并排除自身
     *
     * @param teachingClassId 教学班 ID
     * @param requestParam    排课入参
     */
    void updateSchedule(Long teachingClassId, ScheduleSaveReqDTO requestParam);

    /**
     * 删除排课
     *
     * @param teachingClassId 教学班 ID
     * @param scheduleId      排课 ID
     */
    void removeSchedule(Long teachingClassId, Long scheduleId);

    /**
     * 批量查询排课
     *
     * @param teachingClassIds 教学班 ID 集合
     * @return 排课数据对象集合
     */
    List<TeachingClassScheduleDO> listScheduleByTeachingClassIds(List<Long> teachingClassIds);
}
