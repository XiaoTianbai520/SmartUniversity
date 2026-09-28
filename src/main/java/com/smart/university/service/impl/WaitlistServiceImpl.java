package com.smart.university.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.university.common.base.PageResult;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.common.util.RedisKeyUtil;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.dto.req.WaitlistPageQueryReqDTO;
import com.smart.university.domain.dto.resp.WaitlistPromoteRespDTO;
import com.smart.university.domain.dto.resp.WaitlistRespDTO;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.SelectionBatchClassDO;
import com.smart.university.domain.entity.SelectionBatchDO;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.entity.TeacherDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.entity.TeachingClassGradeDO;
import com.smart.university.domain.entity.TeachingClassMajorDO;
import com.smart.university.domain.entity.TeachingClassScheduleDO;
import com.smart.university.domain.enums.NoticeTypeEnum;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.domain.event.NoticeEvent;
import com.smart.university.domain.event.NoticeEventPublisher;
import com.smart.university.domain.event.NoticeTargetResolver;
import com.smart.university.mapper.CourseMapper;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.SelectionBatchClassMapper;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.mapper.TeacherMapper;
import com.smart.university.mapper.TeachingClassGradeMapper;
import com.smart.university.mapper.TeachingClassMajorMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.mapper.TeachingClassScheduleMapper;
import com.smart.university.service.SelectionBatchService;
import com.smart.university.service.SemesterService;
import com.smart.university.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 候补选课服务实现，候补记录复用 course_selection 表并通过 status 区分
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaitlistServiceImpl implements WaitlistService {

    private final CourseSelectionMapper courseSelectionMapper;

    private final TeachingClassMapper teachingClassMapper;

    private final TeachingClassMajorMapper teachingClassMajorMapper;

    private final TeachingClassGradeMapper teachingClassGradeMapper;

    private final TeachingClassScheduleMapper teachingClassScheduleMapper;

    private final SelectionBatchClassMapper selectionBatchClassMapper;

    private final CourseMapper courseMapper;

    private final StudentMapper studentMapper;

    private final TeacherMapper teacherMapper;

    private final SemesterService semesterService;

    private final SelectionBatchService selectionBatchService;

    private final StringRedisTemplate stringRedisTemplate;

    private final NoticeEventPublisher noticeEventPublisher;

    private final NoticeTargetResolver noticeTargetResolver;

    /**
     * 是否开启 Redis 容量预扣减，与选课主流程共用同一开关，关闭时以数据库实时统计为准
     */
    @Value("${smart-university.selection.redis-deduct-enabled}")
    private boolean redisDeductEnabled;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaitlistRespDTO joinWaitlist(Long teachingClassId) {
        StudentDO studentDO = getCurrentStudent();
        TeachingClassDO teachingClassDO = getTeachingClass(teachingClassId);
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
        if (existSelectionDO != null) {
            if (SelectionStatusEnum.SELECTED == existSelectionDO.getStatus()) {
                throw new BizException(ResultCodeEnum.DUPLICATE_SELECTION);
            }
            if (SelectionStatusEnum.WAITING == existSelectionDO.getStatus()) {
                throw new BizException(ResultCodeEnum.WAITLIST_DUPLICATE);
            }
        }
        checkScheduleConflict(studentDO.getId(), teachingClassDO.getId());
        checkCreditLimit(studentDO.getId(), semesterId, teachingClassDO.getCourseId());
        // 候补只在名额已满时开放，仍有余量时应直接选课
        if (courseSelectionMapper.countSelectedByTeachingClassId(teachingClassDO.getId()) < getCapacity(teachingClassDO)) {
            throw new BizException(ResultCodeEnum.WAITLIST_CAPACITY_AVAILABLE);
        }

        Integer waitlistNo = getNextWaitlistNo(teachingClassDO.getId());
        LocalDateTime now = LocalDateTime.now();
        Long selectionId;
        if (existSelectionDO == null) {
            CourseSelectionDO selectionDO = new CourseSelectionDO();
            selectionDO.setStudentId(studentDO.getId());
            selectionDO.setTeachingClassId(teachingClassDO.getId());
            selectionDO.setBatchId(batchDO.getId());
            selectionDO.setStatus(SelectionStatusEnum.WAITING);
            selectionDO.setWaitlistNo(waitlistNo);
            selectionDO.setSelectedAt(now);
            courseSelectionMapper.saveSelection(selectionDO);
            selectionId = selectionDO.getId();
        } else {
            // 同一学生同一教学班只有一条记录，退课后重新候补时恢复为候补状态
            CourseSelectionDO updateSelectionDO = new CourseSelectionDO();
            updateSelectionDO.setId(existSelectionDO.getId());
            updateSelectionDO.setBatchId(batchDO.getId());
            updateSelectionDO.setStatus(SelectionStatusEnum.WAITING);
            updateSelectionDO.setWaitlistNo(waitlistNo);
            updateSelectionDO.setSelectedAt(now);
            courseSelectionMapper.updateSelection(updateSelectionDO);
            selectionId = existSelectionDO.getId();
        }

        CourseSelectionDO waitlistSelectionDO = new CourseSelectionDO();
        waitlistSelectionDO.setId(selectionId);
        waitlistSelectionDO.setStudentId(studentDO.getId());
        waitlistSelectionDO.setTeachingClassId(teachingClassDO.getId());
        waitlistSelectionDO.setStatus(SelectionStatusEnum.WAITING);
        waitlistSelectionDO.setWaitlistNo(waitlistNo);
        waitlistSelectionDO.setSelectedAt(now);
        return convertToWaitlistRespDTO(waitlistSelectionDO, teachingClassDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelWaitlist(Long selectionId) {
        StudentDO studentDO = getCurrentStudent();
        CourseSelectionDO selectionDO = courseSelectionMapper.getSelectionById(selectionId);
        if (selectionDO == null || !studentDO.getId().equals(selectionDO.getStudentId())) {
            throw new BizException(ResultCodeEnum.WAITLIST_NOT_EXIST);
        }
        if (SelectionStatusEnum.WAITING != selectionDO.getStatus()) {
            throw new BizException(ResultCodeEnum.WAITLIST_NOT_ALLOWED);
        }
        checkInSelectionWindow(selectionDO.getBatchId());
        LocalDateTime now = LocalDateTime.now();
        CourseSelectionDO updateSelectionDO = new CourseSelectionDO();
        updateSelectionDO.setId(selectionId);
        updateSelectionDO.setStatus(SelectionStatusEnum.WITHDRAWN);
        updateSelectionDO.setWithdrawnAt(now);
        // 候补序号保留不清零，其余候补同学的位次通过实时计算自动前移
        courseSelectionMapper.updateSelection(updateSelectionDO);
    }

    @Override
    public PageResult<WaitlistRespDTO> pageWaitlist(WaitlistPageQueryReqDTO requestParam) {
        StudentDO studentDO = getCurrentStudent();
        Long semesterId = requestParam.getSemesterId() == null
                ? semesterService.getCurrentSemester().getId()
                : requestParam.getSemesterId();
        StudentSelectionQueryReqDTO queryParam = new StudentSelectionQueryReqDTO();
        queryParam.setStudentId(studentDO.getId());
        queryParam.setSemesterId(semesterId);
        Page<CourseSelectionDO> page = Page.of(requestParam.getCurrentPage(), requestParam.getLimit());
        IPage<CourseSelectionDO> pageResult = courseSelectionMapper.listWaitlistByStudent(page, queryParam);
        List<CourseSelectionDO> selectionDOList = pageResult.getRecords();
        if (CollUtil.isEmpty(selectionDOList)) {
            return PageResult.empty(requestParam.getCurrentPage(), requestParam.getLimit());
        }
        List<Long> teachingClassIds = selectionDOList.stream()
                .map(CourseSelectionDO::getTeachingClassId).distinct().toList();
        List<TeachingClassDO> teachingClassDOList = teachingClassMapper.listTeachingClassByIds(teachingClassIds);
        List<WaitlistRespDTO> records = new ArrayList<>(selectionDOList.size());
        for (CourseSelectionDO each : selectionDOList) {
            TeachingClassDO teachingClassDO = teachingClassDOList.stream()
                    .filter(item -> item.getId().equals(each.getTeachingClassId()))
                    .findFirst().orElse(null);
            records.add(convertToWaitlistRespDTO(each, teachingClassDO));
        }
        return new PageResult<>(records, requestParam.getCurrentPage(), requestParam.getLimit(), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaitlistPromoteRespDTO tryPromote(Long teachingClassId) {
        TeachingClassDO teachingClassDO = teachingClassMapper.getTeachingClassById(teachingClassId);
        if (teachingClassDO == null) {
            throw new BizException(ResultCodeEnum.TEACHING_CLASS_NOT_EXIST);
        }
        Long semesterId = teachingClassDO.getSemesterId();
        int capacity = getCapacity(teachingClassDO);
        WaitlistPromoteRespDTO result = new WaitlistPromoteRespDTO();
        result.setTeachingClassId(teachingClassId);
        result.setPromotedCount(0);
        result.setInvalidCount(0);

        while (courseSelectionMapper.countSelectedByTeachingClassId(teachingClassId) < capacity) {
            List<CourseSelectionDO> waitingList = courseSelectionMapper.listWaitingByTeachingClassId(teachingClassId);
            if (CollUtil.isEmpty(waitingList)) {
                break;
            }
            boolean promotedInRound = false;
            // 单轮只遍历当前候补队列一次，队列无可用候补时终止，保证循环必然收敛
            for (CourseSelectionDO each : waitingList) {
                if (courseSelectionMapper.countSelectedByTeachingClassId(teachingClassId) >= capacity) {
                    break;
                }
                try {
                    StudentDO promotedStudentDO = promoteWaitlistSelection(each, teachingClassDO, semesterId);
                    result.setPromotedCount(result.getPromotedCount() + 1);
                    if (promotedStudentDO != null && StrUtil.isNotBlank(promotedStudentDO.getStudentNo())) {
                        result.getPromotedStudentNos().add(promotedStudentDO.getStudentNo());
                    }
                    promotedInRound = true;
                } catch (BizException ex) {
                    invalidateWaitlist(each.getId());
                    result.setInvalidCount(result.getInvalidCount() + 1);
                    log.info("候补记录重新校验未通过，已置为失效，selectionId：{}，原因：{}", each.getId(), ex.getMessage());
                }
            }
            if (!promotedInRound) {
                break;
            }
        }
        return result;
    }

    /**
     * 递补单条候补记录，校验不通过时抛出业务异常由调用方作废该候补
     *
     * @param selectionDO     候补选课记录
     * @param teachingClassDO 教学班信息
     * @param semesterId      学期 ID
     * @return 被递补的学生信息
     */
    private StudentDO promoteWaitlistSelection(CourseSelectionDO selectionDO, TeachingClassDO teachingClassDO,
                                               Long semesterId) {
        StudentDO studentDO = studentMapper.getStudentById(selectionDO.getStudentId());
        if (studentDO == null) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_EXIST);
        }
        checkPromoteWindow(selectionDO.getBatchId(), teachingClassDO);
        checkMajorAndGrade(studentDO, teachingClassDO);
        checkScheduleConflict(studentDO.getId(), teachingClassDO.getId());
        checkCreditLimit(studentDO.getId(), semesterId, teachingClassDO.getCourseId());
        deductCapacity(teachingClassDO.getId(), getCapacity(teachingClassDO));

        LocalDateTime now = LocalDateTime.now();
        CourseSelectionDO updateSelectionDO = new CourseSelectionDO();
        updateSelectionDO.setId(selectionDO.getId());
        updateSelectionDO.setStatus(SelectionStatusEnum.SELECTED);
        updateSelectionDO.setSelectedAt(now);
        updateSelectionDO.setPromotedAt(now);
        courseSelectionMapper.updateSelection(updateSelectionDO);
        publishPromoteNotice(selectionDO.getId(), teachingClassDO);
        return studentDO;
    }

    /**
     * 作废失效的候补记录，保留候补序号便于教务追溯
     *
     * @param selectionId 选课记录 ID
     */
    private void invalidateWaitlist(Long selectionId) {
        CourseSelectionDO updateSelectionDO = new CourseSelectionDO();
        updateSelectionDO.setId(selectionId);
        updateSelectionDO.setStatus(SelectionStatusEnum.WITHDRAWN);
        updateSelectionDO.setWithdrawnAt(LocalDateTime.now());
        courseSelectionMapper.updateSelection(updateSelectionDO);
    }

    /**
     * 获取下一个候补序号，取该教学班当前最大序号加一，无候补时从 1 开始
     *
     * @param teachingClassId 教学班 ID
     * @return 候补序号
     */
    private Integer getNextWaitlistNo(Long teachingClassId) {
        Integer maxWaitlistNo = courseSelectionMapper.getMaxWaitlistNo(teachingClassId);
        return maxWaitlistNo == null ? 1 : maxWaitlistNo + 1;
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
     * 查询教学班，不存在时抛出业务异常
     *
     * @param teachingClassId 教学班 ID
     * @return 教学班数据对象
     */
    private TeachingClassDO getTeachingClass(Long teachingClassId) {
        TeachingClassDO result = teachingClassMapper.getTeachingClassById(teachingClassId);
        if (result == null) {
            throw new BizException(ResultCodeEnum.TEACHING_CLASS_NOT_EXIST);
        }
        return result;
    }

    /**
     * 获取教学班容量，容量缺失时按 0 处理
     *
     * @param teachingClassDO 教学班信息
     * @return 容量
     */
    private int getCapacity(TeachingClassDO teachingClassDO) {
        return teachingClassDO.getCapacity() == null ? 0 : teachingClassDO.getCapacity();
    }

    /**
     * 校验教学班是否在指定批次开放范围内
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
     * 校验学生已选课程与目标教学班是否存在时间冲突
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
     * @param studentId      学生 ID
     * @param semesterId     学期 ID
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
     * 校验候补所属批次是否仍处于可选择时间窗内，用于取消候补
     *
     * @param batchId 批次 ID
     */
    private void checkInSelectionWindow(Long batchId) {
        if (batchId == null) {
            throw new BizException(ResultCodeEnum.NOT_IN_SELECTION_TIME);
        }
        SelectionBatchDO batchDO = selectionBatchService.getSelectionBatchById(batchId);
        LocalDateTime now = LocalDateTime.now();
        if (batchDO.getStartTime() == null || batchDO.getEndTime() == null
                || now.isBefore(batchDO.getStartTime()) || now.isAfter(batchDO.getEndTime())) {
            throw new BizException(ResultCodeEnum.NOT_IN_SELECTION_TIME);
        }
    }

    /**
     * 递补前重新校验批次时间窗、批次开放范围与教学班状态
     *
     * @param batchId         候补记录所属批次 ID
     * @param teachingClassDO 教学班信息
     */
    private void checkPromoteWindow(Long batchId, TeachingClassDO teachingClassDO) {
        // 退课截止时间可能晚于选课结束时间，退课时以退课截止时间为准，
        // 而递补必须在选课批次时间窗内完成，批次结束后未递补的候补记录自动失效
        checkInSelectionWindow(batchId);
        if (!teachingClassDO.getStatus().allowSelect()) {
            throw new BizException(ResultCodeEnum.NOT_IN_SELECTION_TIME);
        }
        checkBatchTeachingClass(batchId, teachingClassDO.getId());
    }

    /**
     * 扣减教学班容量，与选课主流程保持一致：开启 Redis 时走原子自增，否则以数据库实时统计为准
     *
     * @param teachingClassId 教学班 ID
     * @param capacity        教学班容量
     */
    private void deductCapacity(Long teachingClassId, Integer capacity) {
        if (!redisDeductEnabled) {
            return;
        }
        long selectedCount = courseSelectionMapper.countSelectedByTeachingClassId(teachingClassId);
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
     * 发布候补递补成功通知，通知失败不影响递补结果
     *
     * @param selectionId     选课记录 ID
     * @param teachingClassDO 教学班信息
     */
    private void publishPromoteNotice(Long selectionId, TeachingClassDO teachingClassDO) {
        try {
            CourseDO courseDO = courseMapper.getCourseById(teachingClassDO.getCourseId());
            String courseName = courseDO == null ? "" : courseDO.getCourseName();
            String content = StrUtil.format("您已由候补递补为课程【{}】教学班【{}】的正式选课，请及时查看课表。",
                    courseName, teachingClassDO.getClassName());
            noticeEventPublisher.publish(NoticeEvent.builder()
                    .noticeType(NoticeTypeEnum.WAITLIST_PROMOTED)
                    .title(NoticeTypeEnum.WAITLIST_PROMOTED.getDefaultTitle())
                    .content(content)
                    .receiverUserIds(noticeTargetResolver.listUserIdsBySelectionIds(List.of(selectionId)))
                    .receiverRole(RoleEnum.STUDENT)
                    .bizId(selectionId)
                    .build());
        } catch (Exception ex) {
            log.error("候补递补通知发布失败，selectionId：{}", selectionId, ex);
        }
    }

    /**
     * 将选课记录转换为候补视图，位次按同教学班内候补序号实时计算
     *
     * @param selectionDO     选课记录
     * @param teachingClassDO 教学班信息，为空时只返回候补基础字段
     * @return 候补视图
     */
    private WaitlistRespDTO convertToWaitlistRespDTO(CourseSelectionDO selectionDO, TeachingClassDO teachingClassDO) {
        WaitlistRespDTO result = new WaitlistRespDTO();
        result.setSelectionId(selectionDO.getId());
        result.setTeachingClassId(selectionDO.getTeachingClassId());
        result.setStatus(selectionDO.getStatus() == null ? null : selectionDO.getStatus().name());
        result.setWaitlistNo(selectionDO.getWaitlistNo());
        result.setWaitingAt(selectionDO.getSelectedAt());
        if (teachingClassDO == null) {
            return result;
        }
        result.setTeachingClassName(teachingClassDO.getClassName());
        result.setCapacity(teachingClassDO.getCapacity());
        result.setSelectedCount((int) courseSelectionMapper.countSelectedByTeachingClassId(teachingClassDO.getId()));
        result.setWaitingCount((int) courseSelectionMapper.countWaitingByTeachingClassId(teachingClassDO.getId()));
        result.setWaitlistRank((int) (courseSelectionMapper
                .countWaitingAhead(teachingClassDO.getId(), selectionDO.getWaitlistNo()) + 1));
        CourseDO courseDO = courseMapper.getCourseById(teachingClassDO.getCourseId());
        if (courseDO != null) {
            result.setCourseName(courseDO.getCourseName());
            result.setCredit(courseDO.getCredit());
        }
        if (teachingClassDO.getTeacherId() != null) {
            TeacherDO teacherDO = teacherMapper.getTeacherById(teachingClassDO.getTeacherId());
            if (teacherDO != null) {
                result.setTeacherName(teacherDO.getTeacherName());
            }
        }
        return result;
    }
}
