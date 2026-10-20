package com.smart.university.service.impl;

import com.smart.university.common.base.SnapshotResult;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.enums.StatTypeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.dto.resp.CreditProgressRespDTO;
import com.smart.university.domain.dto.resp.SnapshotRefreshRespDTO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.ScoreDO;
import com.smart.university.domain.entity.SelectionBatchDO;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.entity.StatClassroomUsageDO;
import com.smart.university.domain.entity.StatCourseSelectionDO;
import com.smart.university.domain.entity.StatCreditProgressDO;
import com.smart.university.domain.entity.StatSemesterOverviewDO;
import com.smart.university.domain.entity.StatTeacherWorkloadDO;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.enums.ScoreStatusEnum;
import com.smart.university.domain.enums.SelectionBatchStatusEnum;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.domain.enums.TeachingClassStatusEnum;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.ScoreMapper;
import com.smart.university.mapper.SelectionBatchMapper;
import com.smart.university.mapper.StatClassroomUsageMapper;
import com.smart.university.mapper.StatCourseSelectionMapper;
import com.smart.university.mapper.StatCreditProgressMapper;
import com.smart.university.mapper.StatSemesterOverviewMapper;
import com.smart.university.mapper.StatTeacherWorkloadMapper;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.service.ClassroomUsageService;
import com.smart.university.service.CourseSelectionAnalysisService;
import com.smart.university.service.CreditProgressService;
import com.smart.university.service.SemesterService;
import com.smart.university.service.StatisticsSnapshotService;
import com.smart.university.service.TeacherWorkloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 统计快照服务实现，统一 readOrCompute 读法：命中当日快照直接返回，未命中实时计算并回写。
 *
 * <p>覆盖写策略：先清理当日旧数据再写入，写入后清理历史日期数据，保证每学期只保留最新一份（Q12 决议 A）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsSnapshotServiceImpl implements StatisticsSnapshotService {

    private final StatClassroomUsageMapper statClassroomUsageMapper;
    private final StatTeacherWorkloadMapper statTeacherWorkloadMapper;
    private final StatCourseSelectionMapper statCourseSelectionMapper;
    private final StatCreditProgressMapper statCreditProgressMapper;
    private final StatSemesterOverviewMapper statSemesterOverviewMapper;
    private final ClassroomUsageService classroomUsageService;
    private final TeacherWorkloadService teacherWorkloadService;
    private final CourseSelectionAnalysisService courseSelectionAnalysisService;
    private final CreditProgressService creditProgressService;
    private final SemesterService semesterService;
    private final StudentMapper studentMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final CourseSelectionMapper courseSelectionMapper;
    private final SelectionBatchMapper selectionBatchMapper;
    private final ScoreMapper scoreMapper;

    @Override
    public SnapshotResult<List<StatClassroomUsageDO>> readOrComputeClassroomUsage(
            Long semesterId, Supplier<List<StatClassroomUsageDO>> computeFunc) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        LocalDate statDate = LocalDate.now();
        return readOrComputeList(
                statClassroomUsageMapper.listStatClassroomUsageBySemesterId(targetSemesterId),
                computeFunc,
                computedList -> {
                    statClassroomUsageMapper.removeStatClassroomUsageBySemesterAndDate(targetSemesterId, statDate);
                    for (StatClassroomUsageDO each : computedList) {
                        statClassroomUsageMapper.saveStatClassroomUsage(each);
                    }
                    statClassroomUsageMapper.removeStatClassroomUsageBeforeDate(targetSemesterId, statDate);
                },
                StatClassroomUsageDO::getStatDate,
                StatClassroomUsageDO::getGeneratedAt);
    }

    @Override
    public SnapshotResult<List<StatTeacherWorkloadDO>> readOrComputeTeacherWorkload(
            Long semesterId, Supplier<List<StatTeacherWorkloadDO>> computeFunc) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        LocalDate statDate = LocalDate.now();
        return readOrComputeList(
                statTeacherWorkloadMapper.listStatTeacherWorkloadBySemesterId(targetSemesterId),
                computeFunc,
                computedList -> {
                    statTeacherWorkloadMapper.removeStatTeacherWorkloadBySemesterAndDate(targetSemesterId, statDate);
                    for (StatTeacherWorkloadDO each : computedList) {
                        statTeacherWorkloadMapper.saveStatTeacherWorkload(each);
                    }
                    statTeacherWorkloadMapper.removeStatTeacherWorkloadBeforeDate(targetSemesterId, statDate);
                },
                StatTeacherWorkloadDO::getStatDate,
                StatTeacherWorkloadDO::getGeneratedAt);
    }

    @Override
    public SnapshotResult<List<StatCourseSelectionDO>> readOrComputeCourseSelection(
            Long semesterId, Supplier<List<StatCourseSelectionDO>> computeFunc) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        LocalDate statDate = LocalDate.now();
        return readOrComputeList(
                statCourseSelectionMapper.listStatCourseSelectionBySemesterId(targetSemesterId),
                computeFunc,
                computedList -> {
                    statCourseSelectionMapper.removeStatCourseSelectionBySemesterAndDate(targetSemesterId, statDate);
                    for (StatCourseSelectionDO each : computedList) {
                        statCourseSelectionMapper.saveStatCourseSelection(each);
                    }
                    statCourseSelectionMapper.removeStatCourseSelectionBeforeDate(targetSemesterId, statDate);
                },
                StatCourseSelectionDO::getStatDate,
                StatCourseSelectionDO::getGeneratedAt);
    }

    @Override
    public SnapshotResult<List<StatCreditProgressDO>> readOrComputeCreditProgress(
            Long semesterId, Supplier<List<StatCreditProgressDO>> computeFunc) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        LocalDate statDate = LocalDate.now();
        return readOrComputeList(
                statCreditProgressMapper.listStatCreditProgressBySemesterId(targetSemesterId),
                computeFunc,
                computedList -> {
                    statCreditProgressMapper.removeStatCreditProgressBySemesterAndDate(targetSemesterId, statDate);
                    for (StatCreditProgressDO each : computedList) {
                        statCreditProgressMapper.saveStatCreditProgress(each);
                    }
                    statCreditProgressMapper.removeStatCreditProgressBeforeDate(targetSemesterId, statDate);
                },
                StatCreditProgressDO::getStatDate,
                StatCreditProgressDO::getGeneratedAt);
    }

    @Override
    public List<StatCreditProgressDO> getCreditProgressBySemester(Long semesterId) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        return readOrComputeCreditProgress(targetSemesterId,
                () -> computeCreditProgressSnapshot(targetSemesterId)).getData();
    }

    /**
     * 暴露私有的快照计算逻辑，供 {@link #getCreditProgressBySemester} 在冷路径下实时构建回写
     *
     * @param semesterId 学期 ID
     * @return 学生学分进度快照集合
     */
    public List<StatCreditProgressDO> computeCreditProgressSnapshot(Long semesterId) {
        return buildCreditProgressSnapshot(semesterId);
    }

    @Override
    public SnapshotResult<StatSemesterOverviewDO> readOrComputeOverview(
            Long semesterId, Supplier<StatSemesterOverviewDO> computeFunc) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        LocalDate statDate = LocalDate.now();
        List<StatSemesterOverviewDO> todaySnapshotList =
                statSemesterOverviewMapper.listStatSemesterOverviewBySemesterId(targetSemesterId).stream()
                        .filter(each -> statDate.equals(each.getStatDate()))
                        .toList();
        if (!todaySnapshotList.isEmpty()) {
            StatSemesterOverviewDO latest = todaySnapshotList.stream()
                    .max(Comparator.comparing(StatSemesterOverviewDO::getGeneratedAt))
                    .orElse(todaySnapshotList.get(0));
            return SnapshotResult.hit(latest, latest.getGeneratedAt());
        }
        StatSemesterOverviewDO computed = computeFunc.get();
        LocalDateTime generatedAt = LocalDateTime.now();
        if (computed == null) {
            return SnapshotResult.computed(null, generatedAt);
        }
        statSemesterOverviewMapper.removeStatSemesterOverviewBySemesterAndDate(targetSemesterId, statDate);
        statSemesterOverviewMapper.saveStatSemesterOverview(computed);
        statSemesterOverviewMapper.removeStatSemesterOverviewBeforeDate(targetSemesterId, statDate);
        return SnapshotResult.computed(computed,
                computed.getGeneratedAt() == null ? generatedAt : computed.getGeneratedAt());
    }

    @Override
    public StatSemesterOverviewDO buildOverviewSnapshot(Long semesterId) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        SemesterDO semesterDO = semesterService.getSemesterById(targetSemesterId);
        List<TeachingClassDO> teachingClassDOList = teachingClassMapper
                .listTeachingClassBySemesterId(targetSemesterId).stream()
                .filter(each -> TeachingClassStatusEnum.CANCELLED != each.getStatus())
                .toList();
        SelectionBatchDO selectionBatchDO = selectionBatchMapper.getCurrentSelectionBatch(targetSemesterId);
        List<CourseSelectionDO> selectedList = listSemesterSelection(targetSemesterId, SelectionStatusEnum.SELECTED);
        List<CourseSelectionDO> waitingList = listSemesterSelection(targetSemesterId, SelectionStatusEnum.WAITING);
        List<CourseSelectionDO> withdrawList = listSemesterSelection(targetSemesterId, SelectionStatusEnum.WITHDRAWN);
        StatSemesterOverviewDO result = new StatSemesterOverviewDO();
        result.setSemesterId(targetSemesterId);
        result.setSemesterCode(semesterDO == null || semesterDO.getSemesterCode() == null
                ? ""
                : semesterDO.getSemesterCode());
        result.setStudentCount((int) studentMapper.countStudentByStatus(1));
        result.setCourseCount((int) teachingClassDOList.stream()
                .filter(each -> TeachingClassStatusEnum.AVAILABLE == each.getStatus()
                        || TeachingClassStatusEnum.CLOSED == each.getStatus())
                .map(TeachingClassDO::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .count());
        result.setTeachingClassCount(teachingClassDOList.size());
        result.setTeacherCount((int) teachingClassDOList.stream()
                .map(TeachingClassDO::getTeacherId)
                .filter(Objects::nonNull)
                .distinct()
                .count());
        if (selectionBatchDO == null) {
            result.setSelectionCount(selectedList.size());
            result.setWaitlistCount(waitingList.size());
            result.setWithdrawCount(withdrawList.size());
            result.setBatchStatus(SelectionBatchStatusEnum.NOT_STARTED);
        } else {
            result.setSelectionCount((int) courseSelectionMapper.countSelectedByBatchId(selectionBatchDO.getId()));
            result.setWaitlistCount((int) courseSelectionMapper
                    .countByBatchIdAndStatus(selectionBatchDO.getId(), SelectionStatusEnum.WAITING));
            result.setWithdrawCount((int) courseSelectionMapper
                    .countByBatchIdAndStatus(selectionBatchDO.getId(), SelectionStatusEnum.WITHDRAWN));
            result.setBatchStatus(selectionBatchDO.getStatus());
        }
        result.setFullClassRate(courseSelectionAnalysisService.getFullClassRate(targetSemesterId));
        result.setClassroomUsageRate(classroomUsageService.getUsageExtra(targetSemesterId).getOverallUsageRate());
        result.setAvgTeacherWorkload(teacherWorkloadService.getAverageWorkload(targetSemesterId));
        result.setAvgCreditProgressRate(calculateAvgCreditProgressRate(targetSemesterId));
        result.setUnpublishedClassCount(countUnpublishedClass(selectedList));
        result.setStatDate(LocalDate.now());
        result.setGeneratedAt(LocalDateTime.now());
        return result;
    }

    @Override
    public SnapshotRefreshRespDTO refreshSnapshot(Long semesterId, List<StatTypeEnum> statTypes) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        List<StatTypeEnum> targetStatTypes = statTypes == null || statTypes.isEmpty()
                ? List.of(StatTypeEnum.values())
                : statTypes;
        LocalDateTime startTime = LocalDateTime.now();
        int successCount = 0;
        int failCount = 0;
        for (StatTypeEnum each : targetStatTypes) {
            try {
                refreshSingleSnapshot(targetSemesterId, each);
                successCount++;
            } catch (Exception ex) {
                failCount++;
                log.error("统计快照刷新失败，学期ID：{}，统计类型：{}，异常摘要：{}",
                        targetSemesterId, each.getCode(), ex.getMessage());
            }
        }
        if (successCount == 0 && failCount > 0) {
            throw new BizException(ResultCodeEnum.STAT_SNAPSHOT_GENERATE_FAILED);
        }
        SnapshotRefreshRespDTO result = new SnapshotRefreshRespDTO();
        result.setSemesterId(targetSemesterId);
        result.setStatTypes(targetStatTypes.stream().map(StatTypeEnum::getCode).toList());
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setDurationMs(Duration.between(startTime, LocalDateTime.now()).toMillis());
        result.setGeneratedAt(LocalDateTime.now());
        return result;
    }

    /**
     * 刷新单个统计类型的快照
     *
     * @param semesterId 学期 ID
     * @param statType   统计类型
     */
    private void refreshSingleSnapshot(Long semesterId, StatTypeEnum statType) {
        switch (statType) {
            case CLASSROOM_USAGE -> readOrComputeClassroomUsage(semesterId,
                    () -> classroomUsageService.listClassroomUsageSnapshot(semesterId));
            case TEACHER_WORKLOAD -> readOrComputeTeacherWorkload(semesterId,
                    () -> teacherWorkloadService.listTeacherWorkloadSnapshot(semesterId));
            case COURSE_SELECTION -> readOrComputeCourseSelection(semesterId,
                    () -> courseSelectionAnalysisService.listCourseSelectionSnapshot(semesterId));
            case CREDIT_PROGRESS -> readOrComputeCreditProgress(semesterId,
                    () -> buildCreditProgressSnapshot(semesterId));
            case OVERVIEW -> readOrComputeOverview(semesterId, () -> buildOverviewSnapshot(semesterId));
        }
    }

    /**
     * 构建学生学分进度快照，逐个学生复用 S-02 的跨学期累计结果
     *
     * @param semesterId 学期 ID
     * @return 学分进度快照集合
     */
    private List<StatCreditProgressDO> buildCreditProgressSnapshot(Long semesterId) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        LocalDate statDate = LocalDate.now();
        LocalDateTime generatedAt = LocalDateTime.now();
        List<StudentDO> studentDOList = studentMapper.listStudentByMajorAndGrade(null, null);
        List<StatCreditProgressDO> result = new ArrayList<>();
        for (StudentDO each : studentDOList) {
            CreditProgressRespDTO creditProgress = creditProgressService
                    .getCreditProgressByStudentId(each.getId(), targetSemesterId);
            StatCreditProgressDO snapshot = new StatCreditProgressDO();
            snapshot.setSemesterId(targetSemesterId);
            snapshot.setStudentId(each.getId());
            snapshot.setStudentNo(each.getStudentNo() == null ? "" : each.getStudentNo());
            snapshot.setStudentName(each.getStudentName() == null ? "" : each.getStudentName());
            snapshot.setMajorId(each.getMajorId());
            snapshot.setGradeId(each.getGradeId());
            snapshot.setHasPlan(Boolean.TRUE.equals(creditProgress.getHasPlan()) ? 1 : 0);
            snapshot.setCompletedCredit(creditProgress.getCompletedCredit());
            snapshot.setPassedCredit(creditProgress.getPassedCredit());
            snapshot.setPlanTotalCredit(creditProgress.getPlanTotalCredit());
            snapshot.setProgressRate(creditProgress.getProgressRate());
            snapshot.setRequiredCredit(sumItemCredit(creditProgress, "REQUIRED"));
            snapshot.setMajorElectiveCredit(sumItemCredit(creditProgress, "MAJOR_ELECTIVE"));
            snapshot.setPublicElectiveCredit(sumItemCredit(creditProgress, "PUBLIC_ELECTIVE"));
            snapshot.setPlanRequiredCredit(sumPlanCredit(creditProgress, "REQUIRED"));
            snapshot.setPlanMajorElectiveCredit(sumPlanCredit(creditProgress, "MAJOR_ELECTIVE"));
            snapshot.setPlanPublicElectiveCredit(sumPlanCredit(creditProgress, "PUBLIC_ELECTIVE"));
            snapshot.setRiskLevel(creditProgressService.getRiskLevel(each.getId(), targetSemesterId, creditProgress));
            snapshot.setStatDate(statDate);
            snapshot.setGeneratedAt(generatedAt);
            result.add(snapshot);
        }
        return result;
    }

    /**
     * 取指定课程类型的已完成学分
     *
     * @param creditProgress 学分进度
     * @param courseType     课程类型编码
     * @return 已完成学分
     */
    private BigDecimal sumItemCredit(CreditProgressRespDTO creditProgress, String courseType) {
        return sumCredit(creditProgress, courseType, true);
    }

    /**
     * 取指定课程类型的培养方案要求学分
     *
     * @param creditProgress 学分进度
     * @param courseType     课程类型编码
     * @return 要求学分
     */
    private BigDecimal sumPlanCredit(CreditProgressRespDTO creditProgress, String courseType) {
        return sumCredit(creditProgress, courseType, false);
    }

    /**
     * 按课程类型汇总学分
     *
     * @param creditProgress 学分进度
     * @param courseType     课程类型编码
     * @param completed      true 取已完成学分，false 取要求学分
     * @return 学分合计
     */
    private BigDecimal sumCredit(CreditProgressRespDTO creditProgress, String courseType, boolean completed) {
        if (creditProgress.getItems() == null) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return creditProgress.getItems().stream()
                .filter(each -> courseType.equals(each.getCourseType()))
                .map(each -> completed ? each.getCompletedCredit() : each.getRequiredCredit())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 计算平均学分完成度，仅统计存在培养方案的学生
     *
     * @param semesterId 学期 ID
     * @return 平均完成度百分比，保留 1 位小数
     */
    private BigDecimal calculateAvgCreditProgressRate(Long semesterId) {
        List<BigDecimal> rateList = statCreditProgressMapper.listStatCreditProgressBySemesterId(semesterId).stream()
                .filter(each -> Integer.valueOf(1).equals(each.getHasPlan()))
                .map(StatCreditProgressDO::getProgressRate)
                .filter(Objects::nonNull)
                .toList();
        if (rateList.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        BigDecimal total = rateList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(rateList.size()), 1, RoundingMode.HALF_UP);
    }

    /**
     * 统计存在已选记录但成绩未发布的教学班数
     *
     * @param selectedList 本学期已选记录集合
     * @return 教学班数
     */
    private int countUnpublishedClass(List<CourseSelectionDO> selectedList) {
        if (selectedList.isEmpty()) {
            return 0;
        }
        List<Long> selectionIds = selectedList.stream().map(CourseSelectionDO::getId).toList();
        Map<Long, ScoreDO> scoreMap = scoreMapper.listScoreBySelectionIds(selectionIds).stream()
                .collect(Collectors.toMap(ScoreDO::getCourseSelectionId, Function.identity(),
                        (first, second) -> first));
        Set<Long> teachingClassIdSet = new HashSet<>();
        for (CourseSelectionDO each : selectedList) {
            ScoreDO scoreDO = scoreMap.get(each.getId());
            if (scoreDO == null || ScoreStatusEnum.UNPUBLISHED == scoreDO.getStatus()) {
                teachingClassIdSet.add(each.getTeachingClassId());
            }
        }
        return teachingClassIdSet.size();
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
     * 列表型快照的统一读法
     *
     * @param allSnapshotList   该学期的全部快照
     * @param computeFunc       实时计算函数
     * @param saveFunc          回写函数
     * @param statDateGetter    快照日期取值函数
     * @param generatedAtGetter 生成时间取值函数
     * @param <T>               快照数据类型
     * @return 快照读取结果
     */
    private <T> SnapshotResult<List<T>> readOrComputeList(List<T> allSnapshotList,
                                                          Supplier<List<T>> computeFunc,
                                                          Consumer<List<T>> saveFunc,
                                                          Function<T, LocalDate> statDateGetter,
                                                          Function<T, LocalDateTime> generatedAtGetter) {
        LocalDate statDate = LocalDate.now();
        List<T> todaySnapshotList = allSnapshotList.stream()
                .filter(each -> statDate.equals(statDateGetter.apply(each)))
                .toList();
        if (!todaySnapshotList.isEmpty()) {
            LocalDateTime latestGeneratedAt = todaySnapshotList.stream()
                    .map(generatedAtGetter)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(LocalDateTime.now());
            return SnapshotResult.hit(new ArrayList<>(todaySnapshotList), latestGeneratedAt);
        }
        List<T> computedList = computeFunc.get();
        LocalDateTime generatedAt = LocalDateTime.now();
        if (computedList == null || computedList.isEmpty()) {
            return SnapshotResult.computed(List.of(), generatedAt);
        }
        saveFunc.accept(computedList);
        return SnapshotResult.computed(computedList, generatedAt);
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
}
