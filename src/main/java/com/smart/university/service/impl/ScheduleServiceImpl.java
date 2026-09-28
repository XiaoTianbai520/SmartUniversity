package com.smart.university.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.ClassroomDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.entity.TeachingClassScheduleDO;
import com.smart.university.domain.dto.req.ScheduleConflictQueryReqDTO;
import com.smart.university.domain.dto.req.ScheduleSaveReqDTO;
import com.smart.university.domain.dto.resp.ScheduleRespDTO;
import com.smart.university.domain.event.NoticeEvent;
import com.smart.university.domain.event.NoticeEventPublisher;
import com.smart.university.domain.event.NoticeTargetResolver;
import com.smart.university.domain.enums.NoticeTypeEnum;
import com.smart.university.mapper.ClassroomMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.mapper.TeachingClassScheduleMapper;
import com.smart.university.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 教学班排课服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final TeachingClassScheduleMapper teachingClassScheduleMapper;

    private final TeachingClassMapper teachingClassMapper;

    private final ClassroomMapper classroomMapper;

    private final NoticeEventPublisher noticeEventPublisher;

    private final NoticeTargetResolver noticeTargetResolver;

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
        notifyScheduleAdjusted(teachingClassDO, requestParam, "新增");
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
        notifyScheduleAdjusted(teachingClassDO, requestParam, "调整");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeSchedule(Long teachingClassId, Long scheduleId) {
        TeachingClassScheduleDO scheduleDO = teachingClassScheduleMapper.getScheduleById(scheduleId);
        if (scheduleDO == null || !scheduleDO.getTeachingClassId().equals(teachingClassId)) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "排课不存在");
        }
        teachingClassScheduleMapper.removeSchedule(scheduleId);
        notifyScheduleRemoved(teachingClassId, scheduleDO);
    }

    @Override
    public List<TeachingClassScheduleDO> listScheduleByTeachingClassIds(List<Long> teachingClassIds) {
        if (CollUtil.isEmpty(teachingClassIds)) {
            return List.of();
        }
        return teachingClassScheduleMapper.listScheduleByTeachingClassIds(teachingClassIds);
    }

    /**
     * 通知教学班已选学生排课发生变更，通知失败不影响排课结果
     *
     * @param teachingClassDO 教学班信息
     * @param requestParam    排课入参
     * @param action          变更动作描述
     */
    private void notifyScheduleAdjusted(TeachingClassDO teachingClassDO, ScheduleSaveReqDTO requestParam,
                                        String action) {
        try {
            String scheduleText = getScheduleText(requestParam);
            noticeEventPublisher.publish(NoticeEvent.builder()
                    .noticeType(NoticeTypeEnum.COURSE_ADJUSTED)
                    .title(NoticeTypeEnum.COURSE_ADJUSTED.getDefaultTitle())
                    .content(StrUtil.format("教学班【{}】上课安排已{}：{}，请及时查看。",
                            teachingClassDO.getClassName(), action, scheduleText))
                    .receiverUserIds(noticeTargetResolver.listUserIdsByTeachingClassId(teachingClassDO.getId()))
                    .receiverRole(RoleEnum.STUDENT)
                    .bizId(null)
                    .build());
        } catch (Exception ex) {
            log.error("排课调整通知发布失败，teachingClassId：{}", teachingClassDO.getId(), ex);
        }
    }

    /**
     * 通知教学班已选学生排课被删除，通知失败不影响删除结果
     *
     * @param teachingClassId 教学班 ID
     * @param scheduleDO      被删除的排课
     */
    private void notifyScheduleRemoved(Long teachingClassId, TeachingClassScheduleDO scheduleDO) {
        try {
            noticeEventPublisher.publish(NoticeEvent.builder()
                    .noticeType(NoticeTypeEnum.COURSE_ADJUSTED)
                    .title(NoticeTypeEnum.COURSE_ADJUSTED.getDefaultTitle())
                    .content(StrUtil.format("教学班【{}】已取消一条上课安排（星期 {} 第 {} 节），请及时查看。",
                            getTeachingClassName(teachingClassId), scheduleDO.getWeekday(),
                            scheduleDO.getStartSection()))
                    .receiverUserIds(noticeTargetResolver.listUserIdsByTeachingClassId(teachingClassId))
                    .receiverRole(RoleEnum.STUDENT)
                    .bizId(null)
                    .build());
        } catch (Exception ex) {
            log.error("排课删除通知发布失败，teachingClassId：{}", teachingClassId, ex);
        }
    }

    /**
     * 拼接排课时间与地点描述，教室缺失时只返回上课时间
     *
     * @param requestParam 排课入参
     * @return 排课时间与地点描述
     */
    private String getScheduleText(ScheduleSaveReqDTO requestParam) {
        String timeText = StrUtil.format("星期 {} 第 {} 到 {} 节，第 {} 到 {} 周",
                requestParam.getWeekday(), requestParam.getStartSection(), requestParam.getEndSection(),
                requestParam.getStartWeek(), requestParam.getEndWeek());
        ClassroomDO classroomDO = classroomMapper.getClassroomById(requestParam.getClassroomId());
        if (classroomDO == null) {
            return timeText;
        }
        String placeText = StrUtil.trimToEmpty(classroomDO.getBuildingName())
                + StrUtil.trimToEmpty(classroomDO.getRoomNo());
        return StrUtil.isBlank(placeText) ? timeText : timeText + "，" + placeText;
    }

    /**
     * 获取教学班名称，教学班缺失时返回空串
     *
     * @param teachingClassId 教学班 ID
     * @return 教学班名称
     */
    private String getTeachingClassName(Long teachingClassId) {
        TeachingClassDO teachingClassDO = teachingClassMapper.getTeachingClassById(teachingClassId);
        return teachingClassDO == null ? StrUtil.EMPTY : teachingClassDO.getClassName();
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
