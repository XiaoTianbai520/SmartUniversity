package com.smart.university.service.impl;

import com.smart.university.domain.dto.req.CourseSelectionAnalysisQueryReqDTO;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.dto.resp.CourseSelectionAnalysisExtraRespDTO;
import com.smart.university.domain.dto.resp.CourseSelectionAnalysisPageRespDTO;
import com.smart.university.domain.dto.resp.CourseSelectionAnalysisRespDTO;
import com.smart.university.domain.dto.resp.GradeDistributionRespDTO;
import com.smart.university.domain.dto.resp.MajorDistributionRespDTO;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.GradeCohortDO;
import com.smart.university.domain.entity.MajorDO;
import com.smart.university.domain.entity.SelectionBatchDO;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.entity.StatCourseSelectionDO;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.entity.TeacherDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.domain.enums.TeachingClassStatusEnum;
import com.smart.university.mapper.CourseMapper;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.GradeCohortMapper;
import com.smart.university.mapper.MajorMapper;
import com.smart.university.mapper.SelectionBatchClassMapper;
import com.smart.university.mapper.SelectionBatchMapper;
import com.smart.university.mapper.StatCourseSelectionMapper;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.mapper.TeacherMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.service.CourseSelectionAnalysisService;
import com.smart.university.service.SemesterService;
import lombok.RequiredArgsConstructor;
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
 * 课程选课分析服务实现（S-05）
 *
 * <p>C1 红线：已选人数一律 `COUNT(course_selection WHERE status='SELECTED')`，
 * 严禁读 Redis 计数 Key 与 `teaching_class.selected_count`。
 */
@Service
@RequiredArgsConstructor
public class CourseSelectionAnalysisServiceImpl implements CourseSelectionAnalysisService {

    /**
     * 按已选人数排序
     */
    private static final String SORT_BY_SELECTED = "SELECTED";

    /**
     * 按满班率排序
     */
    private static final String SORT_BY_FULL_RATE = "FULL_RATE";

    /**
     * 按退课人数排序
     */
    private static final String SORT_BY_WITHDRAW = "WITHDRAW";

    private final TeachingClassMapper teachingClassMapper;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final CourseSelectionMapper courseSelectionMapper;
    private final StudentMapper studentMapper;
    private final MajorMapper majorMapper;
    private final GradeCohortMapper gradeCohortMapper;
    private final SelectionBatchMapper selectionBatchMapper;
    private final SelectionBatchClassMapper selectionBatchClassMapper;
    private final StatCourseSelectionMapper statCourseSelectionMapper;
    private final SemesterService semesterService;

    @Override
    public CourseSelectionAnalysisPageRespDTO pageCourseSelectionAnalysis(CourseSelectionAnalysisQueryReqDTO requestParam) {
        Long semesterId = resolveSemesterId(requestParam.getSemesterId());
        AnalysisContext context = buildAnalysisContext(semesterId, requestParam.getMajorId(), requestParam.getGradeId());
        List<CourseSelectionAnalysisRespDTO> records = new ArrayList<>();
        for (TeachingClassDO each : context.teachingClassDOList) {
            if (requestParam.getCourseId() != null && !requestParam.getCourseId().equals(each.getCourseId())) {
                continue;
            }
            CourseSelectionAnalysisRespDTO record = buildAnalysisRespDTO(each, context);
            if (Boolean.TRUE.equals(requestParam.getOnlyFull()) && !Integer.valueOf(1).equals(record.getFullFlag())) {
                continue;
            }
            records.add(record);
        }
        sortRecords(records, requestParam.getSortBy());
        int currentPage = requestParam.getCurrentPage();
        int limit = requestParam.getLimit();
        CourseSelectionAnalysisPageRespDTO result = new CourseSelectionAnalysisPageRespDTO();
        result.setRecords(paginate(records, currentPage, limit));
        result.setPage(currentPage);
        result.setPageSize(limit);
        result.setTotal(records.size());
        result.setExtra(buildExtra(semesterId, context));
        return result;
    }

    @Override
    public BigDecimal getFullClassRate(Long semesterId) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        AnalysisContext context = buildAnalysisContext(targetSemesterId, null, null);
        return calculateFullClassRate(targetSemesterId, context);
    }

    @Override
    public List<StatCourseSelectionDO> listCourseSelectionSnapshot(Long semesterId) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        AnalysisContext context = buildAnalysisContext(targetSemesterId, null, null);
        LocalDate statDate = LocalDate.now();
        LocalDateTime generatedAt = LocalDateTime.now();
        List<StatCourseSelectionDO> result = new ArrayList<>();
        for (TeachingClassDO each : context.teachingClassDOList) {
            CourseSelectionAnalysisRespDTO record = buildAnalysisRespDTO(each, context);
            StatCourseSelectionDO snapshot = new StatCourseSelectionDO();
            snapshot.setSemesterId(targetSemesterId);
            snapshot.setCourseId(each.getCourseId());
            snapshot.setTeachingClassId(each.getId());
            snapshot.setCourseCode(record.getCourseCode() == null ? "" : record.getCourseCode());
            snapshot.setCourseName(record.getCourseName() == null ? "" : record.getCourseName());
            snapshot.setClassCode(each.getClassCode() == null ? "" : each.getClassCode());
            snapshot.setTeacherId(each.getTeacherId());
            snapshot.setTeacherName(record.getTeacherName());
            snapshot.setCapacity(each.getCapacity() == null ? 0 : each.getCapacity());
            snapshot.setSelectedCount(record.getSelectedCount());
            snapshot.setWaitingCount(record.getWaitingCount());
            snapshot.setWithdrawCount(record.getWithdrawCount());
            snapshot.setFullFlag(record.getFullFlag());
            snapshot.setStatDate(statDate);
            snapshot.setGeneratedAt(generatedAt);
            result.add(snapshot);
        }
        return result;
    }

    /**
     * 构建单个教学班的分析记录
     *
     * @param teachingClassDO 教学班数据对象
     * @param context         分析上下文
     * @return 课程选课分析记录
     */
    private CourseSelectionAnalysisRespDTO buildAnalysisRespDTO(TeachingClassDO teachingClassDO,
                                                                AnalysisContext context) {
        CourseDO courseDO = context.courseMap.get(teachingClassDO.getCourseId());
        TeacherDO teacherDO = context.teacherMap.get(teachingClassDO.getTeacherId());
        Integer selectedCount = context.selectedCountMap.getOrDefault(teachingClassDO.getId(), 0);
        Integer waitingCount = context.waitingCountMap.getOrDefault(teachingClassDO.getId(), 0);
        Integer withdrawCount = context.withdrawCountMap.getOrDefault(teachingClassDO.getId(), 0);
        Integer capacity = teachingClassDO.getCapacity();
        boolean full = capacity != null && selectedCount >= capacity;
        CourseSelectionAnalysisRespDTO result = new CourseSelectionAnalysisRespDTO();
        result.setCourseId(teachingClassDO.getCourseId());
        result.setCourseCode(courseDO == null ? null : courseDO.getCourseCode());
        result.setCourseName(courseDO == null ? null : courseDO.getCourseName());
        result.setTeachingClassId(teachingClassDO.getId());
        result.setClassCode(teachingClassDO.getClassCode());
        result.setTeacherId(teachingClassDO.getTeacherId());
        result.setTeacherName(teacherDO == null ? null : teacherDO.getTeacherName());
        result.setCapacity(capacity);
        result.setSelectedCount(selectedCount);
        result.setWaitingCount(waitingCount);
        result.setWithdrawCount(withdrawCount);
        result.setFullFlag(full ? 1 : 0);
        result.setFullRate(calculateRate(selectedCount, capacity));
        return result;
    }

    /**
     * 构建分析上下文
     *
     * @param semesterId 学期 ID
     * @param majorId    专业筛选，为空不过滤
     * @param gradeId    年级筛选，为空不过滤
     * @return 分析上下文
     */
    private AnalysisContext buildAnalysisContext(Long semesterId, Long majorId, Long gradeId) {
        AnalysisContext context = new AnalysisContext();
        context.teachingClassDOList = teachingClassMapper.listTeachingClassBySemesterId(semesterId).stream()
                .filter(each -> TeachingClassStatusEnum.CANCELLED != each.getStatus())
                .toList();
        List<Long> courseIds = context.teachingClassDOList.stream()
                .map(TeachingClassDO::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        context.courseMap = courseIds.isEmpty()
                ? Map.of()
                : courseMapper.listCourseByIds(courseIds).stream()
                .collect(Collectors.toMap(CourseDO::getId, Function.identity(), (first, second) -> first));
        List<Long> teacherIds = context.teachingClassDOList.stream()
                .map(TeachingClassDO::getTeacherId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        context.teacherMap = teacherIds.isEmpty()
                ? Map.of()
                : teacherMapper.listTeacherByIds(teacherIds).stream()
                .collect(Collectors.toMap(TeacherDO::getId, Function.identity(), (first, second) -> first));
        List<CourseSelectionDO> selectedList = listSemesterSelection(semesterId, SelectionStatusEnum.SELECTED);
        List<CourseSelectionDO> waitingList = listSemesterSelection(semesterId, SelectionStatusEnum.WAITING);
        List<CourseSelectionDO> withdrawList = listSemesterSelection(semesterId, SelectionStatusEnum.WITHDRAWN);
        context.studentMap = buildStudentMap(selectedList, waitingList, withdrawList);
        context.selectedList = filterByStudent(selectedList, context.studentMap, majorId, gradeId);
        context.selectedCountMap = countByTeachingClass(context.selectedList);
        context.waitingCountMap = countByTeachingClass(filterByStudent(waitingList, context.studentMap, majorId, gradeId));
        context.withdrawCountMap = countByTeachingClass(filterByStudent(withdrawList, context.studentMap, majorId, gradeId));
        return context;
    }

    /**
     * 构建学生映射
     *
     * @param selectionLists 选课记录集合
     * @return 学生 ID 到学生的映射
     */
    @SafeVarargs
    private Map<Long, StudentDO> buildStudentMap(List<CourseSelectionDO>... selectionLists) {
        Set<Long> studentIdSet = new HashSet<>();
        for (List<CourseSelectionDO> each : selectionLists) {
            for (CourseSelectionDO selectionDO : each) {
                if (selectionDO.getStudentId() != null) {
                    studentIdSet.add(selectionDO.getStudentId());
                }
            }
        }
        if (studentIdSet.isEmpty()) {
            return Map.of();
        }
        return studentMapper.listStudentByIds(studentIdSet.stream().toList()).stream()
                .collect(Collectors.toMap(StudentDO::getId, Function.identity(), (first, second) -> first));
    }

    /**
     * 按专业 / 年级过滤选课记录
     *
     * @param selectionDOList 选课记录集合
     * @param studentMap      学生映射
     * @param majorId         专业筛选，为空不过滤
     * @param gradeId         年级筛选，为空不过滤
     * @return 过滤后的选课记录集合
     */
    private List<CourseSelectionDO> filterByStudent(List<CourseSelectionDO> selectionDOList,
                                                    Map<Long, StudentDO> studentMap,
                                                    Long majorId, Long gradeId) {
        if (majorId == null && gradeId == null) {
            return selectionDOList;
        }
        return selectionDOList.stream().filter(each -> {
            StudentDO studentDO = studentMap.get(each.getStudentId());
            if (studentDO == null) {
                return false;
            }
            boolean matchMajor = majorId == null || majorId.equals(studentDO.getMajorId());
            boolean matchGrade = gradeId == null || gradeId.equals(studentDO.getGradeId());
            return matchMajor && matchGrade;
        }).toList();
    }

    /**
     * 按教学班统计选课记录数
     *
     * @param selectionDOList 选课记录集合
     * @return 教学班 ID 到数量的映射
     */
    private Map<Long, Integer> countByTeachingClass(List<CourseSelectionDO> selectionDOList) {
        Map<Long, Integer> result = new HashMap<>();
        for (CourseSelectionDO each : selectionDOList) {
            if (each.getTeachingClassId() == null) {
                continue;
            }
            result.merge(each.getTeachingClassId(), 1, Integer::sum);
        }
        return result;
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
     * 构建口径回显
     *
     * @param semesterId 学期 ID
     * @param context    分析上下文
     * @return 口径回显
     */
    private CourseSelectionAnalysisExtraRespDTO buildExtra(Long semesterId, AnalysisContext context) {
        CourseSelectionAnalysisExtraRespDTO result = new CourseSelectionAnalysisExtraRespDTO();
        result.setSemesterId(semesterId);
        result.setFullClassRate(calculateFullClassRate(semesterId, context));
        result.setMajorDistribution(buildMajorDistribution(context));
        result.setGradeDistribution(buildGradeDistribution(context));
        result.setSnapshotTime(resolveSnapshotTime(semesterId));
        return result;
    }

    /**
     * 计算整体满班率：分母为当前批次关联且未取消的教学班数，分子为其中已满班的数量
     *
     * @param semesterId 学期 ID
     * @param context    分析上下文
     * @return 满班率百分比，保留 1 位小数
     */
    private BigDecimal calculateFullClassRate(Long semesterId, AnalysisContext context) {
        List<TeachingClassDO> targetClassList = resolveBatchTeachingClassList(semesterId, context);
        if (targetClassList.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        int fullCount = 0;
        for (TeachingClassDO each : targetClassList) {
            Integer selectedCount = context.selectedCountMap.getOrDefault(each.getId(), 0);
            if (each.getCapacity() != null && selectedCount >= each.getCapacity()) {
                fullCount++;
            }
        }
        return calculateRate(fullCount, targetClassList.size());
    }

    /**
     * 取满班率的分母教学班集合：优先当前批次关联的教学班，无批次时退回本学期未取消的教学班
     *
     * @param semesterId 学期 ID
     * @param context    分析上下文
     * @return 教学班集合
     */
    private List<TeachingClassDO> resolveBatchTeachingClassList(Long semesterId, AnalysisContext context) {
        SelectionBatchDO selectionBatchDO = selectionBatchMapper.getCurrentSelectionBatch(semesterId);
        if (selectionBatchDO == null) {
            return context.teachingClassDOList;
        }
        List<Long> batchClassIds = selectionBatchClassMapper.listTeachingClassIdByBatchId(selectionBatchDO.getId());
        if (batchClassIds.isEmpty()) {
            return context.teachingClassDOList;
        }
        Set<Long> batchClassIdSet = new HashSet<>(batchClassIds);
        return context.teachingClassDOList.stream()
                .filter(each -> batchClassIdSet.contains(each.getId()))
                .toList();
    }

    /**
     * 构建专业分布
     *
     * @param context 分析上下文
     * @return 专业分布集合
     */
    private List<MajorDistributionRespDTO> buildMajorDistribution(AnalysisContext context) {
        Map<Long, Integer> countMap = new HashMap<>();
        for (CourseSelectionDO each : context.selectedList) {
            StudentDO studentDO = context.studentMap.get(each.getStudentId());
            if (studentDO == null || studentDO.getMajorId() == null) {
                continue;
            }
            countMap.merge(studentDO.getMajorId(), 1, Integer::sum);
        }
        Map<Long, MajorDO> majorMap = countMap.isEmpty()
                ? Map.of()
                : majorMapper.listMajorByIds(countMap.keySet().stream().toList()).stream()
                .collect(Collectors.toMap(MajorDO::getId, Function.identity(), (first, second) -> first));
        List<MajorDistributionRespDTO> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : countMap.entrySet()) {
            MajorDO majorDO = majorMap.get(entry.getKey());
            MajorDistributionRespDTO record = new MajorDistributionRespDTO();
            record.setMajorId(entry.getKey());
            record.setMajorName(majorDO == null ? null : majorDO.getMajorName());
            record.setCount(entry.getValue());
            result.add(record);
        }
        result.sort(Comparator.comparing(MajorDistributionRespDTO::getCount, Comparator.reverseOrder())
                .thenComparing(MajorDistributionRespDTO::getMajorId));
        return result;
    }

    /**
     * 构建年级分布
     *
     * @param context 分析上下文
     * @return 年级分布集合
     */
    private List<GradeDistributionRespDTO> buildGradeDistribution(AnalysisContext context) {
        Map<Long, Integer> countMap = new HashMap<>();
        for (CourseSelectionDO each : context.selectedList) {
            StudentDO studentDO = context.studentMap.get(each.getStudentId());
            if (studentDO == null || studentDO.getGradeId() == null) {
                continue;
            }
            countMap.merge(studentDO.getGradeId(), 1, Integer::sum);
        }
        Map<Long, GradeCohortDO> gradeMap = countMap.isEmpty()
                ? Map.of()
                : gradeCohortMapper.listGradeCohortByIds(countMap.keySet().stream().toList()).stream()
                .collect(Collectors.toMap(GradeCohortDO::getId, Function.identity(), (first, second) -> first));
        List<GradeDistributionRespDTO> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : countMap.entrySet()) {
            GradeCohortDO gradeCohortDO = gradeMap.get(entry.getKey());
            GradeDistributionRespDTO record = new GradeDistributionRespDTO();
            record.setGradeId(entry.getKey());
            record.setGradeName(gradeCohortDO == null ? null : gradeCohortDO.getGradeName());
            record.setCount(entry.getValue());
            result.add(record);
        }
        result.sort(Comparator.comparing(GradeDistributionRespDTO::getCount, Comparator.reverseOrder())
                .thenComparing(GradeDistributionRespDTO::getGradeId));
        return result;
    }

    /**
     * 快照生成时间，无快照时返回当前时间
     *
     * @param semesterId 学期 ID
     * @return 快照时间
     */
    private LocalDateTime resolveSnapshotTime(Long semesterId) {
        LocalDateTime generatedAt = statCourseSelectionMapper.getLatestGeneratedAt(semesterId);
        return generatedAt == null ? LocalDateTime.now() : generatedAt;
    }

    /**
     * 按排序方式排序记录
     *
     * @param records 记录集合
     * @param sortBy  排序方式
     */
    private void sortRecords(List<CourseSelectionAnalysisRespDTO> records, String sortBy) {
        String targetSortBy = sortBy == null || sortBy.isEmpty() ? SORT_BY_SELECTED : sortBy.toUpperCase();
        if (SORT_BY_FULL_RATE.equals(targetSortBy)) {
            records.sort(Comparator.comparing(CourseSelectionAnalysisRespDTO::getFullRate, Comparator.reverseOrder())
                    .thenComparing(CourseSelectionAnalysisRespDTO::getTeachingClassId));
            return;
        }
        if (SORT_BY_WITHDRAW.equals(targetSortBy)) {
            records.sort(Comparator.comparing(CourseSelectionAnalysisRespDTO::getWithdrawCount, Comparator.reverseOrder())
                    .thenComparing(CourseSelectionAnalysisRespDTO::getTeachingClassId));
            return;
        }
        records.sort(Comparator.comparing(CourseSelectionAnalysisRespDTO::getSelectedCount, Comparator.reverseOrder())
                .thenComparing(CourseSelectionAnalysisRespDTO::getTeachingClassId));
    }

    /**
     * 计算百分比，分母为 0 时返回 0，上限 100
     *
     * @param numerator   分子
     * @param denominator 分母
     * @return 百分比，保留 1 位小数
     */
    private BigDecimal calculateRate(int numerator, Integer denominator) {
        if (denominator == null || denominator <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        BigDecimal rate = BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
        BigDecimal maxRate = BigDecimal.valueOf(100).setScale(1, RoundingMode.HALF_UP);
        return rate.compareTo(maxRate) > 0 ? maxRate : rate;
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
     * 课程选课分析上下文
     */
    private static class AnalysisContext {

        /**
         * 本学期未取消的教学班集合
         */
        private List<TeachingClassDO> teachingClassDOList = List.of();

        /**
         * 课程映射
         */
        private Map<Long, CourseDO> courseMap = Map.of();

        /**
         * 教师映射
         */
        private Map<Long, TeacherDO> teacherMap = Map.of();

        /**
         * 学生映射
         */
        private Map<Long, StudentDO> studentMap = Map.of();

        /**
         * 过滤后的已选记录集合
         */
        private List<CourseSelectionDO> selectedList = List.of();

        /**
         * 教学班已选人数映射
         */
        private Map<Long, Integer> selectedCountMap = Map.of();

        /**
         * 教学班候补人数映射
         */
        private Map<Long, Integer> waitingCountMap = Map.of();

        /**
         * 教学班退课人数映射
         */
        private Map<Long, Integer> withdrawCountMap = Map.of();
    }
}
