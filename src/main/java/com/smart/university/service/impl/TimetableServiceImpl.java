package com.smart.university.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.ClassroomDO;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.TeacherDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.entity.TeachingClassScheduleDO;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.dto.resp.TimetableRespDTO;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.mapper.ClassroomMapper;
import com.smart.university.mapper.CourseMapper;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.mapper.TeachingClassScheduleMapper;
import com.smart.university.mapper.TeacherMapper;
import com.smart.university.service.SemesterService;
import com.smart.university.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 课表服务实现
 */
@Service
@RequiredArgsConstructor
public class TimetableServiceImpl implements TimetableService {

    private final CourseSelectionMapper courseSelectionMapper;

    private final TeachingClassMapper teachingClassMapper;

    private final TeachingClassScheduleMapper teachingClassScheduleMapper;

    private final CourseMapper courseMapper;

    private final ClassroomMapper classroomMapper;

    private final SemesterService semesterService;

    private final TeacherMapper teacherMapper;

    @Override
    public List<TimetableRespDTO> listTimetable(Long semesterId) {
        Long studentId = UserContextHolder.getStudentId();
        if (studentId == null) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_EXIST);
        }
        Long targetSemesterId = semesterId == null ? semesterService.getCurrentSemester().getId() : semesterId;
        StudentSelectionQueryReqDTO queryParam = new StudentSelectionQueryReqDTO();
        queryParam.setStudentId(studentId);
        queryParam.setSemesterId(targetSemesterId);
        queryParam.setStatus(SelectionStatusEnum.SELECTED.name());
        List<CourseSelectionDO> selectionDOList = courseSelectionMapper.listSelectionByStudent(queryParam);
        if (CollUtil.isEmpty(selectionDOList)) {
            return List.of();
        }
        List<Long> teachingClassIds = selectionDOList.stream()
                .map(CourseSelectionDO::getTeachingClassId).distinct().toList();
        List<TeachingClassDO> teachingClassDOList = teachingClassMapper.listTeachingClassByIds(teachingClassIds);
        Map<Long, TeachingClassDO> teachingClassMap = teachingClassDOList.stream()
                .collect(Collectors.toMap(TeachingClassDO::getId, Function.identity()));
        Map<Long, CourseDO> courseMap = courseMapper
                .listCourseByIds(teachingClassDOList.stream().map(TeachingClassDO::getCourseId).distinct().toList())
                .stream().collect(Collectors.toMap(CourseDO::getId, Function.identity()));
        Map<Long, String> teacherMap = teacherMapper
                .listTeacherByIds(teachingClassDOList.stream().map(TeachingClassDO::getTeacherId).distinct().toList())
                .stream().collect(Collectors.toMap(TeacherDO::getId, TeacherDO::getTeacherName));
        List<TeachingClassScheduleDO> scheduleDOList =
                teachingClassScheduleMapper.listScheduleByTeachingClassIds(teachingClassIds);
        Map<Long, ClassroomDO> classroomMap = classroomMapper
                .listClassroomByIds(scheduleDOList.stream()
                        .map(TeachingClassScheduleDO::getClassroomId).distinct().toList())
                .stream().collect(Collectors.toMap(ClassroomDO::getId, Function.identity()));

        return scheduleDOList.stream().map(each -> {
            TeachingClassDO teachingClassDO = teachingClassMap.get(each.getTeachingClassId());
            CourseDO courseDO = teachingClassDO == null ? null : courseMap.get(teachingClassDO.getCourseId());
            ClassroomDO classroomDO = classroomMap.get(each.getClassroomId());
            TimetableRespDTO result = new TimetableRespDTO();
            result.setWeekday(each.getWeekday());
            result.setStartSection(each.getStartSection());
            result.setEndSection(each.getEndSection());
            result.setStartWeek(each.getStartWeek());
            result.setEndWeek(each.getEndWeek());
            result.setCourseName(courseDO == null ? null : courseDO.getCourseName());
            result.setTeacherName(teachingClassDO == null ? null : teacherMap.get(teachingClassDO.getTeacherId()));
            result.setBuildingName(classroomDO == null ? null : classroomDO.getBuildingName());
            result.setRoomNo(classroomDO == null ? null : classroomDO.getRoomNo());
            return result;
        }).toList();
    }
}
