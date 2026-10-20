package com.smart.university.service.impl;

import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.enums.RiskLevelEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.common.util.EnumParseUtil;
import com.smart.university.domain.dto.req.GraduationRiskQueryReqDTO;
import com.smart.university.domain.dto.resp.CreditProgressItemRespDTO;
import com.smart.university.domain.dto.resp.CreditProgressRespDTO;
import com.smart.university.domain.dto.resp.GraduationRiskExtraRespDTO;
import com.smart.university.domain.dto.resp.GraduationRiskGapRespDTO;
import com.smart.university.domain.dto.resp.GraduationRiskPageRespDTO;
import com.smart.university.domain.dto.resp.GraduationRiskRespDTO;
import com.smart.university.domain.entity.GradeCohortDO;
import com.smart.university.domain.entity.MajorDO;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.entity.StatCreditProgressDO;
import com.smart.university.domain.enums.CourseTypeEnum;
import com.smart.university.mapper.GradeCohortMapper;
import com.smart.university.mapper.MajorMapper;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.service.CreditProgressService;
import com.smart.university.service.GraduationRiskService;
import com.smart.university.service.StatisticsSnapshotService;
import com.smart.university.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 毕业风险预测服务实现（I-01）
 *
 * <p>C8 红线：跨学期累计学分与完成度一律复用 W3 已交付的 {@link CreditProgressService}，
 * 本服务不拼任何带 semester_id 的学分查询，保证 36.1 / 36.2 / 40.1 三处数值完全一致。
 */
@Service
@RequiredArgsConstructor
public class GraduationRiskServiceImpl implements GraduationRiskService {

    /**
     * 无培养方案时的说明文案
     */
    private static final String NO_PLAN_MESSAGE = "未配置本专业本年级培养方案，暂无法评估毕业风险";

    /**
     * 学期最大学分未配置时的说明文案
     */
    private static final String NO_MAX_CREDIT_MESSAGE = "学期最大学分未配置，无法评估毕业风险";

    private final CreditProgressService creditProgressService;
    private final StatisticsSnapshotService statisticsSnapshotService;
    private final SemesterService semesterService;
    private final StudentMapper studentMapper;
    private final GradeCohortMapper gradeCohortMapper;
    private final MajorMapper majorMapper;

    /**
     * 默认学制（年）
     */
    @Value("${smart-university.statistics.graduation-risk.default-school-years:4}")
    private int defaultSchoolYears;

    @Override
    public GraduationRiskRespDTO getMyGraduationRisk(Long semesterId) {
        Long studentId = UserContextHolder.getStudentId();
        if (studentId == null) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_EXIST);
        }
        return getGraduationRiskByStudentId(studentId, semesterId);
    }

    @Override
    public GraduationRiskRespDTO getGraduationRiskByStudentId(Long studentId, Long semesterId) {
        if (studentId == null) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_EXIST);
        }
        Long targetSemesterId = resolveSemesterId(semesterId);
        CreditProgressRespDTO creditProgress = creditProgressService
                .getCreditProgressByStudentId(studentId, targetSemesterId);
        int remainingSemesters = resolveRemainingSemesters(studentId);
        BigDecimal maxCreditPerSemester = resolveMaxCreditPerSemester(targetSemesterId);
        return buildRespDTO(studentId, targetSemesterId, creditProgress, remainingSemesters,
                maxCreditPerSemester, false);
    }

    @Override
    public GraduationRiskPageRespDTO pageGraduationRisk(GraduationRiskQueryReqDTO requestParam) {
        Long targetSemesterId = resolveSemesterId(requestParam.getSemesterId());
        BigDecimal maxCreditPerSemester = resolveMaxCreditPerSemester(targetSemesterId);
        // 一次读取（暖路径命中当日快照 / 冷路径实时构建并回写）全量学分进度快照，消除逐学生 N+1 跨学期扫描
        List<StatCreditProgressDO> snapList = statisticsSnapshotService.getCreditProgressBySemester(targetSemesterId);
        // 按专业 / 年级过滤（入参为空表示不过滤）
        List<StatCreditProgressDO> filteredSnapList = snapList.stream()
                .filter(each -> requestParam.getMajorId() == null
                        || Objects.equals(each.getMajorId(), requestParam.getMajorId()))
                .filter(each -> requestParam.getGradeId() == null
                        || Objects.equals(each.getGradeId(), requestParam.getGradeId()))
                .toList();
        // 批量查询年级入学年份一次，供剩余学期数计算复用
        List<Long> gradeIds = filteredSnapList.stream()
                .map(StatCreditProgressDO::getGradeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, GradeCohortDO> gradeMap = gradeIds.isEmpty()
                ? Map.of()
                : gradeCohortMapper.listGradeCohortByIds(gradeIds).stream()
                .collect(Collectors.toMap(GradeCohortDO::getId, Function.identity(), (first, second) -> first));
        List<GraduationRiskRespDTO> allRecords = new ArrayList<>();
        for (StatCreditProgressDO each : filteredSnapList) {
            Integer entryYear = null;
            GradeCohortDO gradeCohortDO = each.getGradeId() == null ? null : gradeMap.get(each.getGradeId());
            if (gradeCohortDO != null) {
                entryYear = gradeCohortDO.getEntryYear();
            }
            int remainingSemesters = resolveRemainingSemesters(each.getGradeId(), entryYear);
            allRecords.add(buildRespDTOFromSnapshot(each, remainingSemesters, maxCreditPerSemester));
        }
        fillStudentInfo(allRecords, filteredSnapList);
        GraduationRiskExtraRespDTO extra = buildExtra(targetSemesterId, allRecords);
        RiskLevelEnum riskLevelFilter = EnumParseUtil.parseOrNull(RiskLevelEnum.class, requestParam.getRiskLevel());
        List<GraduationRiskRespDTO> filteredRecords = riskLevelFilter == null
                ? allRecords
                : allRecords.stream()
                .filter(each -> riskLevelFilter.name().equals(each.getRiskLevel()))
                .toList();
        List<GraduationRiskRespDTO> sortedRecords = new ArrayList<>(filteredRecords);
        sortedRecords.sort(Comparator
                .comparing((GraduationRiskRespDTO each) -> resolveRiskWeight(each.getRiskLevel()),
                        Comparator.reverseOrder())
                .thenComparing(each -> defaultZero(each.getCompletedCredit())));
        int currentPage = requestParam.getCurrentPage();
        int limit = requestParam.getLimit();
        GraduationRiskPageRespDTO result = new GraduationRiskPageRespDTO();
        result.setRecords(paginate(sortedRecords, currentPage, limit));
        result.setPage(currentPage);
        result.setPageSize(limit);
        result.setTotal(sortedRecords.size());
        result.setExtra(extra);
        return result;
    }

    @Override
    public RiskLevelEnum getRiskLevel(Long studentId, Long semesterId) {
        return creditProgressService.getRiskLevel(studentId, semesterId, null);
    }

    /**
     * 组装毕业风险预测出参
     *
     * @param studentId            学生 ID
     * @param semesterId           统计截止学期 ID
     * @param creditProgress       学分进度（跨学期累计，C8）
     * @param remainingSemesters   剩余学期数
     * @param maxCreditPerSemester 学期最大学分
     * @param withStudentBase      是否填充学生基础信息
     * @return 毕业风险预测出参
     */
    private GraduationRiskRespDTO buildRespDTO(Long studentId, Long semesterId,
                                               CreditProgressRespDTO creditProgress,
                                               int remainingSemesters, BigDecimal maxCreditPerSemester,
                                               boolean withStudentBase) {
        GraduationRiskRespDTO result = new GraduationRiskRespDTO();
        result.setSemesterId(semesterId);
        if (withStudentBase) {
            result.setStudentId(studentId);
        }
        boolean hasPlan = Boolean.TRUE.equals(creditProgress.getHasPlan());
        result.setHasPlan(hasPlan);
        result.setCompletedCredit(creditProgress.getCompletedCredit());
        result.setPassedCredit(creditProgress.getPassedCredit());
        result.setPlanTotalCredit(creditProgress.getPlanTotalCredit());
        result.setProgressRate(creditProgress.getProgressRate());
        result.setRemainingSemesters(remainingSemesters);
        result.setMaxCreditPerSemester(maxCreditPerSemester);
        result.setGaps(buildGaps(creditProgress));
        BigDecimal remainingCredit = defaultZero(creditProgress.getPlanTotalCredit())
                .subtract(defaultZero(creditProgress.getCompletedCredit()));
        result.setRemainingCredit(scaleOne(remainingCredit.compareTo(BigDecimal.ZERO) > 0
                ? remainingCredit
                : BigDecimal.ZERO));
        BigDecimal requiredCreditPerSemester = scaleOne(remainingCredit
                .divide(BigDecimal.valueOf(Math.max(remainingSemesters, 1)), 1, RoundingMode.HALF_UP));
        result.setRequiredCreditPerSemester(requiredCreditPerSemester);
        BigDecimal requiredRate = calculateRequiredRate(requiredCreditPerSemester, maxCreditPerSemester);
        result.setRequiredRate(requiredRate);
        RiskLevelEnum riskLevel = creditProgressService.getRiskLevel(studentId, semesterId, creditProgress);
        result.setRiskLevel(riskLevel.name());
        if (!hasPlan) {
            result.setMessage(NO_PLAN_MESSAGE);
            result.setRiskReason(NO_PLAN_MESSAGE);
            return result;
        }
        result.setMessage("");
        if (maxCreditPerSemester.compareTo(BigDecimal.ZERO) <= 0) {
            result.setRiskReason(NO_MAX_CREDIT_MESSAGE);
            return result;
        }
        result.setRiskReason(buildRiskReason(requiredCreditPerSemester, requiredRate));
        return result;
    }

    /**
     * 构建按课程类型的学分缺口
     *
     * @param creditProgress 学分进度
     * @return 学分缺口集合
     */
    private List<GraduationRiskGapRespDTO> buildGaps(CreditProgressRespDTO creditProgress) {
        if (creditProgress.getItems() == null) {
            return List.of();
        }
        List<GraduationRiskGapRespDTO> result = new ArrayList<>();
        for (CreditProgressItemRespDTO each : creditProgress.getItems()) {
            GraduationRiskGapRespDTO gap = new GraduationRiskGapRespDTO();
            gap.setCourseType(each.getCourseType());
            gap.setCourseTypeName(each.getCourseTypeName());
            gap.setGapCredit(each.getGapCredit());
            result.add(gap);
        }
        return result;
    }

    /**
     * 从学分进度快照直接组装毕业风险预测出参，不再触发逐学生跨学期扫描
     *
     * <p>completedCredit / passedCredit / planTotalCredit / progressRate / hasPlan / riskLevel 均直接取自快照，
     * 与 {@link #buildRespDTO} 保持数值一致；riskLevel 取快照已算好的值，避免再次 N+1 调用风险计算。
     *
     * @param snap                 学分进度快照
     * @param remainingSemesters   剩余学期数
     * @param maxCreditPerSemester 学期最大学分
     * @return 毕业风险预测出参
     */
    private GraduationRiskRespDTO buildRespDTOFromSnapshot(StatCreditProgressDO snap, int remainingSemesters,
                                                          BigDecimal maxCreditPerSemester) {
        GraduationRiskRespDTO result = new GraduationRiskRespDTO();
        result.setSemesterId(snap.getSemesterId());
        result.setStudentId(snap.getStudentId());
        result.setStudentNo(snap.getStudentNo());
        result.setStudentName(snap.getStudentName());
        boolean hasPlan = Integer.valueOf(1).equals(snap.getHasPlan());
        result.setHasPlan(hasPlan);
        result.setCompletedCredit(snap.getCompletedCredit());
        result.setPassedCredit(snap.getPassedCredit());
        result.setPlanTotalCredit(snap.getPlanTotalCredit());
        result.setProgressRate(snap.getProgressRate());
        result.setRemainingSemesters(remainingSemesters);
        result.setMaxCreditPerSemester(maxCreditPerSemester);
        result.setGaps(buildGapsFromSnapshot(snap));
        BigDecimal remainingCredit = defaultZero(snap.getPlanTotalCredit())
                .subtract(defaultZero(snap.getCompletedCredit()));
        result.setRemainingCredit(scaleOne(remainingCredit.compareTo(BigDecimal.ZERO) > 0
                ? remainingCredit
                : BigDecimal.ZERO));
        BigDecimal requiredCreditPerSemester = scaleOne(remainingCredit
                .divide(BigDecimal.valueOf(Math.max(remainingSemesters, 1)), 1, RoundingMode.HALF_UP));
        result.setRequiredCreditPerSemester(requiredCreditPerSemester);
        BigDecimal requiredRate = calculateRequiredRate(requiredCreditPerSemester, maxCreditPerSemester);
        result.setRequiredRate(requiredRate);
        RiskLevelEnum riskLevel = snap.getRiskLevel() == null ? RiskLevelEnum.LOW : snap.getRiskLevel();
        result.setRiskLevel(riskLevel.name());
        if (!hasPlan) {
            result.setMessage(NO_PLAN_MESSAGE);
            result.setRiskReason(NO_PLAN_MESSAGE);
            return result;
        }
        result.setMessage("");
        if (maxCreditPerSemester.compareTo(BigDecimal.ZERO) <= 0) {
            result.setRiskReason(NO_MAX_CREDIT_MESSAGE);
            return result;
        }
        result.setRiskReason(buildRiskReason(requiredCreditPerSemester, requiredRate));
        return result;
    }

    /**
     * 从快照列构建按课程类型的学分缺口，顺序与 {@link #buildGaps} 一致（REQUIRED / MAJOR_ELECTIVE / PUBLIC_ELECTIVE）
     *
     * @param snap 学分进度快照
     * @return 学分缺口集合
     */
    private List<GraduationRiskGapRespDTO> buildGapsFromSnapshot(StatCreditProgressDO snap) {
        List<GraduationRiskGapRespDTO> result = new ArrayList<>();
        for (CourseTypeEnum each : CourseTypeEnum.values()) {
            BigDecimal completed;
            BigDecimal required;
            switch (each) {
                case REQUIRED -> {
                    completed = snap.getRequiredCredit();
                    required = snap.getPlanRequiredCredit();
                }
                case MAJOR_ELECTIVE -> {
                    completed = snap.getMajorElectiveCredit();
                    required = snap.getPlanMajorElectiveCredit();
                }
                case PUBLIC_ELECTIVE -> {
                    completed = snap.getPublicElectiveCredit();
                    required = snap.getPlanPublicElectiveCredit();
                }
                default -> {
                    completed = BigDecimal.ZERO;
                    required = BigDecimal.ZERO;
                }
            }
            GraduationRiskGapRespDTO gap = new GraduationRiskGapRespDTO();
            gap.setCourseType(each.getCode());
            gap.setCourseTypeName(each.getDescription());
            BigDecimal gapCredit = defaultZero(required).subtract(defaultZero(completed));
            gap.setGapCredit(scaleOne(gapCredit.compareTo(BigDecimal.ZERO) > 0 ? gapCredit : BigDecimal.ZERO));
            result.add(gap);
        }
        return result;
    }

    /**
     * 构建风险原因文案
     *
     * @param requiredCreditPerSemester 每学期需修学分
     * @param requiredRate              需求速率
     * @return 风险原因
     */
    private String buildRiskReason(BigDecimal requiredCreditPerSemester, BigDecimal requiredRate) {
        BigDecimal ratePercent = requiredRate.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP);
        return "按当前进度，剩余学期需每学期修满 " + requiredCreditPerSemester + " 学分，达到学期上限的 "
                + ratePercent.stripTrailingZeros().toPlainString() + "%";
    }

    /**
     * 计算需求速率 = 每学期需修学分 / 学期最大学分上限
     *
     * @param requiredCreditPerSemester 每学期需修学分
     * @param maxCreditPerSemester      学期最大学分
     * @return 需求速率，保留 2 位小数
     */
    private BigDecimal calculateRequiredRate(BigDecimal requiredCreditPerSemester, BigDecimal maxCreditPerSemester) {
        if (maxCreditPerSemester.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return requiredCreditPerSemester
                .divide(maxCreditPerSemester, 2, RoundingMode.HALF_UP);
    }

    /**
     * 构建口径回显，统计各风险等级与无方案人数
     *
     * @param semesterId 学期 ID
     * @param records    全部学生记录（未做风险等级过滤）
     * @return 口径回显
     */
    private GraduationRiskExtraRespDTO buildExtra(Long semesterId, List<GraduationRiskRespDTO> records) {
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;
        int noPlanCount = 0;
        for (GraduationRiskRespDTO each : records) {
            if (!Boolean.TRUE.equals(each.getHasPlan())) {
                noPlanCount++;
                continue;
            }
            if (RiskLevelEnum.HIGH.name().equals(each.getRiskLevel())) {
                highCount++;
            } else if (RiskLevelEnum.MEDIUM.name().equals(each.getRiskLevel())) {
                mediumCount++;
            } else {
                lowCount++;
            }
        }
        GraduationRiskExtraRespDTO result = new GraduationRiskExtraRespDTO();
        result.setSemesterId(semesterId);
        result.setHighCount(highCount);
        result.setMediumCount(mediumCount);
        result.setLowCount(lowCount);
        result.setNoPlanCount(noPlanCount);
        return result;
    }

    /**
     * 填充学生基础信息：学号、姓名、专业名称、年级名称
     *
     * @param records        风险记录集合
     * @param studentDOList  学生数据对象集合
     */
    private void fillStudentInfo(List<GraduationRiskRespDTO> records, List<StatCreditProgressDO> snapList) {
        List<Long> majorIds = snapList.stream().map(StatCreditProgressDO::getMajorId).filter(Objects::nonNull).distinct().toList();
        List<Long> gradeIds = snapList.stream().map(StatCreditProgressDO::getGradeId).filter(Objects::nonNull).distinct().toList();
        Map<Long, MajorDO> majorMap = majorIds.isEmpty()
                ? Map.of()
                : majorMapper.listMajorByIds(majorIds).stream()
                .collect(Collectors.toMap(MajorDO::getId, Function.identity(), (first, second) -> first));
        Map<Long, GradeCohortDO> gradeMap = gradeIds.isEmpty()
                ? Map.of()
                : gradeCohortMapper.listGradeCohortByIds(gradeIds).stream()
                .collect(Collectors.toMap(GradeCohortDO::getId, Function.identity(), (first, second) -> first));
        Map<Long, StatCreditProgressDO> snapMap = snapList.stream()
                .collect(Collectors.toMap(StatCreditProgressDO::getStudentId, Function.identity(), (first, second) -> first));
        for (GraduationRiskRespDTO each : records) {
            StatCreditProgressDO snap = snapMap.get(each.getStudentId());
            if (snap == null) {
                continue;
            }
            MajorDO majorDO = majorMap.get(snap.getMajorId());
            each.setMajorName(majorDO == null ? null : majorDO.getMajorName());
            GradeCohortDO gradeCohortDO = gradeMap.get(snap.getGradeId());
            each.setGradeName(gradeCohortDO == null ? null : gradeCohortDO.getGradeName());
        }
    }

    /**
     * 计算剩余学期数：学制 − (当前学年 − 入学年份)，最小为 1
     *
     * @param studentId 学生 ID
     * @return 剩余学期数
     */
    private int resolveRemainingSemesters(Long studentId) {
        StudentDO studentDO = studentMapper.getStudentById(studentId);
        Long gradeId = studentDO == null ? null : studentDO.getGradeId();
        Integer entryYear = null;
        if (gradeId != null) {
            GradeCohortDO gradeCohortDO = gradeCohortMapper.getGradeCohortById(gradeId);
            if (gradeCohortDO != null) {
                entryYear = gradeCohortDO.getEntryYear();
            }
        }
        return resolveRemainingSemesters(gradeId, entryYear);
    }

    /**
     * 计算剩余学期数：学制 − (当前学年 − 入学年份)，最小为 1
     *
     * <p>供 {@link #pageGraduationRisk} 在批量已知年级入学年份时直接复用，避免逐学生回查。
     *
     * @param gradeId    年级 ID，可为 null
     * @param entryYear  入学年份，为空时按当前年份兜底
     * @return 剩余学期数
     */
    private int resolveRemainingSemesters(Long gradeId, Integer entryYear) {
        int entry = entryYear != null ? entryYear : LocalDate.now().getYear();
        int remaining = defaultSchoolYears - (LocalDate.now().getYear() - entry);
        return remaining < 1 ? 1 : remaining;
    }

    /**
     * 取学期最大学分上限
     *
     * @param semesterId 学期 ID
     * @return 最大学分，未配置时返回 0
     */
    private BigDecimal resolveMaxCreditPerSemester(Long semesterId) {
        SemesterDO semesterDO = semesterService.getSemesterById(semesterId);
        if (semesterDO == null || semesterDO.getMaxSelectionCredit() == null) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return defaultZero(semesterDO.getMaxSelectionCredit());
    }

    /**
     * 风险等级排序权重，数值越大越靠前
     *
     * @param riskLevel 风险等级
     * @return 排序权重
     */
    private int resolveRiskWeight(String riskLevel) {
        if (RiskLevelEnum.HIGH.name().equals(riskLevel)) {
            return 3;
        }
        if (RiskLevelEnum.MEDIUM.name().equals(riskLevel)) {
            return 2;
        }
        return 1;
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
     * 空值兜底为 0
     *
     * @param credit 学分
     * @return 非空学分
     */
    private BigDecimal defaultZero(BigDecimal credit) {
        return credit == null ? BigDecimal.ZERO : credit;
    }

    /**
     * 保留 1 位小数
     *
     * @param credit 学分
     * @return 标度为 1 的学分
     */
    private BigDecimal scaleOne(BigDecimal credit) {
        return defaultZero(credit).setScale(1, RoundingMode.HALF_UP);
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
}
