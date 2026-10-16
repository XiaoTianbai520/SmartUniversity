package com.smart.university.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.entity.StatCreditProgressDO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生学分进度快照持久层，对外只暴露 default 方法，
 * 方法名遵循 get / list / count / save / update / remove 前缀规范
 */
public interface StatCreditProgressMapper extends BaseMapper<StatCreditProgressDO> {

    /**
     * 查询指定学期的学生学分进度快照
     *
     * @param semesterId 学期 ID
     * @return 快照集合
     */
    default List<StatCreditProgressDO> listStatCreditProgressBySemesterId(Long semesterId) {
        return selectList(Wrappers.<StatCreditProgressDO>lambdaQuery()
                .eq(StatCreditProgressDO::getSemesterId, semesterId)
                .orderByAsc(StatCreditProgressDO::getStudentId));
    }

    /**
     * 查询指定学期最近一次快照生成时间
     *
     * @param semesterId 学期 ID
     * @return 生成时间，无快照时返回 null
     */
    default LocalDateTime getLatestGeneratedAt(Long semesterId) {
        StatCreditProgressDO result = selectOne(Wrappers.<StatCreditProgressDO>lambdaQuery()
                .select(StatCreditProgressDO::getGeneratedAt)
                .eq(StatCreditProgressDO::getSemesterId, semesterId)
                .orderByDesc(StatCreditProgressDO::getGeneratedAt)
                .last("LIMIT 1"));
        return result == null ? null : result.getGeneratedAt();
    }

    /**
     * 统计指定学期的快照数量
     *
     * @param semesterId 学期 ID
     * @return 数量
     */
    default long countStatCreditProgressBySemesterId(Long semesterId) {
        return selectCount(Wrappers.<StatCreditProgressDO>lambdaQuery()
                .eq(StatCreditProgressDO::getSemesterId, semesterId));
    }

    /**
     * 保存学生学分进度快照
     *
     * @param requestParam 快照数据对象
     * @return 影响行数
     */
    default int saveStatCreditProgress(StatCreditProgressDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 删除指定学期指定日期的快照，覆盖写前先清理当日旧数据
     *
     * @param semesterId 学期 ID
     * @param statDate   快照日期
     * @return 影响行数
     */
    default int removeStatCreditProgressBySemesterAndDate(Long semesterId, LocalDate statDate) {
        return delete(buildSemesterAndDateWrapper(semesterId, statDate));
    }

    /**
     * 删除指定学期早于指定日期的快照，保证每学期只保留最新一份
     *
     * @param semesterId 学期 ID
     * @param statDate   快照日期
     * @return 影响行数
     */
    default int removeStatCreditProgressBeforeDate(Long semesterId, LocalDate statDate) {
        return delete(Wrappers.<StatCreditProgressDO>lambdaQuery()
                .eq(StatCreditProgressDO::getSemesterId, semesterId)
                .lt(StatCreditProgressDO::getStatDate, statDate));
    }

    /**
     * 构建学期与快照日期的查询条件
     *
     * @param semesterId 学期 ID
     * @param statDate   快照日期
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<StatCreditProgressDO> buildSemesterAndDateWrapper(Long semesterId, LocalDate statDate) {
        return Wrappers.<StatCreditProgressDO>lambdaQuery()
                .eq(StatCreditProgressDO::getSemesterId, semesterId)
                .eq(StatCreditProgressDO::getStatDate, statDate);
    }
}
