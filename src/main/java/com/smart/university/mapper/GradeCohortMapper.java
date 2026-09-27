package com.smart.university.mapper;

import com.smart.university.domain.entity.GradeCohortDO;
import com.smart.university.domain.dto.req.GradePageQueryReqDTO;

import java.util.List;

/**
 * 年级持久层
 */
public interface GradeCohortMapper {

    /**
     * 根据 ID 查询年级
     *
     * @param gradeId 年级 ID
     * @return 年级信息
     */
    GradeCohortDO getGradeCohortById(Long gradeId);

    /**
     * 根据 ID 集合批量查询年级
     *
     * @param gradeIds 年级 ID 集合
     * @return 年级信息集合
     */
    List<GradeCohortDO> listGradeCohortByIds(List<Long> gradeIds);

    /**
     * 按条件统计年级数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    long countGradeCohortByCondition(GradePageQueryReqDTO requestParam);

    /**
     * 按条件分页查询年级
     *
     * @param requestParam 查询条件
     * @return 年级信息集合
     */
    List<GradeCohortDO> listGradeCohortByCondition(GradePageQueryReqDTO requestParam);

    /**
     * 保存年级
     *
     * @param requestParam 年级数据对象
     * @return 影响行数
     */
    int saveGradeCohort(GradeCohortDO requestParam);

    /**
     * 更新年级
     *
     * @param requestParam 年级数据对象
     * @return 影响行数
     */
    int updateGradeCohort(GradeCohortDO requestParam);
}
