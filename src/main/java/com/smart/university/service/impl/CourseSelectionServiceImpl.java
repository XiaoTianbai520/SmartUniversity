package com.smart.university.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.common.util.RedisKeyUtil;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.SelectionBatchClassDO;
import com.smart.university.domain.entity.SelectionBatchDO;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.entity.TeachingClassGradeDO;
import com.smart.university.domain.entity.TeachingClassMajorDO;
import com.smart.university.domain.entity.TeachingClassScheduleDO;
import com.smart.university.domain.dto.req.CourseSelectReqDTO;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.dto.resp.CourseSelectionRespDTO;
import com.smart.university.domain.dto.resp.ScheduleRespDTO;
import com.smart.university.domain.dto.resp.StudentSelectionRespDTO;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.mapper.CourseMapper;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.SelectionBatchClassMapper;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.mapper.TeachingClassGradeMapper;
import com.smart.university.mapper.TeachingClassMajorMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.mapper.TeachingClassScheduleMapper;
import com.smart.university.mq.message.CourseSelectionMessage;
import com.smart.university.mq.producer.CourseSelectionProducer;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.university.service.CourseSelectionService;
import com.smart.university.service.ScheduleService;
import com.smart.university.service.SelectionBatchService;
import com.smart.university.service.SemesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学生选课服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSelectionServiceImpl extends ServiceImpl<CourseSelectionMapper, CourseSelectionDO> implements CourseSelectionService {

    private final CourseSelectionMapper courseSelectionMapper;

    private final TeachingClassMapper teachingClassMapper;

    private final TeachingClassMajorMapper teachingClassMajorMapper;

    private final TeachingClassGradeMapper teachingClassGradeMapper;

    private final TeachingClassScheduleMapper teachingClassScheduleMapper;

    private final SelectionBatchClassMapper selectionBatchClassMapper;

    private final CourseMapper courseMapper;

    private final StudentMapper studentMapper;

    private final SemesterService semesterService;

    private final SelectionBatchService selectionBatchService;

    private final ScheduleService scheduleService;

    /**
     * 选课消息生产者，未开启消息队列时容器中不存在该 Bean
     */
    private final ObjectProvider<CourseSelectionProducer> courseSelectionProducerProvider;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 是否开启 Redis 容量预扣减，关闭时退化为纯数据库事务
     */
    @Value("${smart-university.selection.redis-deduct-enabled}")
    private boolean redisDeductEnabled;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseSelectionRespDTO selectCourse(CourseSelectReqDTO requestParam) {
        StudentDO studentDO = getStudentByUserId();
        TeachingClassDO teachingClassDO = getTeachingClass(requestParam.getTeachingClassId());
        Long semesterId = semesterService.getCurrentSemester().getId();
        if (!semesterId.equals(teachingClassDO.getSemesterId())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "教学班不属于当前学期");
        }
        SelectionBatchDO batchDO = selectionBatchService.getCurrentSelectionBatch(semesterId);
        checkBatchTeachingClass(batchDO.getId(), teachingClassDO.getId());
        if (!teachingClassDO.getStatus().allowSelect()) {
            throw new BizException(ResultCodeEnum.NOT_IN_SELECTION_TIME);
        }
        checkMajorAndGrade(studentDO, teachingClassDO);

        CourseSelectionDO existSelectionDO = getExistSelection(studentDO.getId(), teachingClassDO.getId());
        if (existSelectionDO != null && SelectionStatusEnum.SELECTED == existSelectionDO.getStatus()) {
            throw new BizException(ResultCodeEnum.DUPLICATE_SELECTION);
        }
        checkScheduleConflict(studentDO.getId(), teachingClassDO.getId());
        checkCreditLimit(studentDO.getId(), semesterId, teachingClassDO.getCourseId());
        deductCapacity(teachingClassDO.getId(), teachingClassDO.getCapacity());

        LocalDateTime now = LocalDateTime.now();
        Long selectionId;
        if (existSelectionDO == null) {
            CourseSelectionDO selectionDO = new CourseSelectionDO();
            selectionDO.setStudentId(studentDO.getId());
            selectionDO.setTeachingClassId(teachingClassDO.getId());
            selectionDO.setBatchId(batchDO.getId());
            selectionDO.setStatus(SelectionStatusEnum.SELECTED);
            selectionDO.setSelectedAt(now);
            courseSelectionMapper.saveSelection(selectionDO);
            selectionId = selectionDO.getId();
        } else {
            CourseSelectionDO updateSelectionDO = new CourseSelectionDO();
            updateSelectionDO.setId(existSelectionDO.getId());
            updateSelectionDO.setBatchId(batchDO.getId());
            updateSelectionDO.setStatus(SelectionStatusEnum.SELECTED);
            updateSelectionDO.setSelectedAt(now);
            courseSelectionMapper.updateSelection(updateSelectionDO);
            selectionId = existSelectionDO.getId();
        }
        sendSelectionMessage(selectionId, studentDO.getId(), teachingClassDO.getId(), batchDO.getId(), "SELECT", now);

        CourseSelectionRespDTO result = new CourseSelectionRespDTO();
        result.setSelectionId(selectionId);
        result.setTeachingClassId(teachingClassDO.getId());
        result.setStatus(SelectionStatusEnum.SELECTED.name());
        result.setSelectedAt(now);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawCourse(Long selectionId) {
        StudentDO studentDO = getStudentByUserId();
        CourseSelectionDO selectionDO = courseSelectionMapper.getSelectionById(selectionId);
        if (selectionDO == null || !selectionDO.getStudentId().equals(studentDO.getId())) {
            throw new BizException(ResultCodeEnum.SELECTION_NOT_EXIST);
        }
        if (SelectionStatusEnum.SELECTED != selectionDO.getStatus()) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "当前选课记录不是已选状态");
        }
        SelectionBatchDO batchDO = selectionBatchService.getSelectionBatchById(selectionDO.getBatchId());
        LocalDateTime now = LocalDateTime.now();
        if (batchDO.getDropDeadline() == null || batchDO.getDropDeadline().isBefore(now)) {
            throw new BizException(ResultCodeEnum.WITHDRAW_NOT_ALLOWED);
        }
        CourseSelectionDO updateSelectionDO = new CourseSelectionDO();
        updateSelectionDO.setId(selectionId);
        updateSelectionDO.setStatus(SelectionStatusEnum.WITHDRAWN);
        updateSelectionDO.setWithdrawnAt(now);
        courseSelectionMapper.updateSelection(updateSelectionDO);
        restoreCapacity(selectionDO.getTeachingClassId());
        sendSelectionMessage(selectionId, studentDO.getId(), selectionDO.getTeachingClassId(),
                selectionDO.getBatchId(), "WITHDRAW", now);
    }

    @Override
    public List<StudentSelectionRespDTO> listStudentSelection(StudentSelectionQueryReqDTO requestParam) {
        StudentDO studentDO = getStudentByUserId();
        requestParam.setStudentId(studentDO.getId());
        if (requestParam.getSemesterId() == null) {
            requestParam.setSemesterId(semesterService.getCurrentSemester().getId());
        }
        List<CourseSelectionDO> selectionDOList = courseSelectionMapper.listSelectionByStudent(requestParam);
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
        Map<Long, List<TeachingClassScheduleDO>> scheduleMap = scheduleService
                .listScheduleByTeachingClassIds(teachingClassIds).stream()
                .collect(Collectors.groupingBy(TeachingClassScheduleDO::getTeachingClassId));
        return selectionDOList.stream().map(each -> {
            TeachingClassDO teachingClassDO = teachingClassMap.get(each.getTeachingClassId());
            CourseDO courseDO = teachingClassDO == null ? null : courseMap.get(teachingClassDO.getCourseId());
            StudentSelectionRespDTO result = new StudentSelectionRespDTO();
            result.setSelectionId(each.getId());
            result.setTeachingClassId(each.getTeachingClassId());
            result.setCourseName(courseDO == null ? null : courseDO.getCourseName());
            result.setCredit(courseDO == null ? null : courseDO.getCredit());
            result.setStatus(each.getStatus() == null ? null : each.getStatus().name());
            result.setSelectedAt(each.getSelectedAt());
            result.setSchedules(convertSchedule(scheduleMap.getOrDefault(each.getTeachingClassId(), List.of())));
            return result;
        }).toList();
    }

    /**
     * 获取当前登录学生
     *
     * @return 学生数据对象
     */
    private StudentDO getStudentByUserId() {
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

    private TeachingClassDO getTeachingClass(Long teachingClassId) {
        TeachingClassDO result = teachingClassMapper.getTeachingClassById(teachingClassId);
        if (result == null) {
            throw new BizException(ResultCodeEnum.TEACHING_CLASS_NOT_EXIST);
        }
        return result;
    }

    /**
     * 校验教学班是否在当前批次开放范围内
     *
     * @param batchId         批次 ID
     * @param teachingClassId 教学班 ID
     */
    private void checkBatchTeachingClass(Long batchId, Long teachingClassId) {
        SelectionBatchClassDO param = new SelectionBatchClassDO();
        param.setBatchId(batchId);
        param.setTeachingClassId(teachingClassId);
        if (selectionBatchClassMapper.countSelectionBatchClass(param) <= 0) {
            throw new BizException(ResultCodeEnum.NOT_IN_SELECTION_TIME);
        }
    }

    /**
     * 校验学生专业与年级是否符合教学班开放条件
     *
     * @param studentDO       学生信息
     * @param teachingClassDO 教学班信息
     */
    private void checkMajorAndGrade(StudentDO studentDO, TeachingClassDO teachingClassDO) {
        TeachingClassMajorDO majorParam = new TeachingClassMajorDO();
        majorParam.setTeachingClassId(teachingClassDO.getId());
        majorParam.setMajorId(studentDO.getMajorId());
        if (teachingClassMajorMapper.countTeachingClassMajor(majorParam) <= 0) {
            throw new BizException(ResultCodeEnum.MAJOR_NOT_ALLOWED);
        }
        TeachingClassGradeDO gradeParam = new TeachingClassGradeDO();
        gradeParam.setTeachingClassId(teachingClassDO.getId());
        gradeParam.setGradeId(studentDO.getGradeId());
        if (teachingClassGradeMapper.countTeachingClassGrade(gradeParam) <= 0) {
            throw new BizException(ResultCodeEnum.GRADE_NOT_ALLOWED);
        }
    }

    /**
     * 查询学生在该教学班的历史选课记录
     *
     * @param studentId       学生 ID
     * @param teachingClassId 教学班 ID
     * @return 选课记录，不存在返回 null
     */
    private CourseSelectionDO getExistSelection(Long studentId, Long teachingClassId) {
        CourseSelectionDO param = new CourseSelectionDO();
        param.setStudentId(studentId);
        param.setTeachingClassId(teachingClassId);
        return courseSelectionMapper.getSelectionByStudentAndClass(param);
    }

    /**
     * 校验学生已选课程与目标课程是否存在时间冲突
     *
     * @param studentId       学生 ID
     * @param teachingClassId 目标教学班 ID
     */
    private void checkScheduleConflict(Long studentId, Long teachingClassId) {
        List<TeachingClassScheduleDO> targetSchedules =
                teachingClassScheduleMapper.listScheduleByTeachingClassId(teachingClassId);
        if (CollUtil.isEmpty(targetSchedules)) {
            return;
        }
        StudentSelectionQueryReqDTO queryParam = new StudentSelectionQueryReqDTO();
        queryParam.setStudentId(studentId);
        queryParam.setSemesterId(semesterService.getCurrentSemester().getId());
        queryParam.setStatus(SelectionStatusEnum.SELECTED.name());
        List<Long> selectedClassIds = courseSelectionMapper.listSelectionByStudent(queryParam).stream()
                .map(CourseSelectionDO::getTeachingClassId).toList();
        if (CollUtil.isEmpty(selectedClassIds)) {
            return;
        }
        List<TeachingClassScheduleDO> selectedSchedules =
                teachingClassScheduleMapper.listScheduleByTeachingClassIds(selectedClassIds);
        for (TeachingClassScheduleDO targetEach : targetSchedules) {
            for (TeachingClassScheduleDO selectedEach : selectedSchedules) {
                if (isConflict(targetEach, selectedEach)) {
                    throw new BizException(ResultCodeEnum.STUDENT_SCHEDULE_CONFLICT);
                }
            }
        }
    }

    /**
     * 判断两段排课是否冲突：星期相同且教学周、节次均有交集
     *
     * @param first  第一段排课
     * @param second 第二段排课
     * @return 冲突返回 true
     */
    private boolean isConflict(TeachingClassScheduleDO first, TeachingClassScheduleDO second) {
        if (!first.getWeekday().equals(second.getWeekday())) {
            return false;
        }
        boolean weekOverlap = first.getStartWeek() <= second.getEndWeek()
                && first.getEndWeek() >= second.getStartWeek();
        boolean sectionOverlap = first.getStartSection() <= second.getEndSection()
                && first.getEndSection() >= second.getStartSection();
        return weekOverlap && sectionOverlap;
    }

    /**
     * 校验本学期学分上限
     *
     * @param studentId   学生 ID
     * @param semesterId  学期 ID
     * @param targetCourseId 目标课程 ID
     */
    private void checkCreditLimit(Long studentId, Long semesterId, Long targetCourseId) {
        BigDecimal maxCredit = semesterService.getSemesterById(semesterId).getMaxSelectionCredit();
        BigDecimal currentCredit = sumSelectedCredit(studentId, semesterId);
        CourseDO targetCourseDO = courseMapper.getCourseById(targetCourseId);
        BigDecimal targetCredit = targetCourseDO == null ? BigDecimal.ZERO : targetCourseDO.getCredit();
        if (currentCredit.add(targetCredit).compareTo(maxCredit) > 0) {
            throw new BizException(ResultCodeEnum.CREDIT_LIMIT_EXCEEDED);
        }
    }

    /**
     * 统计学生本学期已选学分
     *
     * @param studentId  学生 ID
     * @param semesterId 学期 ID
     * @return 已选学分
     */
    private BigDecimal sumSelectedCredit(Long studentId, Long semesterId) {
        StudentSelectionQueryReqDTO queryParam = new StudentSelectionQueryReqDTO();
        queryParam.setStudentId(studentId);
        queryParam.setSemesterId(semesterId);
        queryParam.setStatus(SelectionStatusEnum.SELECTED.name());
        List<CourseSelectionDO> selectionDOList = courseSelectionMapper.listSelectionByStudent(queryParam);
        if (CollUtil.isEmpty(selectionDOList)) {
            return BigDecimal.ZERO;
        }
        List<Long> teachingClassIds = selectionDOList.stream()
                .map(CourseSelectionDO::getTeachingClassId).distinct().toList();
        List<TeachingClassDO> teachingClassDOList = teachingClassMapper.listTeachingClassByIds(teachingClassIds);
        List<Long> courseIds = teachingClassDOList.stream().map(TeachingClassDO::getCourseId).distinct().toList();
        return courseMapper.listCourseByIds(courseIds).stream()
                .map(CourseDO::getCredit)
                .filter(each -> each != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 扣减教学班容量，开启 Redis 时走原子自增预扣减，否则以数据库实时统计为准
     *
     * @param teachingClassId 教学班 ID
     * @param capacity        教学班容量
     */
    private void deductCapacity(Long teachingClassId, Integer capacity) {
        long selectedCount = courseSelectionMapper.countSelectedByTeachingClassId(teachingClassId);
        if (!redisDeductEnabled) {
            if (selectedCount >= capacity) {
                throw new BizException(ResultCodeEnum.CLASS_CAPACITY_FULL);
            }
            return;
        }
        String key = RedisKeyUtil.buildTeachingClassCapacityKey(teachingClassId);
        stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(selectedCount));
        long afterIncrement = stringRedisTemplate.opsForValue().increment(key);
        if (afterIncrement > capacity) {
            stringRedisTemplate.opsForValue().decrement(key);
            throw new BizException(ResultCodeEnum.CLASS_CAPACITY_FULL);
        }
        if (afterIncrement > selectedCount + 1) {
            // Redis 计数与数据库不一致时以数据库为准，避免缓存漂移导致误判满员
            stringRedisTemplate.opsForValue().set(key, String.valueOf(selectedCount + 1));
        }
    }

    /**
     * 退课后回补教学班容量
     *
     * @param teachingClassId 教学班 ID
     */
    private void restoreCapacity(Long teachingClassId) {
        String key = RedisKeyUtil.buildTeachingClassCapacityKey(teachingClassId);
        if (redisDeductEnabled && Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            stringRedisTemplate.opsForValue().decrement(key);
        }
    }

    /**
     * 发送选课结果消息
     *
     * @param selectionId     选课记录 ID
     * @param studentId       学生 ID
     * @param teachingClassId 教学班 ID
     * @param batchId         批次 ID
     * @param operateType     操作类型
     * @param operateTime     操作时间
     */
    private void sendSelectionMessage(Long selectionId, Long studentId, Long teachingClassId, Long batchId,
                                      String operateType, LocalDateTime operateTime) {
        CourseSelectionMessage message = CourseSelectionMessage.builder()
                .selectionId(selectionId)
                .studentId(studentId)
                .teachingClassId(teachingClassId)
                .batchId(batchId)
                .operateType(operateType)
                .operateTime(operateTime)
                .build();
        CourseSelectionProducer producer = courseSelectionProducerProvider.getIfAvailable();
        if (producer == null) {
            return;
        }
        try {
            if ("WITHDRAW".equals(operateType)) {
                producer.sendCourseWithdrawMessage(message);
            } else {
                producer.sendCourseSelectionMessage(message);
            }
        } catch (Exception ex) {
            // 选课主流程已落库，消息发送失败仅记录日志，由消费端兜底刷新缓存
            log.error("选课消息发送失败，selectionId：{}", selectionId, ex);
        }
    }

    private List<ScheduleRespDTO> convertSchedule(List<TeachingClassScheduleDO> scheduleDOList) {
        return scheduleDOList.stream().map(each -> {
            ScheduleRespDTO result = new ScheduleRespDTO();
            result.setWeekday(each.getWeekday());
            result.setStartSection(each.getStartSection());
            result.setEndSection(each.getEndSection());
            result.setStartWeek(each.getStartWeek());
            result.setEndWeek(each.getEndWeek());
            return result;
        }).toList();
    }
}
