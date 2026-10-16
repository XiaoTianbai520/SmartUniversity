package com.smart.university.service.impl;

import cn.hutool.core.util.StrUtil;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.dto.req.TeacherWorkloadQueryReqDTO;
import com.smart.university.domain.dto.resp.TeacherWorkloadClassRespDTO;
import com.smart.university.domain.dto.resp.TeacherWorkloadDetailRespDTO;
import com.smart.university.domain.dto.resp.TeacherWorkloadExtraRespDTO;
import com.smart.university.domain.dto.resp.TeacherWorkloadPageRespDTO;
import com.smart.university.domain.dto.resp.TeacherWorkloadRespDTO;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.entity.StatTeacherWorkloadDO;
import com.smart.university.domain.entity.TeacherDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.entity.TeachingClassScheduleDO;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.domain.enums.TeachingClassStatusEnum;
import com.smart.university.mapper.CourseMapper;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.StatTeacherWorkloadMapper;
import com.smart.university.mapper.TeacherMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.mapper.TeachingClassScheduleMapper;
import com.smart.university.service.SemesterService;
import com.smart.university.service.TeacherWorkloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 教师工作量统计服务实现（S-04）
 *
 * <p>课时口径（Q8 决议 A）：1 节课 = 1 课时，即 `节次数 × 周数`，不引入课程系数；
 * 配置项 `credit-factor-enabled` 打开时按课程学分加权。
 */
@Service
@RequiredArgsConstructor
public class TeacherWorkloadServiceImpl implements TeacherWorkloadService {

    private final TeachingClassMapper teachingClassMapper;
    private final TeachingClassScheduleMapper teachingClassScheduleMapper;
    private final CourseMapper courseMapper;
    private final CourseSelectionMapper courseSelectionMapper;
    private final TeacherMapper teacherMapper;
    private final SemesterService semesterService;
    private final StatTeacherWorkloadMapper statTeacherWorkloadMapper;

    /**
     * 是否按课程学分加权计算课时，默认关闭
     */
    @Value("${smart-university.statistics.teacher-workload.credit-factor-enabled:false}")
    private boolean creditFactorEnabled;

    @Override
    public TeacherWorkloadPageRespDTO pageTeacherWorkload(TeacherWorkloadQueryReqDTO requestParam) {
        Long semesterId = resolveSemesterId(requestParam.getSemesterId());
        WorkloadContext context = buildWorkloadContext(semesterId);
        List<TeacherWorkloadRespDTO> records = new ArrayList<>();
        for (Map.Entry<Long, List<TeachingClassDO>> entry : context.classMap.entrySet()) {
            Long teacherId = entry.getKey();
            if (requestParam.getTeacherId() != null && !requestParam.getTeacherId().equals(teacherId)) {
                continue;
            }
            TeacherDO teacherDO = context.teacherMap.get(teacherId);
            if (!matchKeyword(teacherDO, requestParam.getKeyword())) {
                continue;
            }
            records.add(buildWorkloadRespDTO(teacherId, teacherDO, entry.getValue(), context));
        }
        records.sort(Comparator.comparing(TeacherWorkloadRespDTO::getTotalHours, Comparator.reverseOrder())
                .thenComparing(TeacherWorkloadRespDTO::getTeacherId));
        int currentPage = requestParam.getCurrentPage();
        int limit = requestParam.getLimit();
        TeacherWorkloadPageRespDTO result = new TeacherWorkloadPageRespDTO();
        result.setRecords(paginate(records, currentPage, limit));
        result.setPage(currentPage);
        result.setPageSize(limit);
        result.setTotal(records.size());
        result.setExtra(buildExtra(semesterId, records));
        return result;
    }

    @Override
    public TeacherWorkloadDetailRespDTO getMyWorkload(Long semesterId) {
        Long teacherId = UserContextHolder.getTeacherId();
        if (teacherId == null) {
            throw new BizException(ResultCodeEnum.TEACHER_NOT_EXIST);
        }
        return getTeacherWorkload(teacherId, semesterId);
    }

    @Override
    public TeacherWorkloadDetailRespDTO getTeacherWorkload(Long teacherId, Long semesterId) {
        if (teacherId == null) {
            throw new BizException(ResultCodeEnum.TEACHER_NOT_EXIST);
        }
        Long targetSemesterId = resolveSemesterId(semesterId);
        WorkloadContext context = buildWorkloadContext(targetSemesterId);
        List<TeachingClassDO> teachingClassDOList = context.classMap.getOrDefault(teacherId, List.of());
        TeacherDO teacherDO = context.teacherMap.get(teacherId);
        if (teacherDO == null) {
            teacherDO = teacherMapper.getTeacherById(teacherId);
        }
        if (teacherDO == null) {
            throw new BizException(ResultCodeEnum.TEACHER_NOT_EXIST);
        }
        TeacherWorkloadDetailRespDTO result = new TeacherWorkloadDetailRespDTO();
        result.setTeacherId(teacherId);
        result.setTeacherNo(teacherDO.getTeacherNo());
        result.setTeacherName(teacherDO.getTeacherName());
        result.setSemesterId(targetSemesterId);
        result.setClassCount(teachingClassDOList.size());
        result.setTotalHours(sumHours(teachingClassDOList, context));
        result.setStudentCount(countDistinctStudent(teachingClassDOList, context));
        result.setClasses(buildClassDetailList(teachingClassDOList, context));
        return result;
    }

    @Override
    public BigDecimal getAverageWorkload(Long semesterId) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        WorkloadContext context = buildWorkloadContext(targetSemesterId);
        return buildExtra(targetSemesterId, buildAllWorkloadRespDTOList(context)).getAvgWorkload();
    }

    @Override
    public List<StatTeacherWorkloadDO> listTeacherWorkloadSnapshot(Long semesterId) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        WorkloadContext context = buildWorkloadContext(targetSemesterId);
        LocalDate statDate = LocalDate.now();
        LocalDateTime generatedAt = LocalDateTime.now();
        List<StatTeacherWorkloadDO> result = new ArrayList<>();
        for (TeacherWorkloadRespDTO each : buildAllWorkloadRespDTOList(context)) {
            StatTeacherWorkloadDO snapshot = new StatTeacherWorkloadDO();
            snapshot.setSemesterId(targetSemesterId);
            snapshot.setTeacherId(each.getTeacherId());
            snapshot.setTeacherNo(each.getTeacherNo() == null ? "" : each.getTeacherNo());
            snapshot.setTeacherName(each.getTeacherName() == null ? "" : each.getTeacherName());
            snapshot.setClassCount(each.getClassCount());
            snapshot.setTotalHours(each.getTotalHours());
            snapshot.setStudentCount(each.getStudentCount());
            snapshot.setStatDate(statDate);
            snapshot.setGeneratedAt(generatedAt);
            result.add(snapshot);
        }
        return result;
    }

    /**
     * 构建全部教师的工作量记录
     *
     * @param context 工作量统计上下文
     * @return 教师工作量记录集合
     */
    private List<TeacherWorkloadRespDTO> buildAllWorkloadRespDTOList(WorkloadContext context) {
        List<TeacherWorkloadRespDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<TeachingClassDO>> entry : context.classMap.entrySet()) {
            result.add(buildWorkloadRespDTO(entry.getKey(), context.teacherMap.get(entry.getKey()),
                    entry.getValue(), context));
        }
        return result;
    }

    /**
     * 构建单个教师的工作量记录
     *
     * @param teacherId           教师 ID
     * @param teacherDO           教师数据对象，可能为空
     * @param teachingClassDOList 该教师的教学班集合
     * @param context             工作量统计上下文
     * @return 教师工作量记录
     */
    private TeacherWorkloadRespDTO buildWorkloadRespDTO(Long teacherId, TeacherDO teacherDO,
                                                         List<TeachingClassDO> teachingClassDOList,
                                                         WorkloadContext context) {
        TeacherWorkloadRespDTO result = new TeacherWorkloadRespDTO();
        result.setTeacherId(teacherId);
        result.setTeacherNo(teacherDO == null ? null : teacherDO.getTeacherNo());
        result.setTeacherName(teacherDO == null ? null : teacherDO.getTeacherName());
        result.setClassCount(teachingClassDOList.size());
        result.setTotalHours(sumHours(teachingClassDOList, context));
        result.setStudentCount(countDistinctStudent(teachingClassDOList, context));
        return result;
    }

    /**
     * 构建教学班明细
     *
     * @param teachingClassDOList 教学班集合
     * @param context             工作量统计上下文
     * @return 教学班明细集合
     */
    private List<TeacherWorkloadClassRespDTO> buildClassDetailList(List<TeachingClassDO> teachingClassDOList,
                                                                   WorkloadContext context) {
        List<TeacherWorkloadClassRespDTO> result = new ArrayList<>();
        for (TeachingClassDO each : teachingClassDOList) {
            CourseDO courseDO = context.courseMap.get(each.getCourseId());
            TeacherWorkloadClassRespDTO record = new TeacherWorkloadClassRespDTO();
            record.setTeachingClassId(each.getId());
            record.setClassCode(each.getClassCode());
            record.setCourseName(courseDO == null ? null : courseDO.getCourseName());
            record.setCredit(courseDO == null ? null : courseDO.getCredit());
            record.setCapacity(each.getCapacity());
            record.setSelectedCount(context.studentMap.getOrDefault(each.getId(), Set.of()).size());
            record.setTotalHours(context.hoursMap.getOrDefault(each.getId(), 0));
            result.add(record);
        }
        result.sort(Comparator.comparing(TeacherWorkloadClassRespDTO::getTeachingClassId));
        return result;
    }

    /**
     * 构建口径回显
     *
     * @param semesterId 学期 ID
     * @param records    教师工作量记录集合
     * @return 口径回显
     */
    private TeacherWorkloadExtraRespDTO buildExtra(Long semesterId, List<TeacherWorkloadRespDTO> records) {
        int totalHours = 0;
        for (TeacherWorkloadRespDTO each : records) {
            totalHours += each.getTotalHours() == null ? 0 : each.getTotalHours();
        }
        TeacherWorkloadExtraRespDTO result = new TeacherWorkloadExtraRespDTO();
        result.setSemesterId(semesterId);
        result.setAvgWorkload(calculateAverage(totalHours, records.size()));
        result.setSnapshotTime(resolveSnapshotTime(semesterId));
        return result;
    }

    /**
     * 快照生成时间，无快照时返回当前时间
     *
     * @param semesterId 学期 ID
     * @return 快照时间
     */
    private LocalDateTime resolveSnapshotTime(Long semesterId) {
        LocalDateTime generatedAt = statTeacherWorkloadMapper.getLatestGeneratedAt(semesterId);
        return generatedAt == null ? LocalDateTime.now() : generatedAt;
    }

    /**
     * 构建工作量统计上下文
     *
     * @param semesterId 学期 ID
     * @return 工作量统计上下文
     */
    private WorkloadContext buildWorkloadContext(Long semesterId) {
        WorkloadContext context = new WorkloadContext();
        List<TeachingClassDO> teachingClassDOList = teachingClassMapper.listTeachingClassBySemesterId(semesterId)
                .stream()
                .filter(each -> TeachingClassStatusEnum.CANCELLED != each.getStatus())
                .toList();
        Map<Long, List<TeachingClassDO>> classMap = new HashMap<>();
        for (TeachingClassDO each : teachingClassDOList) {
            if (each.getTeacherId() == null) {
                continue;
            }
            classMap.computeIfAbsent(each.getTeacherId(), key -> new ArrayList<>()).add(each);
        }
        context.classMap = classMap;
        List<Long> courseIds = teachingClassDOList.stream()
                .map(TeachingClassDO::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        context.courseMap = courseIds.isEmpty()
                ? Map.of()
                : courseMapper.listCourseByIds(courseIds).stream()
                .collect(Collectors.toMap(CourseDO::getId, Function.identity(), (first, second) -> first));
        List<Long> teacherIds = classMap.keySet().stream().toList();
        context.teacherMap = teacherIds.isEmpty()
                ? Map.of()
                : teacherMapper.listTeacherByIds(teacherIds).stream()
                .collect(Collectors.toMap(TeacherDO::getId, Function.identity(), (first, second) -> first));
        Map<Long, Integer> hoursMap = new HashMap<>();
        List<TeachingClassScheduleDO> scheduleDOList = teachingClassScheduleMapper.listScheduleBySemesterId(semesterId);
        for (TeachingClassScheduleDO each : scheduleDOList) {
            int hours = calculateScheduleHours(each);
            if (creditFactorEnabled) {
                TeachingClassDO teachingClassDO = findTeachingClass(teachingClassDOList, each.getTeachingClassId());
                CourseDO courseDO = teachingClassDO == null ? null : context.courseMap.get(teachingClassDO.getCourseId());
                BigDecimal factor = courseDO == null || courseDO.getCredit() == null
                        ? BigDecimal.ONE
                        : courseDO.getCredit();
                hours = BigDecimal.valueOf(hours).multiply(factor).setScale(0, RoundingMode.HALF_UP).intValue();
            }
            hoursMap.merge(each.getTeachingClassId(), hours, Integer::sum);
        }
        context.hoursMap = hoursMap;
        Map<Long, Set<Long>> studentMap = new HashMap<>();
        for (CourseSelectionDO each : listSemesterSelection(semesterId, SelectionStatusEnum.SELECTED)) {
            if (each.getStudentId() == null || each.getTeachingClassId() == null) {
                continue;
            }
            studentMap.computeIfAbsent(each.getTeachingClassId(), key -> new HashSet<>()).add(each.getStudentId());
        }
        context.studentMap = studentMap;
        return context;
    }

    /**
     * 查询本学期指定状态的选课记录
     *
     * @param semesterId 学期 ID
     * @param status     选课状态
     * @return 选课记录集合
     */
    private List<CourseSelectionDO> listSemesterSelection(Long semesterId, SelectionStatusEnum status) {
        StudentSelectionQueryReqDTO requestParam = new StudentSelectionQueryReqDTO();
        requestParam.setSemesterId(semesterId);
        requestParam.setStatus(status.name());
        return courseSelectionMapper.listSelectionByStudent(requestParam);
    }

    /**
     * 计算单个教师的总课时
     *
     * @param teachingClassDOList 教学班集合
     * @param context             工作量统计上下文
     * @return 总课时
     */
    private int sumHours(List<TeachingClassDO> teachingClassDOList, WorkloadContext context) {
        int totalHours = 0;
        for (TeachingClassDO each : teachingClassDOList) {
            totalHours += context.hoursMap.getOrDefault(each.getId(), 0);
        }
        return totalHours;
    }

    /**
     * 统计教师的授课学生数（去重）
     *
     * @param teachingClassDOList 教学班集合
     * @param context             工作量统计上下文
     * @return 去重学生数
     */
    private int countDistinctStudent(List<TeachingClassDO> teachingClassDOList, WorkloadContext context) {
        Set<Long> studentIdSet = new HashSet<>();
        for (TeachingClassDO each : teachingClassDOList) {
            studentIdSet.addAll(context.studentMap.getOrDefault(each.getId(), Set.of()));
        }
        return studentIdSet.size();
    }

    /**
     * 计算单条排课的课时：节次数 × 周数
     *
     * @param scheduleDO 排课数据对象
     * @return 课时
     */
    private int calculateScheduleHours(TeachingClassScheduleDO scheduleDO) {
        if (scheduleDO.getStartSection() == null || scheduleDO.getEndSection() == null
                || scheduleDO.getStartWeek() == null || scheduleDO.getEndWeek() == null) {
            return 0;
        }
        int sectionCount = scheduleDO.getEndSection() - scheduleDO.getStartSection() + 1;
        int weekCount = scheduleDO.getEndWeek() - scheduleDO.getStartWeek() + 1;
        return Math.max(sectionCount, 0) * Math.max(weekCount, 0);
    }

    /**
     * 查找教学班
     *
     * @param teachingClassDOList 教学班集合
     * @param teachingClassId     教学班 ID
     * @return 教学班数据对象
     */
    private TeachingClassDO findTeachingClass(List<TeachingClassDO> teachingClassDOList, Long teachingClassId) {
        for (TeachingClassDO each : teachingClassDOList) {
            if (Objects.equals(each.getId(), teachingClassId)) {
                return each;
            }
        }
        return null;
    }

    /**
     * 判断教师是否命中关键字
     *
     * @param teacherDO 教师数据对象
     * @param keyword   姓名 / 工号关键字
     * @return 命中返回 true
     */
    private boolean matchKeyword(TeacherDO teacherDO, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return true;
        }
        if (teacherDO == null) {
            return false;
        }
        boolean matchNo = teacherDO.getTeacherNo() != null && teacherDO.getTeacherNo().contains(keyword);
        boolean matchName = teacherDO.getTeacherName() != null && teacherDO.getTeacherName().contains(keyword);
        return matchNo || matchName;
    }

    /**
     * 计算平均值，分母为 0 时返回 0
     *
     * @param total 总数
     * @param count 数量
     * @return 平均值，保留 1 位小数
     */
    private BigDecimal calculateAverage(int total, int count) {
        if (count <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
    }

    /**
     * 解析学期 ID，为空取当前学期
     *
     * @param semesterId 学期 ID
     * @return 学期 ID
     */
    private Long resolveSemesterId(Long semesterId) {
        if (semesterId != null) {
            return semesterId;
        }
        SemesterDO semesterDO = semesterService.getCurrentSemester();
        return semesterDO.getId();
    }

    /**
     * 内存分页
     *
     * @param source      全量记录
     * @param currentPage 当前页码
     * @param limit       每页条数
     * @return 当前页记录
     */
    private <T> List<T> paginate(List<T> source, int currentPage, int limit) {
        int fromIndex = Math.min((currentPage - 1) * limit, source.size());
        int toIndex = Math.min(fromIndex + limit, source.size());
        return new ArrayList<>(source.subList(fromIndex, toIndex));
    }

    /**
     * 教师工作量统计上下文
     */
    private static class WorkloadContext {

        /**
         * 教师 ID 到教学班集合的映射
         */
        private Map<Long, List<TeachingClassDO>> classMap = Map.of();

        /**
         * 课程 ID 到课程的映射
         */
        private Map<Long, CourseDO> courseMap = Map.of();

        /**
         * 教师 ID 到教师的映射
         */
        private Map<Long, TeacherDO> teacherMap = Map.of();

        /**
         * 教学班 ID 到课时的映射
         */
        private Map<Long, Integer> hoursMap = Map.of();

        /**
         * 教学班 ID 到已选学生 ID 集合的映射
         */
        private Map<Long, Set<Long>> studentMap = Map.of();
    }
}
