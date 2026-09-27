package com.smart.university.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.university.common.base.PageResult;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.AcademicClassDO;
import com.smart.university.domain.entity.ClassroomDO;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.MajorDO;
import com.smart.university.domain.entity.GradeCohortDO;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.entity.TeachingClassGradeDO;
import com.smart.university.domain.entity.TeachingClassMajorDO;
import com.smart.university.domain.entity.TeacherDO;
import com.smart.university.domain.entity.TeachingClassScheduleDO;
import com.smart.university.domain.dto.req.StudentTeachingClassPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeachingClassPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeachingClassSaveReqDTO;
import com.smart.university.domain.dto.req.TeacherStudentPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeachingClassStatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.CourseBriefRespDTO;
import com.smart.university.domain.dto.resp.GradeRespDTO;
import com.smart.university.domain.dto.resp.MajorRespDTO;
import com.smart.university.domain.dto.resp.ScheduleRespDTO;
import com.smart.university.domain.dto.resp.StudentTeachingClassDetailRespDTO;
import com.smart.university.domain.dto.resp.StudentTeachingClassRespDTO;
import com.smart.university.domain.dto.resp.TeacherBriefRespDTO;
import com.smart.university.domain.dto.resp.TeacherClassStudentRespDTO;
import com.smart.university.domain.dto.resp.TeacherTeachingClassRespDTO;
import com.smart.university.domain.dto.resp.TeachingClassRespDTO;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.domain.enums.TeachingClassStatusEnum;
import com.smart.university.mapper.AcademicClassMapper;
import com.smart.university.mapper.ClassroomMapper;
import com.smart.university.mapper.CourseMapper;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.GradeCohortMapper;
import com.smart.university.mapper.MajorMapper;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.mapper.TeachingClassGradeMapper;
import com.smart.university.mapper.TeachingClassMajorMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.mapper.TeacherMapper;
import com.smart.university.mapper.TeachingClassScheduleMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.university.service.SelectionBatchService;
import com.smart.university.service.SemesterService;
import com.smart.university.service.TeachingClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 教学班服务实现
 */
@Service
@RequiredArgsConstructor
public class TeachingClassServiceImpl extends ServiceImpl<TeachingClassMapper, TeachingClassDO> implements TeachingClassService {

    private final TeachingClassMapper teachingClassMapper;

    private final TeachingClassMajorMapper teachingClassMajorMapper;

    private final TeachingClassGradeMapper teachingClassGradeMapper;

    private final TeachingClassScheduleMapper teachingClassScheduleMapper;

    private final CourseSelectionMapper courseSelectionMapper;

    private final CourseMapper courseMapper;

    private final MajorMapper majorMapper;

    private final GradeCohortMapper gradeCohortMapper;

    private final ClassroomMapper classroomMapper;

    private final AcademicClassMapper academicClassMapper;

    private final TeacherMapper teacherMapper;

    private final StudentMapper studentMapper;

    private final SemesterService semesterService;

    private final SelectionBatchService selectionBatchService;

    @Override
    public PageResult<TeachingClassRespDTO> pageTeachingClass(TeachingClassPageQueryReqDTO requestParam) {
        Page<TeachingClassDO> page = Page.of(requestParam.getCurrentPage(), requestParam.getLimit());
        IPage<TeachingClassDO> pageResult = teachingClassMapper.listTeachingClassByCondition(page, requestParam);
        List<TeachingClassDO> teachingClassDOList = pageResult.getRecords();
        return new PageResult<>(convertToRespDTO(teachingClassDOList), requestParam.getCurrentPage(),
                requestParam.getLimit(), pageResult.getTotal());
    }

    @Override
    public TeachingClassRespDTO getTeachingClassDetail(Long teachingClassId) {
        return convertToRespDTO(List.of(getTeachingClassById(teachingClassId))).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveTeachingClass(TeachingClassSaveReqDTO requestParam) {
        CourseDO courseDO = courseMapper.getCourseById(requestParam.getCourseId());
        if (courseDO == null) {
            throw new BizException(ResultCodeEnum.COURSE_NOT_EXIST);
        }
        TeachingClassStatusEnum statusEnum = TeachingClassStatusEnum.DRAFT;
        if (requestParam.getId() != null) {
            statusEnum = getTeachingClassById(requestParam.getId()).getStatus();
        } else if (teachingClassMapper.getTeachingClassByClassCode(requestParam.getClassCode()) != null) {
            throw new BizException(ResultCodeEnum.TEACHING_CLASS_CODE_EXIST);
        }
        TeachingClassDO teachingClassDO = new TeachingClassDO();
        teachingClassDO.setClassCode(requestParam.getClassCode());
        teachingClassDO.setClassName(requestParam.getClassName());
        teachingClassDO.setCourseId(requestParam.getCourseId());
        teachingClassDO.setSemesterId(requestParam.getSemesterId());
        teachingClassDO.setTeacherId(requestParam.getTeacherId());
        teachingClassDO.setCapacity(requestParam.getCapacity());
        teachingClassDO.setStatus(statusEnum);
        if (requestParam.getId() == null) {
            teachingClassMapper.saveTeachingClass(teachingClassDO);
        } else {
            teachingClassDO.setId(requestParam.getId());
            teachingClassMapper.updateTeachingClass(teachingClassDO);
            teachingClassMajorMapper.removeTeachingClassMajorByTeachingClassId(requestParam.getId());
            teachingClassGradeMapper.removeTeachingClassGradeByTeachingClassId(requestParam.getId());
        }
        Long teachingClassId = teachingClassDO.getId();
        for (Long each : CollUtil.defaultIfEmpty(requestParam.getMajorIds(), List.of())) {
            TeachingClassMajorDO teachingClassMajorDO = new TeachingClassMajorDO();
            teachingClassMajorDO.setTeachingClassId(teachingClassId);
            teachingClassMajorDO.setMajorId(each);
            teachingClassMajorMapper.saveTeachingClassMajor(teachingClassMajorDO);
        }
        for (Long each : CollUtil.defaultIfEmpty(requestParam.getGradeIds(), List.of())) {
            TeachingClassGradeDO teachingClassGradeDO = new TeachingClassGradeDO();
            teachingClassGradeDO.setTeachingClassId(teachingClassId);
            teachingClassGradeDO.setGradeId(each);
            teachingClassGradeMapper.saveTeachingClassGrade(teachingClassGradeDO);
        }
        return teachingClassId;
    }

    @Override
    public void updateTeachingClassStatus(Long teachingClassId, TeachingClassStatusUpdateReqDTO requestParam) {
        TeachingClassStatusEnum statusEnum = TeachingClassStatusEnum.getByCode(requestParam.getStatus());
        if (statusEnum == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "教学班状态不合法");
        }
        TeachingClassDO teachingClassDO = new TeachingClassDO();
        teachingClassDO.setId(teachingClassId);
        teachingClassDO.setStatus(statusEnum);
        teachingClassMapper.updateTeachingClass(teachingClassDO);
    }

    @Override
    public PageResult<StudentTeachingClassRespDTO> pageStudentVisibleTeachingClass(
            StudentTeachingClassPageQueryReqDTO requestParam) {
        StudentDO studentDO = getCurrentStudent();
        Long semesterId = semesterService.getCurrentSemester().getId();
        Long batchId = selectionBatchService.getCurrentSelectionBatch(semesterId).getId();
        requestParam.setStudentId(studentDO.getId());
        requestParam.setMajorId(studentDO.getMajorId());
        requestParam.setGradeId(studentDO.getGradeId());
        requestParam.setSemesterId(semesterId);
        requestParam.setBatchId(batchId);

        Page<TeachingClassDO> page = Page.of(requestParam.getCurrentPage(), requestParam.getLimit());
        IPage<TeachingClassDO> pageResult = teachingClassMapper.listStudentVisibleTeachingClass(page, requestParam);
        List<TeachingClassDO> teachingClassDOList = pageResult.getRecords();
        Map<Long, CourseDO> courseMap = buildCourseMap(teachingClassDOList);
        Map<Long, String> teacherMap = buildTeacherMap(teachingClassDOList);
        Map<Long, List<TeachingClassScheduleDO>> scheduleMap = buildScheduleMap(
                teachingClassDOList.stream().map(TeachingClassDO::getId).toList());
        List<StudentTeachingClassRespDTO> records = teachingClassDOList.stream().map(each -> {
            CourseDO courseDO = courseMap.get(each.getCourseId());
            long selectedCount = courseSelectionMapper.countSelectedByTeachingClassId(each.getId());
            StudentTeachingClassRespDTO result = new StudentTeachingClassRespDTO();
            result.setTeachingClassId(each.getId());
            result.setClassCode(each.getClassCode());
            result.setClassName(each.getClassName());
            result.setCourseId(each.getCourseId());
            result.setCourseCode(courseDO == null ? null : courseDO.getCourseCode());
            result.setCourseName(courseDO == null ? null : courseDO.getCourseName());
            result.setCourseType(courseDO == null || courseDO.getCourseType() == null
                    ? null : courseDO.getCourseType().name());
            result.setCredit(courseDO == null ? null : courseDO.getCredit());
            result.setTeacherId(each.getTeacherId());
            result.setTeacherName(teacherMap.get(each.getTeacherId()));
            result.setCapacity(each.getCapacity());
            result.setSelectedCount(selectedCount);
            result.setRemainingCount(Math.max(each.getCapacity() - selectedCount, 0L));
            result.setSchedules(convertSchedule(scheduleMap.getOrDefault(each.getId(), List.of())));
            return result;
        }).toList();
        return new PageResult<>(records, requestParam.getCurrentPage(), requestParam.getLimit(), pageResult.getTotal());
    }

    @Override
    public StudentTeachingClassDetailRespDTO getStudentTeachingClassDetail(Long studentId, Long teachingClassId) {
        StudentDO studentDO = studentMapper.getStudentById(studentId);
        if (studentDO == null) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_EXIST);
        }
        TeachingClassDO teachingClassDO = getTeachingClassById(teachingClassId);
        checkStudentViewPermission(studentDO, teachingClassDO);

        CourseDO courseDO = courseMapper.getCourseById(teachingClassDO.getCourseId());
        long selectedCount = courseSelectionMapper.countSelectedByTeachingClassId(teachingClassId);
        StudentTeachingClassDetailRespDTO result = new StudentTeachingClassDetailRespDTO();
        result.setTeachingClassId(teachingClassDO.getId());
        result.setClassCode(teachingClassDO.getClassCode());
        result.setClassName(teachingClassDO.getClassName());
        result.setCapacity(teachingClassDO.getCapacity());
        result.setSelectedCount(selectedCount);
        result.setRemainingCount(Math.max(teachingClassDO.getCapacity() - selectedCount, 0L));

        CourseBriefRespDTO course = new CourseBriefRespDTO();
        course.setCourseId(courseDO == null ? null : courseDO.getId());
        course.setCourseCode(courseDO == null ? null : courseDO.getCourseCode());
        course.setCourseName(courseDO == null ? null : courseDO.getCourseName());
        course.setDescription(courseDO == null ? null : courseDO.getDescription());
        course.setCredit(courseDO == null ? null : courseDO.getCredit());
        course.setCourseType(courseDO == null || courseDO.getCourseType() == null
                ? null : courseDO.getCourseType().name());
        result.setCourse(course);

        TeacherBriefRespDTO teacher = new TeacherBriefRespDTO();
        teacher.setTeacherId(teachingClassDO.getTeacherId());
        TeacherDO teacherDO = teacherMapper.getTeacherById(teachingClassDO.getTeacherId());
        teacher.setTeacherName(teacherDO == null ? null : teacherDO.getTeacherName());
        result.setTeacher(teacher);

        List<Long> majorIds = teachingClassMajorMapper.listMajorIdByTeachingClassId(teachingClassId);
        result.setAllowedMajors(majorMapper.listMajorByIds(majorIds).stream().map(each -> {
            MajorRespDTO majorRespDTO = new MajorRespDTO();
            majorRespDTO.setMajorId(each.getId());
            majorRespDTO.setMajorCode(each.getMajorCode());
            majorRespDTO.setMajorName(each.getMajorName());
            return majorRespDTO;
        }).toList());
        List<Long> gradeIds = teachingClassGradeMapper.listGradeIdByTeachingClassId(teachingClassId);
        result.setAllowedGrades(gradeCohortMapper.listGradeCohortByIds(gradeIds).stream().map(each -> {
            GradeRespDTO gradeRespDTO = new GradeRespDTO();
            gradeRespDTO.setGradeId(each.getId());
            gradeRespDTO.setGradeName(each.getGradeName());
            gradeRespDTO.setEntryYear(each.getEntryYear());
            return gradeRespDTO;
        }).toList());
        result.setSchedules(convertSchedule(
                teachingClassScheduleMapper.listScheduleByTeachingClassId(teachingClassId)));
        return result;
    }

    @Override
    public TeachingClassDO getTeachingClassById(Long teachingClassId) {
        TeachingClassDO result = teachingClassMapper.getTeachingClassById(teachingClassId);
        if (result == null) {
            throw new BizException(ResultCodeEnum.TEACHING_CLASS_NOT_EXIST);
        }
        return result;
    }

    @Override
    public List<TeacherTeachingClassRespDTO> listTeacherTeachingClass(Long semesterId) {
        Long teacherId = UserContextHolder.getTeacherId();
        if (teacherId == null) {
            throw new BizException(ResultCodeEnum.NO_TEACHING_CLASS_PERMISSION);
        }
        Long targetSemesterId = semesterId == null ? semesterService.getCurrentSemester().getId() : semesterId;
        TeachingClassDO queryParam = new TeachingClassDO();
        queryParam.setTeacherId(teacherId);
        queryParam.setSemesterId(targetSemesterId);
        List<TeachingClassDO> teachingClassDOList = teachingClassMapper.listTeachingClassByTeacher(queryParam);
        if (CollUtil.isEmpty(teachingClassDOList)) {
            return List.of();
        }
        Map<Long, CourseDO> courseMap = buildCourseMap(teachingClassDOList);
        Map<Long, List<TeachingClassScheduleDO>> scheduleMap = buildScheduleMap(
                teachingClassDOList.stream().map(TeachingClassDO::getId).toList());
        return teachingClassDOList.stream().map(each -> {
            CourseDO courseDO = courseMap.get(each.getCourseId());
            TeacherTeachingClassRespDTO result = new TeacherTeachingClassRespDTO();
            result.setTeachingClassId(each.getId());
            result.setClassCode(each.getClassCode());
            result.setClassName(each.getClassName());
            result.setCourseName(courseDO == null ? null : courseDO.getCourseName());
            result.setCapacity(each.getCapacity());
            result.setSelectedCount(courseSelectionMapper.countSelectedByTeachingClassId(each.getId()));
            result.setSchedules(convertSchedule(scheduleMap.getOrDefault(each.getId(), List.of())));
            return result;
        }).toList();
    }

    @Override
    public PageResult<TeacherClassStudentRespDTO> pageTeacherClassStudent(Long teachingClassId,
                                                                          TeacherStudentPageQueryReqDTO requestParam) {
        Long teacherId = UserContextHolder.getTeacherId();
        TeachingClassDO teachingClassDO = getTeachingClassById(teachingClassId);
        if (teacherId == null || !teacherId.equals(teachingClassDO.getTeacherId())) {
            throw new BizException(ResultCodeEnum.NO_TEACHING_CLASS_PERMISSION);
        }
        CourseSelectionDO selectionParam = new CourseSelectionDO();
        selectionParam.setTeachingClassId(teachingClassId);
        selectionParam.setStatus(SelectionStatusEnum.SELECTED);
        List<Long> studentIds = courseSelectionMapper.listSelectionByCondition(selectionParam).stream()
                .map(CourseSelectionDO::getStudentId).distinct().toList();
        if (CollUtil.isEmpty(studentIds)) {
            return PageResult.empty(requestParam.getCurrentPage(), requestParam.getLimit());
        }
        List<StudentDO> studentDOList = studentMapper.listStudentByIds(studentIds).stream()
                .filter(each -> matchKeyword(each, requestParam.getKeyword()))
                .toList();
        long total = studentDOList.size();
        if (total <= 0) {
            return PageResult.empty(requestParam.getCurrentPage(), requestParam.getLimit());
        }
        int fromIndex = (int) Math.min(requestParam.getOffset(), total);
        int toIndex = (int) Math.min(fromIndex + requestParam.getLimit(), total);
        List<StudentDO> pageList = studentDOList.subList(fromIndex, toIndex);
        Map<Long, MajorDO> majorMap = majorMapper
                .listMajorByIds(pageList.stream().map(StudentDO::getMajorId).distinct().toList())
                .stream().collect(Collectors.toMap(MajorDO::getId, Function.identity()));
        Map<Long, GradeCohortDO> gradeMap = gradeCohortMapper
                .listGradeCohortByIds(pageList.stream().map(StudentDO::getGradeId).distinct().toList())
                .stream().collect(Collectors.toMap(GradeCohortDO::getId, Function.identity()));
        Map<Long, AcademicClassDO> classMap = academicClassMapper
                .listAcademicClassByIds(pageList.stream().map(StudentDO::getClassId).distinct().toList())
                .stream().collect(Collectors.toMap(AcademicClassDO::getId, Function.identity()));
        List<TeacherClassStudentRespDTO> records = pageList.stream().map(each -> {
            MajorDO majorDO = majorMap.get(each.getMajorId());
            GradeCohortDO gradeCohortDO = gradeMap.get(each.getGradeId());
            AcademicClassDO academicClassDO = classMap.get(each.getClassId());
            TeacherClassStudentRespDTO result = new TeacherClassStudentRespDTO();
            result.setStudentId(each.getId());
            result.setStudentNo(each.getStudentNo());
            result.setStudentName(each.getStudentName());
            result.setMajorName(majorDO == null ? null : majorDO.getMajorName());
            result.setGradeName(gradeCohortDO == null ? null : gradeCohortDO.getGradeName());
            result.setClassName(academicClassDO == null ? null : academicClassDO.getClassName());
            return result;
        }).toList();
        return new PageResult<>(records, requestParam.getCurrentPage(), requestParam.getLimit(), total);
    }

    /**
     * 判断学生是否命中关键字
     *
     * @param studentDO 学生信息
     * @param keyword   学号或姓名关键字
     * @return 命中返回 true
     */
    private boolean matchKeyword(StudentDO studentDO, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        return studentDO.getStudentNo().contains(keyword) || studentDO.getStudentName().contains(keyword);
    }

    /**
     * 校验学生的专业与年级是否满足教学班开放范围
     *
     * @param studentDO       学生信息
     * @param teachingClassDO 教学班信息
     */
    private void checkStudentViewPermission(StudentDO studentDO, TeachingClassDO teachingClassDO) {
        TeachingClassMajorDO majorParam = new TeachingClassMajorDO();
        majorParam.setTeachingClassId(teachingClassDO.getId());
        majorParam.setMajorId(studentDO.getMajorId());
        if (teachingClassMajorMapper.countTeachingClassMajor(majorParam) <= 0) {
            throw new BizException(ResultCodeEnum.NO_COURSE_VIEW_PERMISSION);
        }
        TeachingClassGradeDO gradeParam = new TeachingClassGradeDO();
        gradeParam.setTeachingClassId(teachingClassDO.getId());
        gradeParam.setGradeId(studentDO.getGradeId());
        if (teachingClassGradeMapper.countTeachingClassGrade(gradeParam) <= 0) {
            throw new BizException(ResultCodeEnum.NO_COURSE_VIEW_PERMISSION);
        }
    }

    /**
     * 获取当前登录学生
     *
     * @return 学生数据对象
     */
    private StudentDO getCurrentStudent() {
        Long studentId = UserContextHolder.getStudentId();
        if (studentId == null) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_EXIST);
        }
        StudentDO result = studentMapper.getStudentById(studentId);
        if (result == null) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_EXIST);
        }
        return result;
    }

    /**
     * 构建课程映射
     *
     * @param teachingClassDOList 教学班数据对象集合
     * @return 课程 ID 与课程信息的映射
     */
    private Map<Long, CourseDO> buildCourseMap(List<TeachingClassDO> teachingClassDOList) {
        List<Long> courseIds = teachingClassDOList.stream()
                .map(TeachingClassDO::getCourseId).distinct().toList();
        if (CollUtil.isEmpty(courseIds)) {
            return Map.of();
        }
        return courseMapper.listCourseByIds(courseIds).stream()
                .collect(Collectors.toMap(CourseDO::getId, Function.identity()));
    }

    /**
     * 构建教学班任课教师姓名映射
     *
     * @param teachingClassDOList 教学班数据对象集合
     * @return 教师 ID 与教师姓名的映射
     */
    private Map<Long, String> buildTeacherMap(List<TeachingClassDO> teachingClassDOList) {
        List<Long> teacherIds = teachingClassDOList.stream()
                .map(TeachingClassDO::getTeacherId).distinct().toList();
        if (CollUtil.isEmpty(teacherIds)) {
            return Map.of();
        }
        return teacherMapper.listTeacherByIds(teacherIds).stream()
                .collect(Collectors.toMap(TeacherDO::getId, TeacherDO::getTeacherName));
    }

    /**
     * 构建教学班排课映射
     *
     * @param teachingClassIds 教学班 ID 集合
     * @return 教学班 ID 与排课集合的映射
     */
    private Map<Long, List<TeachingClassScheduleDO>> buildScheduleMap(List<Long> teachingClassIds) {
        if (CollUtil.isEmpty(teachingClassIds)) {
            return Map.of();
        }
        return teachingClassScheduleMapper.listScheduleByTeachingClassIds(teachingClassIds).stream()
                .collect(Collectors.groupingBy(TeachingClassScheduleDO::getTeachingClassId));
    }

    /**
     * 转换排课出参，并补齐教室信息
     *
     * @param scheduleDOList 排课数据对象集合
     * @return 排课出参集合
     */
    private List<ScheduleRespDTO> convertSchedule(List<TeachingClassScheduleDO> scheduleDOList) {
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

    /**
     * 批量转换教学班出参
     *
     * @param teachingClassDOList 教学班数据对象集合
     * @return 教学班出参集合
     */
    private List<TeachingClassRespDTO> convertToRespDTO(List<TeachingClassDO> teachingClassDOList) {
        if (CollUtil.isEmpty(teachingClassDOList)) {
            return List.of();
        }
        Map<Long, CourseDO> courseMap = buildCourseMap(teachingClassDOList);
        Map<Long, List<TeachingClassScheduleDO>> scheduleMap = buildScheduleMap(
                teachingClassDOList.stream().map(TeachingClassDO::getId).toList());
        return teachingClassDOList.stream().map(each -> {
            TeachingClassRespDTO result = BeanUtil.copyProperties(each, TeachingClassRespDTO.class);
            result.setTeachingClassId(each.getId());
            CourseDO courseDO = courseMap.get(each.getCourseId());
            result.setCourseName(courseDO == null ? null : courseDO.getCourseName());
            result.setStatus(each.getStatus() == null ? null : each.getStatus().name());
            result.setSelectedCount(courseSelectionMapper.countSelectedByTeachingClassId(each.getId()));
            result.setSchedules(convertSchedule(scheduleMap.getOrDefault(each.getId(), List.of())));
            return result;
        }).toList();
    }
}
