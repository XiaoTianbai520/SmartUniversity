package com.smart.university.mapper;

import com.smart.university.domain.entity.TeachingClassGradeDO;

import java.util.List;

/**
 * 教学班适用年级持久层
 */
public interface TeachingClassGradeMapper {

    /**
     * 保存教学班适用年级
     *
     * @param requestParam 教学班适用年级数据对象
     * @return 影响行数
     */
    int saveTeachingClassGrade(TeachingClassGradeDO requestParam);

    /**
     * 删除教学班下的全部适用年级
     *
     * @param teachingClassId 教学班 ID
     * @return 影响行数
     */
    int removeTeachingClassGradeByTeachingClassId(Long teachingClassId);

    /**
     * 查询教学班开放的年级 ID 集合
     *
     * @param teachingClassId 教学班 ID
     * @return 年级 ID 集合
     */
    List<Long> listGradeIdByTeachingClassId(Long teachingClassId);

    /**
     * 统计指定教学班与年级的绑定数量
     *
     * @param requestParam 查询条件，包含 teachingClassId 与 gradeId
     * @return 数量
     */
    long countTeachingClassGrade(TeachingClassGradeDO requestParam);
}
