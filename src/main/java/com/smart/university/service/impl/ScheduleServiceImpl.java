package com.smart.university.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.ClassroomDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.entity.TeachingClassScheduleDO;
import com.smart.university.domain.dto.req.ScheduleConflictQueryReqDTO;
import com.smart.university.domain.dto.req.ScheduleSaveReqDTO;
import com.smart.university.domain.dto.resp.ScheduleRespDTO;
import com.smart.university.mapper.ClassroomMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.mapper.TeachingClassScheduleMapper;
import com.smart.university.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 教学班排课服务实现
 */
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final TeachingClassScheduleMapper teachingClassScheduleMapper;

    private final TeachingClassMapper teachingClassMapper;

    private final ClassroomMapper classroomMapper;

    @Override
    public List<ScheduleRespDTO> listSchedule(Long teachingClassId) {
        return convertToRespDTO(teachingClassScheduleMapper.listScheduleByTeachingClassId(teachingClassId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveSchedule(Long teachingClassId, ScheduleSaveReqDTO requestParam) {
        TeachingClassDO teachingClassDO = getTeachingClass(teachingClassId);
        checkClassroom(requestParam);
        checkScheduleConflict(teachingClassDO, requestParam, null);

        TeachingClassScheduleDO scheduleDO = new TeachingClassScheduleDO();
        scheduleDO.setTeachingClassId(teachingClassId);
        scheduleDO.setClassroomId(requestParam.getClassroomId());
        scheduleDO.setWeekday(requestParam.getWeekday());
        scheduleDO.setStartSection(requestParam.getStartSection());
        scheduleDO.setEndSection(requestParam.getEndSection());
        scheduleDO.setStartWeek(requestParam.getStartWeek());
        scheduleDO.setEndWeek(requestParam.getEndWeek());
        teachingClassScheduleMapper.saveSchedule(scheduleDO);
        return scheduleDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSchedule(Long teachingClassId, ScheduleSaveReqDTO requestParam) {
        TeachingClassDO teachingClassDO = getTeachingClass(teachingClassId);
        TeachingClassScheduleDO scheduleDO = teachingClassScheduleMapper.getScheduleById(requestParam.getId());
        if (scheduleDO == null || !scheduleDO.getTeachingClassId().equals(teachingClassId)) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "排课不存在");
        }
        checkClassroom(requestParam);
        checkScheduleConflict(teachingClassDO, requestParam, requestParam.getId());

        TeachingClassScheduleDO updateScheduleDO = new TeachingClassScheduleDO();
        updateScheduleDO.setId(requestParam.getId());
        updateScheduleDO.setClassroomId(requestParam.getClassroomId());
        updateScheduleDO.setWeekday(requestParam.getWeekday());
        updateScheduleDO.setStartSection(requestParam.getStartSection());
        updateScheduleDO.setEndSection(requestParam.getEndSection());
        updateScheduleDO.setStartWeek(requestParam.getStartWeek());
        updateScheduleDO.setEndWeek(requestParam.getEndWeek());
        teachingClassScheduleMapper.updateSchedule(updateScheduleDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeSchedule(Long teachingClassId, Long scheduleId) {
        TeachingClassScheduleDO scheduleDO = teachingClassScheduleMapper.getScheduleById(scheduleId);
        if (scheduleDO == null || !scheduleDO.getTeachingClassId().equals(teachingClassId)) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "排课不存在");
        }
        teachingClassScheduleMapper.removeSchedule(scheduleId);
    }

    @Override
    public List<TeachingClassScheduleDO> listScheduleByTeachingClassIds(List<Long> teachingClassIds) {
        if (CollUtil.isEmpty(teachingClassIds)) {
            return List.of();
        }
        return teachingClassScheduleMapper.listScheduleByTeachingClassIds(teachingClassIds);
    }

    /**
     * 校验教室存在、启用且容量满足教学班人数
     *
     * @param requestParam 排课入参
     */
    private void checkClassroom(ScheduleSaveReqDTO requestParam) {
        ClassroomDO classroomDO = classroomMapper.getClassroomById(requestParam.getClassroomId());
        if (classroomDO == null || Integer.valueOf(0).equals(classroomDO.getStatus())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "教室不存在或已停用");
        }
    }

    /**
     * 检测教师与教室时间冲突
     *
     * @param teachingClassDO 教学班信息
     * @param requestParam    排课入参
     * @param excludeId       排除的排课 ID
     */
    private void checkScheduleConflict(TeachingClassDO teachingClassDO, ScheduleSaveReqDTO requestParam,
                                       Long excludeId) {
        ScheduleConflictQueryReqDTO conflictParam = new ScheduleConflictQueryReqDTO();
        conflictParam.setTeachingClassId(teachingClassDO.getId());
        conflictParam.setTeacherId(teachingClassDO.getTeacherId());
        conflictParam.setClassroomId(requestParam.getClassroomId());
        conflictParam.setWeekday(requestParam.getWeekday());
        conflictParam.setStartSection(requestParam.getStartSection());
        conflictParam.setEndSection(requestParam.getEndSection());
        conflictParam.setStartWeek(requestParam.getStartWeek());
        conflictParam.setEndWeek(requestParam.getEndWeek());
        conflictParam.setExcludeScheduleId(excludeId);
        if (teachingClassScheduleMapper.countTeacherConflictSchedule(conflictParam) > 0) {
            throw new BizException(ResultCodeEnum.TEACHER_SCHEDULE_CONFLICT);
        }
        if (teachingClassScheduleMapper.countClassroomConflictSchedule(conflictParam) > 0) {
            throw new BizException(ResultCodeEnum.CLASSROOM_SCHEDULE_CONFLICT);
        }
    }

    private TeachingClassDO getTeachingClass(Long teachingClassId) {
        TeachingClassDO result = teachingClassMapper.getTeachingClassById(teachingClassId);
        if (result == null) {
            throw new BizException(ResultCodeEnum.TEACHING_CLASS_NOT_EXIST);
        }
        return result;
    }

    /**
     * 转换排课出参，并补齐教室信息
     *
     * @param scheduleDOList 排课数据对象集合
     * @return 排课出参集合
     */
    private List<ScheduleRespDTO> convertToRespDTO(List<TeachingClassScheduleDO> scheduleDOList) {
        if (CollUtil.isEmpty(scheduleDOList)) {
            return List.of();
        }
        Map<Long, ClassroomDO> classroomMap = classroomMapper
                .listClassroomByIds(scheduleDOList.stream()
                        .map(TeachingClassScheduleDO::getClassroomId).distinct().toList())
                .stream().collect(Collectors.toMap(ClassroomDO::getId, Function.identity()));
        return scheduleDOList.stream().map(each -> {
            ScheduleRespDTO result = BeanUtil.copyProperties(each, ScheduleRespDTO.class);
            ClassroomDO classroomDO = classroomMap.get(each.getClassroomId());
            result.setBuildingName(classroomDO == null ? null : classroomDO.getBuildingName());
            result.setRoomNo(classroomDO == null ? null : classroomDO.getRoomNo());
            return result;
        }).toList();
    }
}
