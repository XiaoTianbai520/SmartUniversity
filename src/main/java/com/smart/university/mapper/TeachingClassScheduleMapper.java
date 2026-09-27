package com.smart.university.mapper;

import com.smart.university.domain.entity.TeachingClassScheduleDO;
import com.smart.university.domain.dto.req.ScheduleConflictQueryReqDTO;

import java.util.List;

/**
 * 教学班排课持久层
 */
public interface TeachingClassScheduleMapper {

    /**
     * 根据 ID 查询排课
     *
     * @param scheduleId 排课 ID
     * @return 排课信息
     */
    TeachingClassScheduleDO getScheduleById(Long scheduleId);

    /**
     * 查询教学班下的全部排课
     *
     * @param teachingClassId 教学班 ID
     * @return 排课信息集合
     */
    List<TeachingClassScheduleDO> listScheduleByTeachingClassId(Long teachingClassId);

    /**
     * 批量查询多个教学班下的排课
     *
     * @param teachingClassIds 教学班 ID 集合
     * @return 排课信息集合
     */
    List<TeachingClassScheduleDO> listScheduleByTeachingClassIds(List<Long> teachingClassIds);

    /**
     * 保存排课
     *
     * @param requestParam 排课数据对象
     * @return 影响行数
     */
    int saveSchedule(TeachingClassScheduleDO requestParam);

    /**
     * 更新排课
     *
     * @param requestParam 排课数据对象
     * @return 影响行数
     */
    int updateSchedule(TeachingClassScheduleDO requestParam);

    /**
     * 删除排课
     *
     * @param scheduleId 排课 ID
     * @return 影响行数
     */
    int removeSchedule(Long scheduleId);

    /**
     * 统计与当前排课存在教师时间冲突的排课数量
     *
     * @param requestParam 冲突检测条件
     * @return 冲突数量
     */
    long countTeacherConflictSchedule(ScheduleConflictQueryReqDTO requestParam);

    /**
     * 统计与当前排课存在教室时间冲突的排课数量
     *
     * @param requestParam 冲突检测条件
     * @return 冲突数量
     */
    long countClassroomConflictSchedule(ScheduleConflictQueryReqDTO requestParam);
}
