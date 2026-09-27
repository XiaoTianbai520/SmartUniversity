package com.smart.university.mapper;

import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.dto.req.SemesterPageQueryReqDTO;

import java.util.List;

/**
 * 学期持久层
 */
public interface SemesterMapper {

    /**
     * 根据 ID 查询学期
     *
     * @param semesterId 学期 ID
     * @return 学期信息
     */
    SemesterDO getSemesterById(Long semesterId);

    /**
     * 查询当前生效学期
     *
     * @return 学期信息
     */
    SemesterDO getCurrentSemester();

    /**
     * 按条件统计学期数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    long countSemesterByCondition(SemesterPageQueryReqDTO requestParam);

    /**
     * 按条件分页查询学期
     *
     * @param requestParam 查询条件
     * @return 学期信息集合
     */
    List<SemesterDO> listSemesterByCondition(SemesterPageQueryReqDTO requestParam);

    /**
     * 保存学期
     *
     * @param requestParam 学期数据对象
     * @return 影响行数
     */
    int saveSemester(SemesterDO requestParam);

    /**
     * 更新学期
     *
     * @param requestParam 学期数据对象
     * @return 影响行数
     */
    int updateSemester(SemesterDO requestParam);
}
