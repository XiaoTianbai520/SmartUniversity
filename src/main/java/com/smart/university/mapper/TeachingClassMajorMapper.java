package com.smart.university.mapper;

import com.smart.university.domain.entity.TeachingClassMajorDO;

import java.util.List;

/**
 * 教学班适用专业持久层
 */
public interface TeachingClassMajorMapper {

    /**
     * 保存教学班适用专业
     *
     * @param requestParam 教学班适用专业数据对象
     * @return 影响行数
     */
    int saveTeachingClassMajor(TeachingClassMajorDO requestParam);

    /**
     * 删除教学班下的全部适用专业
     *
     * @param teachingClassId 教学班 ID
     * @return 影响行数
     */
    int removeTeachingClassMajorByTeachingClassId(Long teachingClassId);

    /**
     * 查询教学班开放的专业 ID 集合
     *
     * @param teachingClassId 教学班 ID
     * @return 专业 ID 集合
     */
    List<Long> listMajorIdByTeachingClassId(Long teachingClassId);

    /**
     * 统计指定教学班与专业的绑定数量
     *
     * @param requestParam 查询条件，包含 teachingClassId 与 majorId
     * @return 数量
     */
    long countTeachingClassMajor(TeachingClassMajorDO requestParam);
}
