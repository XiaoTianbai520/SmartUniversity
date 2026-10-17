package com.smart.university.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.university.common.context.TaskContext;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.enums.TaskCodeEnum;
import com.smart.university.common.enums.TriggerTypeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.common.util.ScheduleTaskLogUtil;
import com.smart.university.domain.dto.req.SemesterPageQueryReqDTO;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.entity.ScoreDO;
import com.smart.university.domain.entity.SemesterArchiveDO;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.domain.enums.SemesterStatusEnum;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.ScoreMapper;
import com.smart.university.mapper.SemesterArchiveMapper;
import com.smart.university.mapper.SemesterMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.service.StatisticsSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学期数据归档定时任务（A-09）
 *
 * <p>仅归档 `status='FINISHED'` 的学期，未结束的学期归档时抛
 * {@link ResultCodeEnum#SEMESTER_ARCHIVE_NOT_ALLOWED}；归档内容按「学期 + 归档类型」覆盖写，幂等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SemesterArchiveTask {

    /**
     * 归档类型：选课
     */
    private static final String ARCHIVE_SELECTION = "SELECTION";

    /**
     * 归档类型：成绩
     */
    private static final String ARCHIVE_SCORE = "SCORE";

    /**
     * 归档类型：看板总览
     */
    private static final String ARCHIVE_OVERVIEW = "OVERVIEW";

    /**
     * 学期扫描上限
     */
    private static final int SEMESTER_SCAN_LIMIT = 200;

    private final SemesterMapper semesterMapper;

    private final TeachingClassMapper teachingClassMapper;

    private final CourseSelectionMapper courseSelectionMapper;

    private final ScoreMapper scoreMapper;

    private final SemesterArchiveMapper semesterArchiveMapper;

    private final StatisticsSnapshotService statisticsSnapshotService;

    private final ObjectMapper objectMapper;

    private final ScheduleTaskLogUtil scheduleTaskLogUtil;

    /**
     * 任务开关，默认关闭
     */
    @Value("${smart-university.schedule.tasks.semester-archive.enabled:false}")
    private boolean enabled;

    /**
     * 定时归档已结束学期，开关关闭时直接跳过
     */
    @Scheduled(cron = "${smart-university.schedule.tasks.semester-archive.cron:0 0 4 * * ?}")
    public void execute() {
        if (!enabled) {
            return;
        }
        scheduleTaskLogUtil.runWithLog(TaskCodeEnum.SEMESTER_ARCHIVE_TASK, TriggerTypeEnum.SCHEDULED, this::doExecute);
    }

    /**
     * 手工触发入口，供教务端手动执行任务复用
     *
     * @return 任务执行上下文
     */
    public TaskContext runManually() {
        return doExecute();
    }

    /**
     * 归档全部已结束学期的三类数据，单条失败跳过并记录异常摘要
     *
     * @return 任务执行上下文
     */
    public TaskContext doExecute() {
        TaskContext context = TaskContext.empty();
        List<SemesterDO> semesterDOList = listFinishedSemester();
        for (SemesterDO each : semesterDOList) {
            for (String archiveType : List.of(ARCHIVE_SELECTION, ARCHIVE_SCORE, ARCHIVE_OVERVIEW)) {
                context.addScanCount(1);
                try {
                    archiveSemester(each.getId(), archiveType);
                    context.addSuccessCount(1);
                } catch (Exception ex) {
                    context.addFailCount(1);
                    context.appendError("归档失败，学期ID：" + each.getId() + "，归档类型：" + archiveType
                            + "，" + ex.getMessage());
                    log.error("学期归档失败，学期ID：{}，归档类型：{}", each.getId(), archiveType, ex);
                }
            }
        }
        return context;
    }

    /**
     * 归档单个学期的指定类型，覆盖写且幂等
     *
     * @param semesterId  学期 ID
     * @param archiveType 归档类型
     */
    public void archiveSemester(Long semesterId, String archiveType) {
        SemesterDO semesterDO = semesterMapper.getSemesterById(semesterId);
        if (semesterDO == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "学期不存在");
        }
        if (SemesterStatusEnum.FINISHED != semesterDO.getStatus()) {
            throw new BizException(ResultCodeEnum.SEMESTER_ARCHIVE_NOT_ALLOWED);
        }
        String content = buildArchiveContent(semesterId, archiveType);
        SemesterArchiveDO existDO = semesterArchiveMapper.getSemesterArchiveByType(semesterId, archiveType);
        SemesterArchiveDO requestParam = new SemesterArchiveDO();
        requestParam.setSemesterId(semesterId);
        requestParam.setArchiveType(archiveType);
        requestParam.setArchiveContent(content);
        requestParam.setArchivedAt(LocalDateTime.now());
        if (existDO == null) {
            semesterArchiveMapper.saveSemesterArchive(requestParam);
            return;
        }
        requestParam.setId(existDO.getId());
        semesterArchiveMapper.updateSemesterArchive(requestParam);
    }

    /**
     * 构建归档内容 JSON
     *
     * @param semesterId  学期 ID
     * @param archiveType 归档类型
     * @return 归档内容 JSON 串
     */
    private String buildArchiveContent(Long semesterId, String archiveType) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("semesterId", semesterId);
        result.put("archiveType", archiveType);
        try {
            if (ARCHIVE_OVERVIEW.equals(archiveType)) {
                result.put("overview", statisticsSnapshotService.buildOverviewSnapshot(semesterId));
            } else if (ARCHIVE_SCORE.equals(archiveType)) {
                buildScoreArchive(result, semesterId);
            } else {
                buildSelectionArchive(result, semesterId);
            }
            return objectMapper.writeValueAsString(result);
        } catch (Exception ex) {
            log.error("归档内容序列化失败，学期ID：{}，归档类型：{}", semesterId, archiveType, ex);
            return "{\"semesterId\":" + semesterId + ",\"archiveType\":\"" + archiveType + "\"}";
        }
    }

    /**
     * 填充选课归档内容
     *
     * @param result     归档内容容器
     * @param semesterId 学期 ID
     */
    private void buildSelectionArchive(Map<String, Object> result, Long semesterId) {
        List<TeachingClassDO> teachingClassDOList = teachingClassMapper.listTeachingClassBySemesterId(semesterId);
        result.put("teachingClassCount", teachingClassDOList.size());
        result.put("selectedCount", countSelection(semesterId, SelectionStatusEnum.SELECTED));
        result.put("waitlistCount", countSelection(semesterId, SelectionStatusEnum.WAITING));
        result.put("withdrawCount", countSelection(semesterId, SelectionStatusEnum.WITHDRAWN));
    }

    /**
     * 填充成绩归档内容
     *
     * @param result     归档内容容器
     * @param semesterId 学期 ID
     */
    private void buildScoreArchive(Map<String, Object> result, Long semesterId) {
        List<ScoreDO> scoreDOList = scoreMapper.listPublishedScoreBySemesterId(semesterId);
        result.put("scoreCount", scoreDOList.size());
        long passCount = scoreDOList.stream()
                .filter(each -> each.getScoreValue() != null
                        && each.getScoreValue().compareTo(new BigDecimal("60")) >= 0)
                .count();
        result.put("passCount", passCount);
        result.put("archiveAt", LocalDateTime.now().toString());
    }

    /**
     * 统计本学期指定状态的选课记录数
     *
     * @param semesterId 学期 ID
     * @param status     选课状态
     * @return 记录数
     */
    private long countSelection(Long semesterId, SelectionStatusEnum status) {
        StudentSelectionQueryReqDTO requestParam = new StudentSelectionQueryReqDTO();
        requestParam.setSemesterId(semesterId);
        requestParam.setStatus(status.name());
        return courseSelectionMapper.listSelectionByStudent(requestParam).size();
    }

    /**
     * 查询已结束的学期
     *
     * @return 已结束学期集合
     */
    private List<SemesterDO> listFinishedSemester() {
        SemesterPageQueryReqDTO requestParam = new SemesterPageQueryReqDTO();
        return semesterMapper.listSemesterByCondition(Page.of(1, SEMESTER_SCAN_LIMIT), requestParam)
                .getRecords().stream()
                .filter(each -> SemesterStatusEnum.FINISHED == each.getStatus())
                .toList();
    }
}
