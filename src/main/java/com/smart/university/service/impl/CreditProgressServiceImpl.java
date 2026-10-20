package com.smart.university.service.impl;

import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.enums.RiskLevelEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.dto.resp.CreditProgressItemRespDTO;
import com.smart.university.domain.dto.resp.CreditProgressRespDTO;
import com.smart.university.domain.dto.resp.TrainingPlanRespDTO;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.GradeCohortDO;
import com.smart.university.domain.entity.ScoreDO;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.enums.CourseTypeEnum;
import com.smart.university.mapper.CourseMapper;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.GradeCohortMapper;
import com.smart.university.mapper.ScoreMapper;
import com.smart.university.mapper.SemesterMapper;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.service.CreditProgressService;
import com.smart.university.service.SemesterService;
import com.smart.university.service.TrainingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学生学分进度服务实现（S-02）
 *
 * <p>C8 红线：本服务的学分一律为「跨学期累计」。S-01 起累计增加学期上界：
 * 只统计开课学期开始日期不晚于统计截止学期（{@code semesterId}，为空取当前生效学期）的已发布成绩。
 * 仍使用 {@code ScoreMapper#listPublishedScoreByStudentId} 与
 * {@code CourseSelectionMapper#listSelectionByStudentId} 两个按学生维度查询的方法，
 * 不使用任何按学生逐学期 N+1 的查询。
 */
@Service
@RequiredArgsConstructor
public class CreditProgressServiceImpl implements CreditProgressService {

    /**
     * 无培养方案时的提示文案
     */
    private static final String NO_PLAN_MESSAGE = "未配置本专业本年级培养方案，暂无法计算学分进度";

    private final CourseSelectionMapper courseSelectionMapper;
    private final ScoreMapper scoreMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final CourseMapper courseMapper;
    private final StudentMapper studentMapper;
    private final GradeCohortMapper gradeCohortMapper;
    private final SemesterMapper semesterMapper;
    private final SemesterService semesterService;
    private final TrainingPlanService trainingPlanService;

    /**
     * 及格线，仅用于计算附加字段 passedCredit，不作为计入门槛
     */
    @Value("${smart-university.statistics.credit.pass-score:60}")
    private int passScore;

    /**
     * 同一课程重修多次是否去重，默认不去重
     */
    @Value("${smart-university.statistics.credit.dedup-by-course:false}")
    private boolean dedupByCourse;

    /**
     * 默认学制（年）
     */
    @Value("${smart-university.statistics.graduation-risk.default-school-years:4}")
    private int defaultSchoolYears;

    /**
     * 高风险阈值：需求速率超过学期最大学分的该比例
     */
    @Value("${smart-university.statistics.graduation-risk.high-rate:0.8}")
    private double highRate;

    /**
     * 中风险阈值：需求速率超过学期最大学分的该比例
     */
    @Value("${smart-university.statistics.graduation-risk.medium-rate:0.6}")
    private double mediumRate;

    @Override
    public CreditProgressRespDTO getCreditProgress(Long semesterId) {
        Long studentId = UserContextHolder.getStudentId();
        if (studentId == null) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_EXIST);
        }
        return getCreditProgressByStudentId(studentId, semesterId);
    }

    @Override
    public CreditProgressRespDTO getCreditProgressByStudentId(Long studentId, Long semesterId) {
        if (studentId == null) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_EXIST);
        }
        Long targetSemesterId = semesterId == null ? semesterService.getCurrentSemester().getId() : semesterId;
        Map<Long, CourseSelectionDO> selectionMap = courseSelectionMapper.listSelectionByStudentId(studentId)
                .stream()
                .collect(Collectors.toMap(CourseSelectionDO::getId, Function.identity(), (first, second) -> first));
        List<ScoreDO> publishedScoreDOList = scoreMapper.listPublishedScoreByStudentId(studentId);
        Map<Long, TeachingClassDO> teachingClassMap = buildTeachingClassMap(selectionMap, publishedScoreDOList);
        Map<Long, CourseDO> courseMap = buildCourseMap(teachingClassMap);
        LocalDate targetSemesterStart = resolveSemesterStart(targetSemesterId);
        Map<Long, LocalDate> semesterStartMap = buildSemesterStartMap(teachingClassMap);
        CreditAccumulator accumulator = accumulateCredit(selectionMap, teachingClassMap, courseMap,
                publishedScoreDOList, targetSemesterStart, semesterStartMap);
        TrainingPlanRespDTO trainingPlanRespDTO = getEnabledPlan(studentId);
        return buildRespDTO(targetSemesterId, accumulator, trainingPlanRespDTO);
    }

    @Override
    public RiskLevelEnum getRiskLevel(Long studentId, Long semesterId, CreditProgressRespDTO creditProgress) {
        CreditProgressRespDTO progress = creditProgress == null
                ? getCreditProgressByStudentId(studentId, semesterId)
                : creditProgress;
        if (progress == null || !Boolean.TRUE.equals(progress.getHasPlan())) {
            return RiskLevelEnum.LOW;
        }
        BigDecimal planTotalCredit = defaultZero(progress.getPlanTotalCredit());
        BigDecimal gapCredit = planTotalCredit.subtract(defaultZero(progress.getCompletedCredit()));
        if (gapCredit.compareTo(BigDecimal.ZERO) <= 0) {
            return RiskLevelEnum.LOW;
        }
        BigDecimal maxSelectionCredit = getMaxSelectionCredit(semesterId);
        if (maxSelectionCredit.compareTo(BigDecimal.ZERO) <= 0) {
            return RiskLevelEnum.LOW;
        }
        int remainingTermCount = resolveRemainingTermCount(studentId, semesterId);
        BigDecimal requiredRate = gapCredit.divide(BigDecimal.valueOf(remainingTermCount), 1, RoundingMode.HALF_UP);
        BigDecimal highThreshold = maxSelectionCredit.multiply(BigDecimal.valueOf(highRate));
        BigDecimal mediumThreshold = maxSelectionCredit.multiply(BigDecimal.valueOf(mediumRate));
        if (requiredRate.compareTo(highThreshold) > 0) {
            return RiskLevelEnum.HIGH;
        }
        if (requiredRate.compareTo(mediumThreshold) > 0) {
            return RiskLevelEnum.MEDIUM;
        }
        return RiskLevelEnum.LOW;
    }

    /**
     * 构建教学班维度映射，只覆盖已发布成绩涉及的选课记录
     *
     * @param selectionMap          选课记录映射
     * @param publishedScoreDOList  已发布成绩集合
     * @return 教学班 ID 到教学班的映射
     */
    private Map<Long, TeachingClassDO> buildTeachingClassMap(Map<Long, CourseSelectionDO> selectionMap,
                                                             List<ScoreDO> publishedScoreDOList) {
        List<Long> teachingClassIds = publishedScoreDOList.stream()
                .map(each -> selectionMap.get(each.getCourseSelectionId()))
                .filter(Objects::nonNull)
                .map(CourseSelectionDO::getTeachingClassId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (teachingClassIds.isEmpty()) {
            return Map.of();
        }
        return teachingClassMapper.listTeachingClassByIds(teachingClassIds).stream()
                .collect(Collectors.toMap(TeachingClassDO::getId, Function.identity(), (first, second) -> first));
    }

    /**
     * 构建课程维度映射
     *
     * @param teachingClassMap 教学班映射
     * @return 课程 ID 到课程的映射
     */
    private Map<Long, CourseDO> buildCourseMap(Map<Long, TeachingClassDO> teachingClassMap) {
        List<Long> courseIds = teachingClassMap.values().stream()
                .map(TeachingClassDO::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        return courseMapper.listCourseByIds(courseIds).stream()
                .collect(Collectors.toMap(CourseDO::getId, Function.identity(), (first, second) -> first));
    }

    /**
     * 跨学期累计学分：已发布即计入 completedCredit，达到及格线的部分额外计入 passedCredit
     *
     * @param selectionMap          选课记录映射
     * @param teachingClassMap      教学班映射
     * @param courseMap             课程映射
     * @param publishedScoreDOList  已发布成绩集合
     * @param targetSemesterStart   统计截止学期开始日期，为空表示不限上界
     * @param semesterStartMap      学期 ID 到开始日期的映射
     * @return 学分累计结果
     */
    private CreditAccumulator accumulateCredit(Map<Long, CourseSelectionDO> selectionMap,
                                               Map<Long, TeachingClassDO> teachingClassMap,
                                               Map<Long, CourseDO> courseMap,
                                               List<ScoreDO> publishedScoreDOList,
                                               LocalDate targetSemesterStart,
                                               Map<Long, LocalDate> semesterStartMap) {
        CreditAccumulator result = new CreditAccumulator();
        Set<Long> countedCourseIds = new HashSet<>();
        BigDecimal passScoreDecimal = BigDecimal.valueOf(passScore);
        for (ScoreDO each : publishedScoreDOList) {
            CourseSelectionDO selectionDO = selectionMap.get(each.getCourseSelectionId());
            if (selectionDO == null) {
                continue;
            }
            TeachingClassDO teachingClassDO = teachingClassMap.get(selectionDO.getTeachingClassId());
            if (teachingClassDO == null) {
                continue;
            }
            // S-01：只累计「开课学期开始日期 <= 统计截止学期开始日期」的成绩，实现按学期切片的累计
            LocalDate scoreSemesterStart = semesterStartMap.get(teachingClassDO.getSemesterId());
            if (targetSemesterStart != null && scoreSemesterStart != null
                    && scoreSemesterStart.isAfter(targetSemesterStart)) {
                continue;
            }
            CourseDO courseDO = courseMap.get(teachingClassDO.getCourseId());
            if (courseDO == null || courseDO.getCredit() == null) {
                continue;
            }
            if (dedupByCourse && !countedCourseIds.add(courseDO.getId())) {
                continue;
            }
            BigDecimal credit = courseDO.getCredit();
            result.completedCredit = result.completedCredit.add(credit);
            result.addByType(result.completedByType, courseDO.getCourseType(), credit);
            boolean passed = each.getScoreValue() != null
                    && each.getScoreValue().compareTo(passScoreDecimal) >= 0;
            if (passed) {
                result.passedCredit = result.passedCredit.add(credit);
                result.addByType(result.passedByType, courseDO.getCourseType(), credit);
            }
        }
        return result;
    }

    /**
     * 查询学生「专业 + 入学年级」对应的启用中培养方案
     *
     * @param studentId 学生 ID
     * @return 培养方案，无方案时返回 null
     */
    private TrainingPlanRespDTO getEnabledPlan(Long studentId) {
        StudentDO studentDO = studentMapper.getStudentById(studentId);
        if (studentDO == null) {
            return null;
        }
        return trainingPlanService.getEnabledTrainingPlan(studentDO.getMajorId(), studentDO.getGradeId());
    }

    /**
     * 组装学分进度出参
     *
     * @param semesterId          统计截止学期 ID
     * @param accumulator         学分累计结果
     * @param trainingPlanRespDTO 培养方案，无方案时为 null
     * @return 学分进度出参
     */
    private CreditProgressRespDTO buildRespDTO(Long semesterId, CreditAccumulator accumulator,
                                               TrainingPlanRespDTO trainingPlanRespDTO) {
        CreditProgressRespDTO result = new CreditProgressRespDTO();
        result.setSemesterId(semesterId);
        result.setHasPlan(trainingPlanRespDTO != null);
        result.setMessage(trainingPlanRespDTO == null ? NO_PLAN_MESSAGE : "");
        BigDecimal completedCredit = scaleOne(accumulator.completedCredit);
        BigDecimal passedCredit = scaleOne(accumulator.passedCredit);
        BigDecimal planTotalCredit = trainingPlanRespDTO == null
                ? BigDecimal.ZERO
                : defaultZero(trainingPlanRespDTO.getTotalCredit());
        result.setCompletedCredit(completedCredit);
        result.setPassedCredit(passedCredit);
        result.setPlanTotalCredit(scaleOne(planTotalCredit));
        result.setProgressRate(calculateRate(completedCredit, planTotalCredit));
        result.setItems(buildItems(accumulator, trainingPlanRespDTO));
        return result;
    }

    /**
     * 构建按课程类型的分项完成情况
     *
     * @param accumulator         学分累计结果
     * @param trainingPlanRespDTO 培养方案，无方案时为 null
     * @return 分项集合
     */
    private List<CreditProgressItemRespDTO> buildItems(CreditAccumulator accumulator,
                                                       TrainingPlanRespDTO trainingPlanRespDTO) {
        List<CreditProgressItemRespDTO> result = new ArrayList<>();
        for (CourseTypeEnum each : CourseTypeEnum.values()) {
            CreditProgressItemRespDTO item = new CreditProgressItemRespDTO();
            item.setCourseType(each.name());
            item.setCourseTypeName(each.getDescription());
            BigDecimal typeCompleted = scaleOne(accumulator.getByType(accumulator.completedByType, each));
            BigDecimal typePassed = scaleOne(accumulator.getByType(accumulator.passedByType, each));
            BigDecimal typeRequired = trainingPlanRespDTO == null
                    ? BigDecimal.ZERO
                    : defaultZero(resolvePlanCredit(trainingPlanRespDTO, each));
            BigDecimal gapCredit = typeRequired.subtract(typeCompleted);
            item.setCompletedCredit(typeCompleted);
            item.setPassedCredit(typePassed);
            item.setRequiredCredit(scaleOne(typeRequired));
            item.setGapCredit(scaleOne(gapCredit.compareTo(BigDecimal.ZERO) > 0 ? gapCredit : BigDecimal.ZERO));
            item.setProgressRate(calculateRate(typeCompleted, typeRequired));
            result.add(item);
        }
        return result;
    }

    /**
     * 取培养方案中指定课程类型的学分要求
     *
     * @param trainingPlanRespDTO 培养方案
     * @param courseType          课程类型
     * @return 学分要求
     */
    private BigDecimal resolvePlanCredit(TrainingPlanRespDTO trainingPlanRespDTO, CourseTypeEnum courseType) {
        if (CourseTypeEnum.REQUIRED == courseType) {
            return trainingPlanRespDTO.getRequiredCredit();
        }
        if (CourseTypeEnum.MAJOR_ELECTIVE == courseType) {
            return trainingPlanRespDTO.getMajorElectiveCredit();
        }
        return trainingPlanRespDTO.getPublicElectiveCredit();
    }

    /**
     * 计算完成度百分比，分母为 0 时返回 0
     *
     * @param completed 已完成学分
     * @param required  要求学分
     * @return 百分比，保留 1 位小数
     */
    private BigDecimal calculateRate(BigDecimal completed, BigDecimal required) {
        BigDecimal safeCompleted = defaultZero(completed);
        BigDecimal safeRequired = defaultZero(required);
        if (safeRequired.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        BigDecimal rate = safeCompleted.multiply(BigDecimal.valueOf(100))
                .divide(safeRequired, 1, RoundingMode.HALF_UP);
        // 完成度上限 100，与 stat_credit_progress.progress_rate 的 CHECK(BETWEEN 0 AND 100) 对齐
        BigDecimal maxRate = BigDecimal.valueOf(100).setScale(1, RoundingMode.HALF_UP);
        return rate.compareTo(maxRate) > 0 ? maxRate : rate;
    }

    /**
     * 获取学期最大学分上限
     *
     * @param semesterId 学期 ID
     * @return 最大学分，无学期或未配置时返回 0
     */
    private BigDecimal getMaxSelectionCredit(Long semesterId) {
        Long targetSemesterId = semesterId == null ? semesterService.getCurrentSemester().getId() : semesterId;
        SemesterDO semesterDO = semesterService.getSemesterById(targetSemesterId);
        if (semesterDO == null || semesterDO.getMaxSelectionCredit() == null) {
            return BigDecimal.ZERO;
        }
        return defaultZero(semesterDO.getMaxSelectionCredit());
    }

    /**
     * 计算剩余学期数，至少为 1。
     *
     * <p>M-01：按学期表实算已读学期数，不再用「当前学年 - 入学年份」的年差近似。
     * {@code 剩余学期数 = 学制 × 2 - 已读学期数 + 1}。
     *
     * @param studentId  学生 ID
     * @param semesterId 统计截止学期 ID，为空时取当前生效学期
     * @return 剩余学期数
     */
    private int resolveRemainingTermCount(Long studentId, Long semesterId) {
        int entryYear = LocalDate.now().getYear();
        StudentDO studentDO = studentMapper.getStudentById(studentId);
        if (studentDO != null && studentDO.getGradeId() != null) {
            GradeCohortDO gradeCohortDO = gradeCohortMapper.getGradeCohortById(studentDO.getGradeId());
            if (gradeCohortDO != null && gradeCohortDO.getEntryYear() != null) {
                entryYear = gradeCohortDO.getEntryYear();
            }
        }
        LocalDate entryStart = resolveEntrySemesterStart(entryYear);
        LocalDate baseStart = resolveSemesterStart(semesterId);
        LocalDate baseDate = baseStart == null ? LocalDate.now() : baseStart;
        long passedTermCount = semesterMapper.countSemesterByStartDateBetween(entryStart, baseDate);
        int remaining = defaultSchoolYears * 2 - (int) passedTermCount + 1;
        return Math.max(remaining, 1);
    }

    /**
     * 计算入学学期开始日期：按 grade_cohort.entry_year 拼学年 + 第 1 学期查询学期表
     *
     * @param entryYear 入学年份
     * @return 入学学期开始日期，查不到时兜底为当年 9 月 1 日
     */
    private LocalDate resolveEntrySemesterStart(int entryYear) {
        SemesterDO entrySemester = semesterMapper.getSemesterByAcademicYearAndTerm(
                entryYear + "-" + (entryYear + 1), 1);
        if (entrySemester != null && entrySemester.getStartDate() != null) {
            return entrySemester.getStartDate();
        }
        return LocalDate.of(entryYear, 9, 1);
    }

    /**
     * 取学期开始日期，semesterId 为空时取当前生效学期
     *
     * @param semesterId 学期 ID
     * @return 学期开始日期，取不到时返回 null
     */
    private LocalDate resolveSemesterStart(Long semesterId) {
        SemesterDO semesterDO = semesterId == null
                ? semesterService.getCurrentSemester()
                : semesterService.getSemesterById(semesterId);
        return semesterDO == null ? null : semesterDO.getStartDate();
    }

    /**
     * 批量构建教学班所属学期的开始日期映射，用于按学期切片累计
     *
     * @param teachingClassMap 教学班映射
     * @return 学期 ID 到开始日期的映射
     */
    private Map<Long, LocalDate> buildSemesterStartMap(Map<Long, TeachingClassDO> teachingClassMap) {
        List<Long> semesterIds = teachingClassMap.values().stream()
                .map(TeachingClassDO::getSemesterId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, LocalDate> result = new HashMap<>();
        for (SemesterDO each : semesterMapper.listSemesterByIds(semesterIds)) {
            if (each.getStartDate() != null) {
                result.put(each.getId(), each.getStartDate());
            }
        }
        return result;
    }

    /**
     * 空学分按 0 处理
     *
     * @param credit 学分
     * @return 非空学分
     */
    private BigDecimal defaultZero(BigDecimal credit) {
        return credit == null ? BigDecimal.ZERO : credit;
    }

    /**
     * 学分统一保留 1 位小数
     *
     * @param credit 学分
     * @return 标度为 1 的学分
     */
    private BigDecimal scaleOne(BigDecimal credit) {
        return defaultZero(credit).setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 学分累计载体，按课程类型分桶保存已完成与及格学分
     */
    private static class CreditAccumulator {

        /**
         * 已完成总学分
         */
        private BigDecimal completedCredit = BigDecimal.ZERO;

        /**
         * 及格总学分
         */
        private BigDecimal passedCredit = BigDecimal.ZERO;

        /**
         * 分类型的已完成学分
         */
        private final Map<CourseTypeEnum, BigDecimal> completedByType = new EnumMap<>(CourseTypeEnum.class);

        /**
         * 分类型的及格学分
         */
        private final Map<CourseTypeEnum, BigDecimal> passedByType = new EnumMap<>(CourseTypeEnum.class);

        /**
         * 按课程类型累加学分
         *
         * @param target     目标分桶
         * @param courseType 课程类型
         * @param credit     学分
         */
        private void addByType(Map<CourseTypeEnum, BigDecimal> target, CourseTypeEnum courseType, BigDecimal credit) {
            if (courseType == null) {
                return;
            }
            target.merge(courseType, credit, BigDecimal::add);
        }

        /**
         * 取指定课程类型的累计学分
         *
         * @param target     目标分桶
         * @param courseType 课程类型
         * @return 累计学分
         */
        private BigDecimal getByType(Map<CourseTypeEnum, BigDecimal> target, CourseTypeEnum courseType) {
            return target.getOrDefault(courseType, BigDecimal.ZERO);
        }
    }
}
