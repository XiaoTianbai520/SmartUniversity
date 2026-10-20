package com.smart.university.mapper;

import com.smart.university.common.util.EnumParseUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.SemesterPageQueryReqDTO;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.enums.SemesterStatusEnum;
import com.smart.university.domain.enums.SemesterStatusEnum;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 学期持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface SemesterMapper extends BaseMapper<SemesterDO> {

    /**
     * 根据 ID 查询学期
     *
     * @param semesterId 学期 ID
     * @return 学期信息
     */
    default SemesterDO getSemesterById(Long semesterId) {
        return selectById(semesterId);
    }

    /**
     * 查询当前启用的学期
     *
     * @return 学期信息
     */
    default SemesterDO getCurrentSemester() {
        return selectOne(Wrappers.<SemesterDO>lambdaQuery()
                .eq(SemesterDO::getStatus, SemesterStatusEnum.ACTIVE)
                .orderByDesc(SemesterDO::getStartDate)
                .last("LIMIT 1"));
    }

    /**
     * 按学年 + 学期序号查询学期，用于定位入学学期
     *
     * @param academicYear 学年，例如 2026-2027
     * @param termNo       学期序号 1/2
     * @return 学期信息，不存在时返回 null
     */
    default SemesterDO getSemesterByAcademicYearAndTerm(String academicYear, Integer termNo) {
        return selectOne(Wrappers.<SemesterDO>lambdaQuery()
                .eq(SemesterDO::getAcademicYear, academicYear)
                .eq(SemesterDO::getTermNo, termNo)
                .orderByAsc(SemesterDO::getStartDate)
                .last("LIMIT 1"));
    }

    /**
     * 统计开学日期落在指定闭区间内的学期数量（按学期表实算已读学期数）
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 学期数量
     */
    default long countSemesterByStartDateBetween(LocalDate startDate, LocalDate endDate) {
        return selectCount(Wrappers.<SemesterDO>lambdaQuery()
                .ge(SemesterDO::getStartDate, startDate)
                .le(SemesterDO::getStartDate, endDate));
    }

    /**
     * 按 ID 集合批量查询学期
     *
     * @param semesterIds 学期 ID 集合
     * @return 学期集合
     */
    default List<SemesterDO> listSemesterByIds(Collection<Long> semesterIds) {
        if (semesterIds == null || semesterIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SemesterDO>lambdaQuery()
                .in(SemesterDO::getId, semesterIds)
                .orderByAsc(SemesterDO::getStartDate));
    }

    /**
     * 按条件统计学期数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    default long countSemesterByCondition(SemesterPageQueryReqDTO requestParam) {
        return selectCount(buildQueryWrapper(requestParam));
    }

    /**
     * 按条件分页查询学期
     *
     * @param page         分页参数
     * @param requestParam 查询条件
     * @return 分页结果
     */
    default IPage<SemesterDO> listSemesterByCondition(IPage<SemesterDO> page, SemesterPageQueryReqDTO requestParam) {
        return selectPage(page, buildQueryWrapper(requestParam));
    }

    /**
     * 保存学期
     *
     * @param requestParam 学期数据对象
     * @return 影响行数
     */
    default int saveSemester(SemesterDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新学期
     *
     * @param requestParam 学期数据对象
     * @return 影响行数
     */
    default int updateSemester(SemesterDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 构建学期查询条件
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<SemesterDO> buildQueryWrapper(SemesterPageQueryReqDTO requestParam) {
        String keyword = requestParam.getKeyword();
        LambdaQueryWrapper<SemesterDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.and(StrUtil.isNotBlank(keyword), each -> each
                .like(SemesterDO::getSemesterCode, keyword));
        SemesterStatusEnum status = EnumParseUtil.parseOrNull(SemesterStatusEnum.class, requestParam.getStatus());
        queryWrapper.eq(status != null, SemesterDO::getStatus, status);
        queryWrapper.orderByDesc(SemesterDO::getStartDate);
        return queryWrapper;
    }
}
