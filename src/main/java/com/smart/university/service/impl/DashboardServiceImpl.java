package com.smart.university.service.impl;

import com.smart.university.common.base.SnapshotResult;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.dto.resp.DashboardOverviewRespDTO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.SelectionBatchDO;
import com.smart.university.domain.entity.StatSemesterOverviewDO;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.SelectionBatchMapper;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.service.DashboardService;
import com.smart.university.service.SemesterService;
import com.smart.university.service.StatisticsSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 教务数据看板服务实现（S-06）
 *
 * <p>决策 6 / Q20：`studentCount`（在册学生总数）与 `selectedStudentCount`（本学期已选课学生数）
 * 双口径同时输出，二者数值不等属预期，不可互相替代。
 * 由于 `stat_semester_overview` 表未提供「本学期已选课学生数」列，该字段与在册学生总数一律实时计算。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final StatisticsSnapshotService statisticsSnapshotService;
    private final SemesterService semesterService;
    private final StudentMapper studentMapper;
    private final CourseSelectionMapper courseSelectionMapper;
    private final SelectionBatchMapper selectionBatchMapper;

    @Override
    public DashboardOverviewRespDTO getDashboardOverview(Long semesterId) {
        Long targetSemesterId = semesterId == null
                ? semesterService.getCurrentSemester().getId()
                : semesterId;
        SnapshotResult<StatSemesterOverviewDO> snapshotResult = statisticsSnapshotService
                .readOrComputeOverview(targetSemesterId,
                        () -> statisticsSnapshotService.buildOverviewSnapshot(targetSemesterId));
        StatSemesterOverviewDO overviewDO = snapshotResult.getData();
        DashboardOverviewRespDTO result = convertToRespDTO(overviewDO, targetSemesterId);
        result.setSnapshotTime(snapshotResult.getSnapshotTime());
        result.setFromSnapshot(snapshotResult.isFromSnapshot());
        // 双口径人数实时计算，避免快照过期导致口径失真
        result.setStudentCount((int) studentMapper.countStudentByStatus(1));
        result.setSelectedStudentCount(countSelectedStudent(targetSemesterId));
        return result;
    }

    /**
     * 转换看板快照为出参
     *
     * @param overviewDO      看板快照
     * @param targetSemesterId 学期 ID
     * @return 看板出参
     */
    private DashboardOverviewRespDTO convertToRespDTO(StatSemesterOverviewDO overviewDO, Long targetSemesterId) {
        DashboardOverviewRespDTO result = new DashboardOverviewRespDTO();
        result.setSemesterId(targetSemesterId);
        if (overviewDO == null) {
            result.setStudentCount(0);
            result.setSelectedStudentCount(0);
            result.setCourseCount(0);
            result.setTeachingClassCount(0);
            result.setTeacherCount(0);
            result.setSelectionCount(0);
            result.setWaitlistCount(0);
            result.setWithdrawCount(0);
            result.setUnpublishedClassCount(0);
            return result;
        }
        result.setSemesterCode(overviewDO.getSemesterCode());
        result.setStudentCount(nullSafeInt(overviewDO.getStudentCount()));
        result.setSelectedStudentCount(0);
        result.setCourseCount(nullSafeInt(overviewDO.getCourseCount()));
        result.setTeachingClassCount(nullSafeInt(overviewDO.getTeachingClassCount()));
        result.setTeacherCount(nullSafeInt(overviewDO.getTeacherCount()));
        result.setSelectionCount(nullSafeInt(overviewDO.getSelectionCount()));
        result.setWaitlistCount(nullSafeInt(overviewDO.getWaitlistCount()));
        result.setWithdrawCount(nullSafeInt(overviewDO.getWithdrawCount()));
        result.setFullClassRate(overviewDO.getFullClassRate());
        result.setClassroomUsageRate(overviewDO.getClassroomUsageRate());
        result.setAvgTeacherWorkload(overviewDO.getAvgTeacherWorkload());
        result.setAvgCreditProgressRate(overviewDO.getAvgCreditProgressRate());
        result.setUnpublishedClassCount(nullSafeInt(overviewDO.getUnpublishedClassCount()));
        result.setBatchStatus(overviewDO.getBatchStatus() == null ? null : overviewDO.getBatchStatus().name());
        return result;
    }

    /**
     * 统计本学期已选课学生数（去重）：优先按当前批次，无批次时按本学期已选记录
     *
     * @param semesterId 学期 ID
     * @return 已选课学生数
     */
    private int countSelectedStudent(Long semesterId) {
        Set<Long> studentIdSet = new HashSet<>();
        SelectionBatchDO selectionBatchDO = selectionBatchMapper.getCurrentSelectionBatch(semesterId);
        if (selectionBatchDO != null) {
            CourseSelectionDO requestParam = new CourseSelectionDO();
            requestParam.setBatchId(selectionBatchDO.getId());
            requestParam.setStatus(SelectionStatusEnum.SELECTED);
            for (CourseSelectionDO each : courseSelectionMapper.listSelectionByCondition(requestParam)) {
                studentIdSet.add(each.getStudentId());
            }
            return studentIdSet.size();
        }
        StudentSelectionQueryReqDTO requestParam = new StudentSelectionQueryReqDTO();
        requestParam.setSemesterId(semesterId);
        requestParam.setStatus(SelectionStatusEnum.SELECTED.name());
        List<CourseSelectionDO> selectedList = courseSelectionMapper.listSelectionByStudent(requestParam);
        for (CourseSelectionDO each : selectedList) {
            studentIdSet.add(each.getStudentId());
        }
        return studentIdSet.size();
    }

    /**
     * 空值兜底为 0
     *
     * @param value 数值
     * @return 非空数值
     */
    private int nullSafeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
