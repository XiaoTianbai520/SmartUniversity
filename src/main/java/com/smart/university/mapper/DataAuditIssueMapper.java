package com.smart.university.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.entity.DataAuditIssueDO;

import java.time.LocalDate;
import java.util.List;

/**
 * 异常数据巡检结果持久层，对外只暴露 default 方法，
 * 方法名遵循 get / list / count / save / update / remove 前缀规范
 */
public interface DataAuditIssueMapper extends BaseMapper<DataAuditIssueDO> {

    /**
     * 根据 ID 查询巡检结果
     *
     * @param issueId 问题 ID
     * @return 巡检结果
     */
    default DataAuditIssueDO getDataAuditIssueById(Long issueId) {
        return selectById(issueId);
    }

    /**
     * 查询指定学期的巡检结果
     *
     * @param semesterId 学期 ID
     * @return 巡检结果集合
     */
    default List<DataAuditIssueDO> listDataAuditIssueBySemesterId(Long semesterId) {
        return selectList(Wrappers.<DataAuditIssueDO>lambdaQuery()
                .eq(DataAuditIssueDO::getSemesterId, semesterId)
                .orderByAsc(DataAuditIssueDO::getRuleCode)
                .orderByAsc(DataAuditIssueDO::getBizId));
    }

    /**
     * 统计指定学期的巡检结果数量
     *
     * @param semesterId 学期 ID
     * @return 数量
     */
    default long countDataAuditIssueBySemesterId(Long semesterId) {
        return selectCount(Wrappers.<DataAuditIssueDO>lambdaQuery()
                .eq(DataAuditIssueDO::getSemesterId, semesterId));
    }

    /**
     * 保存巡检结果
     *
     * @param requestParam 巡检结果数据对象
     * @return 影响行数
     */
    default int saveDataAuditIssue(DataAuditIssueDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 删除指定学期指定巡检日期的结果，覆盖写前先清理当日旧数据
     *
     * @param semesterId 学期 ID
     * @param statDate   巡检日期
     * @return 影响行数
     */
    default int removeDataAuditIssueBySemesterAndDate(Long semesterId, LocalDate statDate) {
        return delete(buildSemesterAndDateWrapper(semesterId, statDate));
    }

    /**
     * 构建学期与巡检日期的查询条件
     *
     * @param semesterId 学期 ID
     * @param statDate   巡检日期
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<DataAuditIssueDO> buildSemesterAndDateWrapper(Long semesterId, LocalDate statDate) {
        return Wrappers.<DataAuditIssueDO>lambdaQuery()
                .eq(DataAuditIssueDO::getSemesterId, semesterId)
                .eq(DataAuditIssueDO::getStatDate, statDate);
    }
}
