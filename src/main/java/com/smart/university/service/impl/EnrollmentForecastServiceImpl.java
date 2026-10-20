package com.smart.university.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.dto.req.EnrollmentForecastQueryReqDTO;
import com.smart.university.domain.dto.req.SemesterPageQueryReqDTO;
import com.smart.university.domain.dto.resp.EnrollmentForecastItemRespDTO;
import com.smart.university.domain.dto.resp.EnrollmentForecastRespDTO;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.enums.TeachingClassStatusEnum;
import com.smart.university.mapper.CourseMapper;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.SemesterMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.service.EnrollmentForecastService;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 选课热度预测与扩容建议服务实现（I-02）
 *
 * <p>口径：`forecastDemand = max(历史同期均值, (已选 + 候补) × 扩容系数)`；
 * 历史样本期数不足时 `sampleInsufficient=true` 且预测值为 0，绝不编造数值。
 */
@Service
@RequiredArgsConstructor
public class EnrollmentForecastServiceImpl implements EnrollmentForecastService {

    /**
     * 默认返回条数
     */
    private static final int DEFAULT_TOP_N = 20;

    /**
     * 样本不足时的说明文案
     */
    private static final String SAMPLE_INSUFFICIENT_REASON = "历史样本不足";

    /**
     * 历史学期扫描绝对上限（安全护栏，不再作为主驱动）
     */
    private static final int SEMESTER_SCAN_LIMIT = 200;

    /**
     * 历史学期回看数量（仅取最近 N 个非当前学期）
     */
    private static final int HISTORY_LOOKBACK_SEMESTERS = 8;

    /**
     * 历史学期回看时间窗（年）
     */
    private static final int HISTORY_LOOKBACK_YEARS = 5;

    /**
     * 招生预测缓存 TTL（毫秒），10 分钟
     */
    private static final long FORECAST_CACHE_TTL_MILLIS = 10L * 60 * 1000;

    /**
     * 招生预测结果内存缓存（学期 ID -> 预测结果），线程安全
     */
    private final Map<Long, ForecastCache> forecastCache = new ConcurrentHashMap<>();

    private final TeachingClassMapper teachingClassMapper;
    private final CourseSelectionMapper courseSelectionMapper;
    private final CourseMapper courseMapper;
    private final SemesterMapper semesterMapper;
    private final SemesterService semesterService;

    /**
     * 扩容系数
     */
    @Value("${smart-university.statistics.enrollment-forecast.expansion-factor:1.2}")
    private double expansionFactor;

    /**
     * 最小历史样本期数
     */
    @Value("${smart-university.statistics.enrollment-forecast.min-history-sample:2}")
    private int minHistorySample;

    @Override
    public EnrollmentForecastRespDTO listEnrollmentForecast(EnrollmentForecastQueryReqDTO requestParam) {
        Long targetSemesterId = requestParam.getSemesterId() == null
                ? semesterService.getCurrentSemester().getId()
                : requestParam.getSemesterId();
        ForecastCache cache = forecastCache.get(targetSemesterId);
        long now = System.currentTimeMillis();
        List<EnrollmentForecastItemRespDTO> fullRecords;
        if (cache != null && cache.expireAt > now) {
            fullRecords = cache.records;
        } else {
            fullRecords = computeFullForecast(targetSemesterId);
            forecastCache.put(targetSemesterId, new ForecastCache(fullRecords, now + FORECAST_CACHE_TTL_MILLIS));
        }
        return buildForecastResponse(targetSemesterId, fullRecords, requestParam);
    }

    /**
     * 计算全量招生预测明细（按预测需求降序），结果不裁剪 topN、不应用课程过滤，便于缓存复用
     *
     * @param targetSemesterId 学期 ID
     * @return 全量预测明细集合
     */
    private List<EnrollmentForecastItemRespDTO> computeFullForecast(Long targetSemesterId) {
        Map<Long, List<TeachingClassDO>> courseClassMap = buildCourseClassMap(targetSemesterId);
        Map<Long, Map<Long, List<Long>>> historyClassMap = buildHistoryClassMap(targetSemesterId);
        List<Long> courseIds = courseClassMap.keySet().stream().sorted().toList();
        Map<Long, CourseDO> courseMap = courseIds.isEmpty()
                ? Map.of()
                : courseMapper.listCourseByIds(courseIds).stream()
                .collect(Collectors.toMap(CourseDO::getId, Function.identity(), (first, second) -> first));
        List<EnrollmentForecastItemRespDTO> records = new ArrayList<>();
        for (Long each : courseIds) {
            records.add(buildForecastItem(each, courseClassMap.get(each), courseMap.get(each), historyClassMap));
        }
        records.sort(Comparator.comparing(EnrollmentForecastItemRespDTO::getForecastDemand, Comparator.reverseOrder())
                .thenComparing(EnrollmentForecastItemRespDTO::getCourseId));
        return records;
    }

    /**
     * 基于全量预测明细，按请求参数裁剪课程、topN，并处理样本不足异常
     *
     * @param targetSemesterId 学期 ID
     * @param fullRecords      全量预测明细
     * @param requestParam     请求参数
     * @return 招生预测出参
     */
    private EnrollmentForecastRespDTO buildForecastResponse(Long targetSemesterId,
                                                           List<EnrollmentForecastItemRespDTO> fullRecords,
                                                           EnrollmentForecastQueryReqDTO requestParam) {
        List<EnrollmentForecastItemRespDTO> records = fullRecords;
        if (requestParam.getCourseId() != null) {
            records = fullRecords.stream()
                    .filter(each -> requestParam.getCourseId().equals(each.getCourseId()))
                    .toList();
            boolean insufficient = records.isEmpty()
                    || Boolean.TRUE.equals(records.get(0).getSampleInsufficient());
            if (insufficient) {
                throw new BizException(ResultCodeEnum.ANALYTICS_SAMPLE_INSUFFICIENT);
            }
        }
        int topN = requestParam.getTopN() == null || requestParam.getTopN() < 1
                ? DEFAULT_TOP_N
                : requestParam.getTopN();
        EnrollmentForecastRespDTO result = new EnrollmentForecastRespDTO();
        result.setSemesterId(targetSemesterId);
        result.setRecords(records.stream().limit(topN).toList());
        return result;
    }

    /**
     * 构建本学期「课程 → 教学班」映射
     *
     * @param semesterId 学期 ID
     * @return 课程 ID 到教学班集合的映射
     */
    private Map<Long, List<TeachingClassDO>> buildCourseClassMap(Long semesterId) {
        Map<Long, List<TeachingClassDO>> result = new HashMap<>();
        for (TeachingClassDO each : teachingClassMapper.listTeachingClassBySemesterId(semesterId)) {
            if (TeachingClassStatusEnum.CANCELLED == each.getStatus() || each.getCourseId() == null) {
                continue;
            }
            result.computeIfAbsent(each.getCourseId(), key -> new ArrayList<>()).add(each);
        }
        return result;
    }

    /**
     * 构建历史学期「学期 → 课程 → 教学班 ID 集合」映射
     *
     * @param currentSemesterId 当前学期 ID，历史统计排除该学期
     * @return 历史映射
     */
    private Map<Long, Map<Long, List<Long>>> buildHistoryClassMap(Long currentSemesterId) {
        SemesterPageQueryReqDTO requestParam = new SemesterPageQueryReqDTO();
        List<SemesterDO> semesterDOList = semesterMapper
                .listSemesterByCondition(Page.of(1, SEMESTER_SCAN_LIMIT), requestParam)
                .getRecords();
        LocalDate historyWindowStart = LocalDate.now().minusYears(HISTORY_LOOKBACK_YEARS);
        List<SemesterDO> historySemesterList = semesterDOList.stream()
                .filter(each -> !Objects.equals(each.getId(), currentSemesterId))
                .filter(each -> each.getEndDate() != null && !each.getEndDate().isBefore(historyWindowStart))
                .sorted(Comparator.comparing(SemesterDO::getEndDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(HISTORY_LOOKBACK_SEMESTERS)
                .toList();
        Map<Long, Map<Long, List<Long>>> result = new HashMap<>();
        for (SemesterDO each : historySemesterList) {
            Map<Long, List<Long>> courseClassIdMap = new HashMap<>();
            for (TeachingClassDO teachingClassDO : teachingClassMapper.listTeachingClassBySemesterId(each.getId())) {
                if (TeachingClassStatusEnum.CANCELLED == teachingClassDO.getStatus()
                        || teachingClassDO.getCourseId() == null) {
                    continue;
                }
                courseClassIdMap.computeIfAbsent(teachingClassDO.getCourseId(), key -> new ArrayList<>())
                        .add(teachingClassDO.getId());
            }
            result.put(each.getId(), courseClassIdMap);
        }
        return result;
    }

    /**
     * 构建单个课程的预测明细
     *
     * @param courseId            课程 ID
     * @param teachingClassDOList 本学期该课程的教学班集合
     * @param courseDO            课程数据对象
     * @param historyClassMap     历史映射
     * @return 预测明细
     */
    private EnrollmentForecastItemRespDTO buildForecastItem(Long courseId,
                                                            List<TeachingClassDO> teachingClassDOList,
                                                            CourseDO courseDO,
                                                            Map<Long, Map<Long, List<Long>>> historyClassMap) {
        List<Long> teachingClassIds = teachingClassDOList.stream()
                .map(TeachingClassDO::getId)
                .toList();
        int currentCapacity = 0;
        for (TeachingClassDO each : teachingClassDOList) {
            currentCapacity += each.getCapacity() == null ? 0 : each.getCapacity();
        }
        long selectedCount = courseSelectionMapper.countSelectedByTeachingClassIds(teachingClassIds);
        Map<Long, Long> waitingMap = courseSelectionMapper.countWaitingMapByTeachingClassIds(teachingClassIds);
        long waitingCount = 0L;
        for (TeachingClassDO each : teachingClassDOList) {
            waitingCount += waitingMap.getOrDefault(each.getId(), 0L);
        }
        List<Long> historySampleList = new ArrayList<>();
        for (Map<Long, List<Long>> each : historyClassMap.values()) {
            List<Long> classIds = each.get(courseId);
            if (classIds == null || classIds.isEmpty()) {
                continue;
            }
            historySampleList.add(courseSelectionMapper.countSelectedByTeachingClassIds(classIds));
        }
        int historySampleCount = historySampleList.size();
        BigDecimal historyAvgSelected = calculateHistoryAvg(historySampleList);
        boolean sampleInsufficient = historySampleCount < minHistorySample;
        int currentDemand = (int) (selectedCount + waitingCount);
        int forecastDemand = 0;
        if (!sampleInsufficient) {
            BigDecimal expandDemand = BigDecimal.valueOf(currentDemand)
                    .multiply(BigDecimal.valueOf(expansionFactor))
                    .setScale(0, RoundingMode.HALF_UP);
            forecastDemand = historyAvgSelected.compareTo(expandDemand) > 0
                    ? historyAvgSelected.setScale(0, RoundingMode.HALF_UP).intValue()
                    : expandDemand.intValue();
        }
        int suggestedCapacityIncrement = forecastDemand > currentCapacity
                ? forecastDemand - currentCapacity
                : 0;
        EnrollmentForecastItemRespDTO result = new EnrollmentForecastItemRespDTO();
        result.setCourseId(courseId);
        result.setCourseCode(courseDO == null ? null : courseDO.getCourseCode());
        result.setCourseName(courseDO == null ? null : courseDO.getCourseName());
        result.setTeachingClassCount(teachingClassDOList.size());
        result.setCurrentCapacity(currentCapacity);
        result.setSelectedCount((int) selectedCount);
        result.setWaitingCount((int) waitingCount);
        result.setHistoryAvgSelected(historyAvgSelected);
        result.setHistorySampleCount(historySampleCount);
        result.setSampleInsufficient(sampleInsufficient);
        result.setForecastDemand(forecastDemand);
        result.setSuggestedCapacityIncrement(suggestedCapacityIncrement);
        result.setFullRisk(forecastDemand > currentCapacity);
        result.setReason(buildReason(sampleInsufficient, historyAvgSelected, (int) selectedCount,
                (int) waitingCount, forecastDemand));
        return result;
    }

    /**
     * 计算历史同期已选人次均值
     *
     * @param historySampleList 历史样本集合
     * @return 均值，保留 1 位小数
     */
    private BigDecimal calculateHistoryAvg(List<Long> historySampleList) {
        if (historySampleList.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        long total = 0L;
        for (Long each : historySampleList) {
            total += each;
        }
        return BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(historySampleList.size()), 1, RoundingMode.HALF_UP);
    }

    /**
     * 构建预测依据说明
     *
     * @param sampleInsufficient 样本是否不足
     * @param historyAvgSelected 历史均值
     * @param selectedCount      已选人数
     * @param waitingCount       候补人数
     * @param forecastDemand     预测需求
     * @return 说明文案
     */
    private String buildReason(boolean sampleInsufficient, BigDecimal historyAvgSelected,
                               int selectedCount, int waitingCount, int forecastDemand) {
        if (sampleInsufficient) {
            return SAMPLE_INSUFFICIENT_REASON;
        }
        return "历史同期均值 " + historyAvgSelected + " 人，本期已选 " + selectedCount
                + " + 候补 " + waitingCount + "，按扩容系数 " + expansionFactor
                + " 预测需求 " + forecastDemand + " 人";
    }

    /**
     * 招生预测缓存值，包含全量预测明细与过期时间戳
     */
    private static class ForecastCache {

        /**
         * 全量招生预测明细（已按预测需求降序排序，未裁剪 topN）
         */
        private final List<EnrollmentForecastItemRespDTO> records;

        /**
         * 过期时间戳（毫秒）
         */
        private final long expireAt;

        ForecastCache(List<EnrollmentForecastItemRespDTO> records, long expireAt) {
            this.records = records;
            this.expireAt = expireAt;
        }
    }
}
