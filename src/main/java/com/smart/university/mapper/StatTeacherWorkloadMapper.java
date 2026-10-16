package com.smart.university.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.entity.StatTeacherWorkloadDO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 教师工作量快照持久层，对外只暴露 default 方法，
 * 方法名遵循 get / list / count / save / update / remove 前缀规范
 */
public interface StatTeacherWorkloadMapper extends BaseMapper<StatTeacherWorkloadDO> {

    /**
     * 查询指定学期的教师工作量快照
     *
     * @param semesterId 学期 ID
     * @return 快照集合
     */
    default List<StatTeacherWorkloadDO> listStatTeacherWorkloadBySemesterId(Long semesterId) {
        return selectList(Wrappers.<StatTeacherWorkloadDO>lambdaQuery()
                .eq(StatTeacherWorkloadDO::getSemesterId, semesterId)
                .orderByAsc(StatTeacherWorkloadDO::getTeacherId));
    }

    /**
     * 查询指定学期最近一次快照生成时间
     *
     * @param semesterId 学期 ID
     * @return 生成时间，无快照时返回 null
     */
    default LocalDateTime getLatestGeneratedAt(Long semesterId) {
        StatTeacherWorkloadDO result = selectOne(Wrappers.<StatTeacherWorkloadDO>lambdaQuery()
                .select(StatTeacherWorkloadDO::getGeneratedAt)
                .eq(StatTeacherWorkloadDO::getSemesterId, semesterId)
                .orderByDesc(StatTeacherWorkloadDO::getGeneratedAt)
                .last("LIMIT 1"));
        return result == null ? null : result.getGeneratedAt();
    }

    /**
     * 统计指定学期的快照数量
     *
     * @param semesterId 学期 ID
     * @return 数量
     */
    default long countStatTeacherWorkloadBySemesterId(Long semesterId) {
        return selectCount(Wrappers.<StatTeacherWorkloadDO>lambdaQuery()
                .eq(StatTeacherWorkloadDO::getSemesterId, semesterId));
    }

    /**
     * 保存教师工作量快照
     *
     * @param requestParam 快照数据对象
     * @return 影响行数
     */
    default int saveStatTeacherWorkload(StatTeacherWorkloadDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 删除指定学期指定日期的快照，覆盖写前先清理当日旧数据
     *
     * @param semesterId 学期 ID
     * @param statDate   快照日期
     * @return 影响行数
     */
    default int removeStatTeacherWorkloadBySemesterAndDate(Long semesterId, LocalDate statDate) {
        return delete(buildSemesterAndDateWrapper(semesterId, statDate));
    }

    /**
     * 删除指定学期早于指定日期的快照，保证每学期只保留最新一份
     *
     * @param semesterId 学期 ID
     * @param statDate   快照日期
     * @return 影响行数
     */
    default int removeStatTeacherWorkloadBeforeDate(Long semesterId, LocalDate statDate) {
        return delete(Wrappers.<StatTeacherWorkloadDO>lambdaQuery()
                .eq(StatTeacherWorkloadDO::getSemesterId, semesterId)
                .lt(StatTeacherWorkloadDO::getStatDate, statDate));
    }

    /**
     * 构建学期与快照日期的查询条件
     *
     * @param semesterId 学期 ID
     * @param statDate   快照日期
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<StatTeacherWorkloadDO> buildSemesterAndDateWrapper(Long semesterId, LocalDate statDate) {
        return Wrappers.<StatTeacherWorkloadDO>lambdaQuery()
                .eq(StatTeacherWorkloadDO::getSemesterId, semesterId)
                .eq(StatTeacherWorkloadDO::getStatDate, statDate);
    }
}
