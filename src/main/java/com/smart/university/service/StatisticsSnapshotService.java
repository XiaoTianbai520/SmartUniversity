package com.smart.university.service;

import com.smart.university.common.base.SnapshotResult;
import com.smart.university.common.enums.StatTypeEnum;
import com.smart.university.domain.dto.resp.SnapshotRefreshRespDTO;
import com.smart.university.domain.entity.StatClassroomUsageDO;
import com.smart.university.domain.entity.StatCourseSelectionDO;
import com.smart.university.domain.entity.StatCreditProgressDO;
import com.smart.university.domain.entity.StatSemesterOverviewDO;
import com.smart.university.domain.entity.StatTeacherWorkloadDO;

import java.util.List;
import java.util.function.Supplier;

/**
 * 统计快照服务，统一「命中快照直接返回、未命中实时计算并回写」的读法
 */
public interface StatisticsSnapshotService {

    /**
     * 读取或计算教室利用率快照
     *
     * @param semesterId  学期 ID
     * @param computeFunc 实时计算函数
     * @return 快照读取结果
     */
    SnapshotResult<List<StatClassroomUsageDO>> readOrComputeClassroomUsage(
            Long semesterId, Supplier<List<StatClassroomUsageDO>> computeFunc);

    /**
     * 读取或计算教师工作量快照
     *
     * @param semesterId  学期 ID
     * @param computeFunc 实时计算函数
     * @return 快照读取结果
     */
    SnapshotResult<List<StatTeacherWorkloadDO>> readOrComputeTeacherWorkload(
            Long semesterId, Supplier<List<StatTeacherWorkloadDO>> computeFunc);

    /**
     * 读取或计算课程选课分析快照
     *
     * @param semesterId  学期 ID
     * @param computeFunc 实时计算函数
     * @return 快照读取结果
     */
    SnapshotResult<List<StatCourseSelectionDO>> readOrComputeCourseSelection(
            Long semesterId, Supplier<List<StatCourseSelectionDO>> computeFunc);

    /**
     * 读取或计算学生学分进度快照
     *
     * @param semesterId  学期 ID
     * @param computeFunc 实时计算函数
     * @return 快照读取结果
     */
    SnapshotResult<List<StatCreditProgressDO>> readOrComputeCreditProgress(
            Long semesterId, Supplier<List<StatCreditProgressDO>> computeFunc);

    /**
     * 读取或计算教务看板指标快照
     *
     * @param semesterId  学期 ID
     * @param computeFunc 实时计算函数
     * @return 快照读取结果
     */
    SnapshotResult<StatSemesterOverviewDO> readOrComputeOverview(
            Long semesterId, Supplier<StatSemesterOverviewDO> computeFunc);

    /**
     * 构建教务看板指标快照，供看板查询与快照刷新共用
     *
     * @param semesterId 学期 ID
     * @return 教务看板指标快照
     */
    StatSemesterOverviewDO buildOverviewSnapshot(Long semesterId);

    /**
     * 手动刷新统计快照，覆盖写且幂等
     *
     * @param semesterId 学期 ID
     * @param statTypes  统计类型集合，为空时刷新全部
     * @return 刷新结果
     */
    SnapshotRefreshRespDTO refreshSnapshot(Long semesterId, List<StatTypeEnum> statTypes);
}
