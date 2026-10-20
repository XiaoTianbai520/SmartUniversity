package com.smart.university.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.university.common.base.PageResult;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.dto.req.ScoreAnomalyQueryReqDTO;
import com.smart.university.domain.dto.req.SemesterPageQueryReqDTO;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.dto.resp.ScoreAnomalyExtraRespDTO;
import com.smart.university.domain.dto.resp.ScoreAnomalyPageRespDTO;
import com.smart.university.domain.dto.resp.ScoreAnomalyRespDTO;
import com.smart.university.domain.dto.resp.ScoreDistributionRespDTO;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.MajorDO;
import com.smart.university.domain.entity.ScoreDO;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.entity.TeacherDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.domain.enums.TeachingClassStatusEnum;
import com.smart.university.mapper.CourseMapper;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.MajorMapper;
import com.smart.university.mapper.ScoreMapper;
import com.smart.university.mapper.SemesterMapper;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.mapper.TeacherMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.service.ScoreAnomalyService;
import com.smart.university.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 成绩分布异常预警服务实现（I-03）
 *
 * <p>基线 = 同 course_id 历史教学班及格率均值；无历史时取全校本学期均值。
 * 异常判定：及格率低于基线 `pass-rate-deviation` 个百分点、录入完整率不足且已过发布窗口、
 * 优秀率高于 `excellent-rate-threshold`。
 */
@Service
@RequiredArgsConstructor
public class ScoreAnomalyServiceImpl implements ScoreAnomalyService {

    /**
     * 及格分数线
     */
    private static final BigDecimal PASS_SCORE = BigDecimal.valueOf(60);

    /**
     * 优秀分数线
     */
    private static final BigDecimal EXCELLENT_SCORE = BigDecimal.valueOf(85);

    /**
     * 及格率偏低
     */
    private static final String ANOMALY_FAIL_RATE_HIGH = "FAIL_RATE_HIGH";

    /**
     * 成绩缺失
     */
    private static final String ANOMALY_SCORE_MISSING = "SCORE_MISSING";

    /**
     * 优秀率偏高
     */
    private static final String ANOMALY_SCORE_TOO_HIGH = "SCORE_TOO_HIGH";

    /**
     * 历史学期扫描绝对上限（安全护栏，不再作为主驱动）
     */
    private static final int SEMESTER_SCAN_LIMIT = 200;

    /**
     * 基线历史学期回看数量（仅取最近 N 个非当前学期）
     */
    private static final int BASELINE_LOOKBACK_SEMESTERS = 8;

    /**
     * 基线历史学期回看时间窗（年）
     */
    private static final int BASELINE_LOOKBACK_YEARS = 5;

    /**
     * 基线缓存 TTL（毫秒），10 分钟
     */
    private static final long BASELINE_CACHE_TTL_MILLIS = 10L * 60 * 1000;

    /**
     * 课程历史及格率基线内存缓存（学期 ID -> 基线数据），线程安全
     */
    private final Map<Long, BaselineCache> baselineCache = new ConcurrentHashMap<>();

    private final TeachingClassMapper teachingClassMapper;
    private final CourseSelectionMapper courseSelectionMapper;
    private final ScoreMapper scoreMapper;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final MajorMapper majorMapper;
    private final SemesterMapper semesterMapper;
    private final SemesterService semesterService;

    /**
     * 及格率与基线允许的偏差（百分点）
     */
    @Value("${smart-university.statistics.score-anomaly.pass-rate-deviation:15}")
    private int passRateDeviation;

    /**
     * 优秀率阈值（百分比）
     */
    @Value("${smart-university.statistics.score-anomaly.excellent-rate-threshold:60}")
    private int excellentRateThreshold;

    @Override
    public PageResult<ScoreAnomalyRespDTO> pageMyScoreAnomaly(ScoreAnomalyQueryReqDTO requestParam) {
        Long teacherId = UserContextHolder.getTeacherId();
        if (teacherId == null) {
            throw new BizException(ResultCodeEnum.TEACHER_NOT_EXIST);
        }
        Long targetSemesterId = resolveSemesterId(requestParam.getSemesterId());
        List<ScoreAnomalyRespDTO> records = listScoreAnomaly(targetSemesterId, teacherId, requestParam.getCourseId())
                .stream()
                .filter(each -> matchAnomalyType(each, requestParam.getAnomalyType()))
                .toList();
        int currentPage = requestParam.getCurrentPage();
        int limit = requestParam.getLimit();
        return new PageResult<>(paginate(records, currentPage, limit), currentPage, limit, records.size());
    }

    @Override
    public ScoreAnomalyPageRespDTO pageScoreAnomaly(ScoreAnomalyQueryReqDTO requestParam) {
        Long targetSemesterId = resolveSemesterId(requestParam.getSemesterId());
        List<ScoreAnomalyRespDTO> records = listScoreAnomaly(targetSemesterId, null, requestParam.getCourseId())
                .stream()
                .filter(each -> matchAnomalyType(each, requestParam.getAnomalyType()))
                .toList();
        int currentPage = requestParam.getCurrentPage();
        int limit = requestParam.getLimit();
        ScoreAnomalyPageRespDTO result = new ScoreAnomalyPageRespDTO();
        result.setRecords(paginate(records, currentPage, limit));
        result.setPage(currentPage);
        result.setPageSize(limit);
        result.setTotal(records.size());
        result.setExtra(buildExtra(targetSemesterId, records));
        return result;
    }

    @Override
    public ScoreAnomalyRespDTO getScoreAnomaly(Long semesterId, Long teachingClassId) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        return listScoreAnomaly(targetSemesterId).stream()
                .filter(each -> Objects.equals(each.getTeachingClassId(), teachingClassId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<ScoreAnomalyRespDTO> listScoreAnomaly(Long semesterId) {
        return listScoreAnomaly(semesterId, null, null);
    }

    /**
     * 查询成绩异常明细，支持按教师与课程过滤
     *
     * @param semesterId 学期 ID
     * @param teacherId  教师 ID，为空表示不过滤
     * @param courseId   课程 ID，为空表示不过滤
     * @return 成绩异常明细集合
     */
    private List<ScoreAnomalyRespDTO> listScoreAnomaly(Long semesterId, Long teacherId, Long courseId) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        AnomalyContext context = buildAnomalyContext(targetSemesterId, teacherId, courseId);
        List<ScoreAnomalyRespDTO> result = new ArrayList<>();
        for (TeachingClassDO each : context.teachingClassDOList) {
            ScoreAnomalyRespDTO record = buildAnomalyRecord(each, context);
            if (record.getAnomalyTypes() == null || record.getAnomalyTypes().isEmpty()) {
                continue;
            }
            result.add(record);
        }
        result.sort(Comparator.comparing(ScoreAnomalyRespDTO::getTeachingClassId));
        return result;
    }

    /**
     * 构建成绩异常统计上下文
     *
     * @param semesterId 学期 ID
     * @param teacherId  教师 ID，为空表示不过滤
     * @param courseId   课程 ID，为空表示不过滤
     * @return 统计上下文
     */
    private AnomalyContext buildAnomalyContext(Long semesterId, Long teacherId, Long courseId) {
        AnomalyContext context = new AnomalyContext();
        context.semesterId = semesterId;
        SemesterDO semesterDO = semesterService.getSemesterById(semesterId);
        context.publishWindowPassed = semesterDO != null && semesterDO.getEndDate() != null
                && semesterDO.getEndDate().isBefore(LocalDate.now());
        context.teachingClassDOList = teachingClassMapper.listTeachingClassBySemesterId(semesterId).stream()
                .filter(each -> TeachingClassStatusEnum.CANCELLED != each.getStatus())
                .filter(each -> teacherId == null || teacherId.equals(each.getTeacherId()))
                .filter(each -> courseId == null || courseId.equals(each.getCourseId()))
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
        context.selectedCountMap = countByTeachingClass(selectedList);
        context.selectionMap = selectedList.stream()
                .collect(Collectors.toMap(CourseSelectionDO::getId, Function.identity(), (first, second) -> first));
        List<ScoreDO> scoreDOList = selectedList.isEmpty()
                ? List.of()
                : scoreMapper.listScoreBySelectionIds(selectedList.stream().map(CourseSelectionDO::getId).toList());
        Map<Long, List<ScoreDO>> scoreMap = new HashMap<>();
        for (ScoreDO each : scoreDOList) {
            CourseSelectionDO selectionDO = context.selectionMap.get(each.getCourseSelectionId());
            if (selectionDO == null || selectionDO.getTeachingClassId() == null) {
                continue;
            }
            scoreMap.computeIfAbsent(selectionDO.getTeachingClassId(), key -> new ArrayList<>()).add(each);
        }
        context.scoreMap = scoreMap;
        context.majorNameMap = buildMajorNameMap(selectedList);
        context.baselineMap = getBaselineMap(semesterId);
        context.schoolBaselinePassRate = calculateSchoolBaseline(context);
        return context;
    }

    /**
     * 构建教学班的主要专业名称映射
     *
     * @param selectedList 本学期已选记录集合
     * @return 教学班 ID 到主要专业名称的映射
     */
    private Map<Long, String> buildMajorNameMap(List<CourseSelectionDO> selectedList) {
        if (selectedList.isEmpty()) {
            return Map.of();
        }
        List<Long> studentIds = selectedList.stream()
                .map(CourseSelectionDO::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, StudentDO> studentMap = studentMapper.listStudentByIds(studentIds).stream()
                .collect(Collectors.toMap(StudentDO::getId, Function.identity(), (first, second) -> first));
        List<Long> majorIds = studentMap.values().stream()
                .map(StudentDO::getMajorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MajorDO> majorDetailMap = majorIds.isEmpty()
                ? Map.of()
                : majorMapper.listMajorByIds(majorIds).stream()
                .collect(Collectors.toMap(MajorDO::getId, Function.identity(), (first, second) -> first));
        Map<Long, Map<Long, Integer>> classMajorCountMap = new HashMap<>();
        for (CourseSelectionDO each : selectedList) {
            StudentDO studentDO = studentMap.get(each.getStudentId());
            if (studentDO == null || studentDO.getMajorId() == null || each.getTeachingClassId() == null) {
                continue;
            }
            Map<Long, Integer> majorCountMap = classMajorCountMap
                    .computeIfAbsent(each.getTeachingClassId(), key -> new HashMap<>());
            majorCountMap.merge(studentDO.getMajorId(), 1, Integer::sum);
        }
        Map<Long, String> result = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Integer>> entry : classMajorCountMap.entrySet()) {
            Long majorId = entry.getValue().entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            MajorDO majorDO = majorId == null ? null : majorDetailMap.get(majorId);
            result.put(entry.getKey(), majorDO == null ? null : majorDO.getMajorName());
        }
        return result;
    }

    /**
     * 构建同课程历史及格率基线
     *
     * @param semesterId 当前学期 ID
     * @param context    统计上下文
     * @return 课程 ID 到基线及格率的映射
     */
    private Map<Long, BigDecimal> buildBaselineMap(Long semesterId) {
        SemesterPageQueryReqDTO requestParam = new SemesterPageQueryReqDTO();
        List<SemesterDO> semesterDOList = semesterMapper
                .listSemesterByCondition(Page.of(1, SEMESTER_SCAN_LIMIT), requestParam)
                .getRecords();
        LocalDate baselineWindowStart = LocalDate.now().minusYears(BASELINE_LOOKBACK_YEARS);
        List<SemesterDO> historySemesterList = semesterDOList.stream()
                .filter(each -> !Objects.equals(each.getId(), semesterId))
                .filter(each -> each.getEndDate() != null && !each.getEndDate().isBefore(baselineWindowStart))
                .sorted(Comparator.comparing(SemesterDO::getEndDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(BASELINE_LOOKBACK_SEMESTERS)
                .toList();
        Map<Long, List<BigDecimal>> coursePassRateMap = new HashMap<>();
        for (SemesterDO each : historySemesterList) {
            List<CourseSelectionDO> historySelectionList =
                    listSemesterSelection(each.getId(), SelectionStatusEnum.SELECTED);
            if (historySelectionList.isEmpty()) {
                continue;
            }
            Map<Long, CourseSelectionDO> historySelectionMap = historySelectionList.stream()
                    .collect(Collectors.toMap(CourseSelectionDO::getId, Function.identity(),
                            (first, second) -> first));
            List<TeachingClassDO> historyClassList = teachingClassMapper.listTeachingClassBySemesterId(each.getId());
            Map<Long, Long> historyClassCourseMap = historyClassList.stream()
                    .filter(item -> item.getCourseId() != null)
                    .collect(Collectors.toMap(TeachingClassDO::getId, TeachingClassDO::getCourseId,
                            (first, second) -> first));
            List<ScoreDO> historyScoreList = scoreMapper.listPublishedScoreBySemesterId(each.getId());
            Map<Long, List<BigDecimal>> courseScoreMap = new HashMap<>();
            for (ScoreDO scoreDO : historyScoreList) {
                CourseSelectionDO selectionDO = historySelectionMap.get(scoreDO.getCourseSelectionId());
                if (selectionDO == null || scoreDO.getScoreValue() == null) {
                    continue;
                }
                Long courseId = historyClassCourseMap.get(selectionDO.getTeachingClassId());
                if (courseId == null) {
                    continue;
                }
                courseScoreMap.computeIfAbsent(courseId, key -> new ArrayList<>())
                        .add(scoreDO.getScoreValue());
            }
            for (Map.Entry<Long, List<BigDecimal>> entry : courseScoreMap.entrySet()) {
                coursePassRateMap.computeIfAbsent(entry.getKey(), key -> new ArrayList<>())
                        .add(calculatePassRate(entry.getValue()));
            }
        }
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map.Entry<Long, List<BigDecimal>> entry : coursePassRateMap.entrySet()) {
            BigDecimal total = entry.getValue().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            result.put(entry.getKey(),
                    total.divide(BigDecimal.valueOf(entry.getValue().size()), 1, RoundingMode.HALF_UP));
        }
        return result;
    }

    /**
     * 获取课程历史及格率基线，带内存 TTL 缓存
     *
     * <p>命中且未过期时直接返回缓存，否则重新计算并写入缓存，10 分钟内重复请求零数据库开销。
     *
     * @param semesterId 当前学期 ID
     * @return 课程 ID 到基线及格率的映射
     */
    private Map<Long, BigDecimal> getBaselineMap(Long semesterId) {
        BaselineCache cache = baselineCache.get(semesterId);
        long now = System.currentTimeMillis();
        if (cache != null && cache.expireAt > now) {
            return cache.data;
        }
        Map<Long, BigDecimal> data = buildBaselineMap(semesterId);
        baselineCache.put(semesterId, new BaselineCache(data, now + BASELINE_CACHE_TTL_MILLIS));
        return data;
    }

    /**
     * 计算全校本学期及格率均值
     *
     * @param context 统计上下文
     * @return 全校基线及格率
     */
    private BigDecimal calculateSchoolBaseline(AnomalyContext context) {
        List<BigDecimal> rateList = new ArrayList<>();
        for (TeachingClassDO each : context.teachingClassDOList) {
            List<ScoreDO> scoreDOList = context.scoreMap.getOrDefault(each.getId(), List.of());
            if (scoreDOList.isEmpty()) {
                continue;
            }
            rateList.add(calculatePassRate(scoreDOList.stream()
                    .map(ScoreDO::getScoreValue)
                    .filter(Objects::nonNull)
                    .toList()));
        }
        if (rateList.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        BigDecimal total = rateList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(rateList.size()), 1, RoundingMode.HALF_UP);
    }

    /**
     * 构建单个教学班的异常记录
     *
     * @param teachingClassDO 教学班数据对象
     * @param context         统计上下文
     * @return 成绩异常记录
     */
    private ScoreAnomalyRespDTO buildAnomalyRecord(TeachingClassDO teachingClassDO, AnomalyContext context) {
        List<ScoreDO> scoreDOList = context.scoreMap.getOrDefault(teachingClassDO.getId(), List.of());
        List<BigDecimal> scoreValueList = scoreDOList.stream()
                .map(ScoreDO::getScoreValue)
                .filter(Objects::nonNull)
                .toList();
        int selectedCount = context.selectedCountMap.getOrDefault(teachingClassDO.getId(), 0);
        int scoredCount = scoreDOList.size();
        BigDecimal passRate = calculatePassRate(scoreValueList);
        BigDecimal excellentRate = calculateRate(countExcellent(scoreValueList), scoreValueList.size());
        BigDecimal inputCompleteRate = calculateRate(scoredCount, selectedCount);
        BigDecimal baselinePassRate = context.baselineMap.getOrDefault(teachingClassDO.getCourseId(),
                context.schoolBaselinePassRate);
        BigDecimal deviation = passRate.subtract(baselinePassRate).setScale(1, RoundingMode.HALF_UP);
        List<String> anomalyTypes = new ArrayList<>();
        if (passRate.compareTo(baselinePassRate.subtract(BigDecimal.valueOf(passRateDeviation))) < 0) {
            anomalyTypes.add(ANOMALY_FAIL_RATE_HIGH);
        }
        if (context.publishWindowPassed && inputCompleteRate.compareTo(BigDecimal.valueOf(100)) < 0) {
            anomalyTypes.add(ANOMALY_SCORE_MISSING);
        }
        if (excellentRate.compareTo(BigDecimal.valueOf(excellentRateThreshold)) > 0) {
            anomalyTypes.add(ANOMALY_SCORE_TOO_HIGH);
        }
        CourseDO courseDO = context.courseMap.get(teachingClassDO.getCourseId());
        TeacherDO teacherDO = context.teacherMap.get(teachingClassDO.getTeacherId());
        ScoreAnomalyRespDTO result = new ScoreAnomalyRespDTO();
        result.setTeachingClassId(teachingClassDO.getId());
        result.setClassCode(teachingClassDO.getClassCode());
        result.setCourseName(courseDO == null ? null : courseDO.getCourseName());
        result.setTeacherName(teacherDO == null ? null : teacherDO.getTeacherName());
        result.setMajorName(context.majorNameMap.get(teachingClassDO.getId()));
        result.setSelectedCount(selectedCount);
        result.setScoredCount(scoredCount);
        result.setInputCompleteRate(inputCompleteRate);
        result.setPassRate(passRate);
        result.setExcellentRate(excellentRate);
        result.setBaselinePassRate(baselinePassRate);
        result.setDeviation(deviation);
        result.setAnomalyTypes(List.copyOf(anomalyTypes));
        result.setAnomalyReason(buildAnomalyReason(anomalyTypes, baselinePassRate, deviation, inputCompleteRate));
        result.setDistribution(buildDistribution(scoreValueList));
        return result;
    }

    /**
     * 构建五段成绩分布
     *
     * @param scoreValueList 成绩集合
     * @return 分段分布
     */
    private List<ScoreDistributionRespDTO> buildDistribution(List<BigDecimal> scoreValueList) {
        Map<String, Integer> segmentCountMap = new LinkedHashMap<>();
        segmentCountMap.put("0-59", 0);
        segmentCountMap.put("60-69", 0);
        segmentCountMap.put("70-79", 0);
        segmentCountMap.put("80-84", 0);
        segmentCountMap.put("85-100", 0);
        for (BigDecimal each : scoreValueList) {
            if (each.compareTo(PASS_SCORE) < 0) {
                segmentCountMap.merge("0-59", 1, Integer::sum);
            } else if (each.compareTo(BigDecimal.valueOf(70)) < 0) {
                segmentCountMap.merge("60-69", 1, Integer::sum);
            } else if (each.compareTo(BigDecimal.valueOf(80)) < 0) {
                segmentCountMap.merge("70-79", 1, Integer::sum);
            } else if (each.compareTo(EXCELLENT_SCORE) < 0) {
                segmentCountMap.merge("80-84", 1, Integer::sum);
            } else {
                segmentCountMap.merge("85-100", 1, Integer::sum);
            }
        }
        List<ScoreDistributionRespDTO> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : segmentCountMap.entrySet()) {
            ScoreDistributionRespDTO record = new ScoreDistributionRespDTO();
            record.setSegment(entry.getKey());
            record.setCount(entry.getValue());
            result.add(record);
        }
        return result;
    }

    /**
     * 构建异常原因文案
     *
     * @param anomalyTypes      异常类型集合
     * @param baselinePassRate  基线及格率
     * @param deviation         偏差
     * @param inputCompleteRate 录入完整率
     * @return 异常原因
     */
    private String buildAnomalyReason(List<String> anomalyTypes, BigDecimal baselinePassRate,
                                      BigDecimal deviation, BigDecimal inputCompleteRate) {
        List<String> reasonList = new ArrayList<>();
        if (anomalyTypes.contains(ANOMALY_FAIL_RATE_HIGH)) {
            reasonList.add("及格率低于同课程历史基线 " + deviation.abs() + " 个百分点（基线 " + baselinePassRate + "）");
        }
        if (anomalyTypes.contains(ANOMALY_SCORE_MISSING)) {
            reasonList.add("成绩录入完整率 " + inputCompleteRate + "%，已过发布窗口仍有缺失");
        }
        if (anomalyTypes.contains(ANOMALY_SCORE_TOO_HIGH)) {
            reasonList.add("优秀率高于阈值 " + excellentRateThreshold + "%");
        }
        return String.join("；", reasonList);
    }

    /**
     * 构建口径回显
     *
     * @param semesterId 学期 ID
     * @param records    异常记录集合
     * @return 口径回显
     */
    private ScoreAnomalyExtraRespDTO buildExtra(Long semesterId, List<ScoreAnomalyRespDTO> records) {
        int failRateHighCount = 0;
        int scoreMissingCount = 0;
        int scoreTooHighCount = 0;
        for (ScoreAnomalyRespDTO each : records) {
            if (each.getAnomalyTypes() == null) {
                continue;
            }
            if (each.getAnomalyTypes().contains(ANOMALY_FAIL_RATE_HIGH)) {
                failRateHighCount++;
            }
            if (each.getAnomalyTypes().contains(ANOMALY_SCORE_MISSING)) {
                scoreMissingCount++;
            }
            if (each.getAnomalyTypes().contains(ANOMALY_SCORE_TOO_HIGH)) {
                scoreTooHighCount++;
            }
        }
        ScoreAnomalyExtraRespDTO result = new ScoreAnomalyExtraRespDTO();
        result.setSemesterId(semesterId);
        result.setFailRateHighCount(failRateHighCount);
        result.setScoreMissingCount(scoreMissingCount);
        result.setScoreTooHighCount(scoreTooHighCount);
        return result;
    }

    /**
     * 计算及格率
     *
     * @param scoreValueList 成绩集合
     * @return 及格率百分比，保留 1 位小数
     */
    private BigDecimal calculatePassRate(List<BigDecimal> scoreValueList) {
        long passCount = scoreValueList.stream().filter(each -> each.compareTo(PASS_SCORE) >= 0).count();
        return calculateRate((int) passCount, scoreValueList.size());
    }

    /**
     * 统计优秀人数
     *
     * @param scoreValueList 成绩集合
     * @return 优秀人数
     */
    private int countExcellent(List<BigDecimal> scoreValueList) {
        return (int) scoreValueList.stream().filter(each -> each.compareTo(EXCELLENT_SCORE) >= 0).count();
    }

    /**
     * 计算百分比，分母为 0 时返回 0
     *
     * @param numerator   分子
     * @param denominator 分母
     * @return 百分比，保留 1 位小数
     */
    private BigDecimal calculateRate(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
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
     * 判断是否命中异常类型筛选
     *
     * @param record      异常记录
     * @param anomalyType 异常类型，为空时全部命中
     * @return 命中返回 true
     */
    private boolean matchAnomalyType(ScoreAnomalyRespDTO record, String anomalyType) {
        if (anomalyType == null || anomalyType.isEmpty()) {
            return true;
        }
        return record.getAnomalyTypes() != null && record.getAnomalyTypes().contains(anomalyType);
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
     * 解析学期 ID，为空取当前学期
     *
     * @param semesterId 学期 ID
     * @return 学期 ID
     */
    private Long resolveSemesterId(Long semesterId) {
        return semesterId == null ? semesterService.getCurrentSemester().getId() : semesterId;
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
     * 基线缓存值，包含基线数据与过期时间戳
     */
    private static class BaselineCache {

        /**
         * 课程 ID 到基线及格率的映射
         */
        private final Map<Long, BigDecimal> data;

        /**
         * 过期时间戳（毫秒）
         */
        private final long expireAt;

        BaselineCache(Map<Long, BigDecimal> data, long expireAt) {
            this.data = data;
            this.expireAt = expireAt;
        }
    }

    /**
     * 成绩异常统计上下文
     */
    private static class AnomalyContext {

        /**
         * 学期 ID
         */
        private Long semesterId;

        /**
         * 是否已过成绩发布窗口
         */
        private boolean publishWindowPassed;

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
         * 选课记录映射
         */
        private Map<Long, CourseSelectionDO> selectionMap = Map.of();

        /**
         * 教学班已选人数映射
         */
        private Map<Long, Integer> selectedCountMap = Map.of();

        /**
         * 教学班成绩集合映射
         */
        private Map<Long, List<ScoreDO>> scoreMap = Map.of();

        /**
         * 教学班主要专业名称映射
         */
        private Map<Long, String> majorNameMap = Map.of();

        /**
         * 课程历史及格率基线映射
         */
        private Map<Long, BigDecimal> baselineMap = Map.of();

        /**
         * 全校本学期及格率基线
         */
        private BigDecimal schoolBaselinePassRate = BigDecimal.ZERO;
    }
}
