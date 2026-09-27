package com.smart.university.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.entity.TeachingClassGradeDO;

import java.util.List;

/**
 * 教学班适用年级持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface TeachingClassGradeMapper extends BaseMapper<TeachingClassGradeDO> {

    /**
     * 保存教学班适用年级
     *
     * @param requestParam 教学班适用年级数据对象
     * @return 影响行数
     */
    default int saveTeachingClassGrade(TeachingClassGradeDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 删除教学班下的全部适用年级
     *
     * @param teachingClassId 教学班 ID
     * @return 影响行数
     */
    default int removeTeachingClassGradeByTeachingClassId(Long teachingClassId) {
        return delete(Wrappers.<TeachingClassGradeDO>lambdaQuery()
                .eq(TeachingClassGradeDO::getTeachingClassId, teachingClassId));
    }

    /**
     * 查询教学班开放的年级 ID 集合
     *
     * @param teachingClassId 教学班 ID
     * @return 年级 ID 集合
     */
    default List<Long> listGradeIdByTeachingClassId(Long teachingClassId) {
        return selectObjs(Wrappers.<TeachingClassGradeDO>lambdaQuery()
                        .select(TeachingClassGradeDO::getGradeId)
                        .eq(TeachingClassGradeDO::getTeachingClassId, teachingClassId))
                .stream()
                .map(each -> each == null ? null : Long.valueOf(each.toString()))
                .toList();
    }

    /**
     * 统计指定教学班与年级的绑定数量
     *
     * @param requestParam 查询条件，包含 teachingClassId 与 gradeId
     * @return 数量
     */
    default long countTeachingClassGrade(TeachingClassGradeDO requestParam) {
        LambdaQueryWrapper<TeachingClassGradeDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(requestParam.getTeachingClassId() != null, TeachingClassGradeDO::getTeachingClassId,
                requestParam.getTeachingClassId());
        queryWrapper.eq(requestParam.getGradeId() != null, TeachingClassGradeDO::getGradeId,
                requestParam.getGradeId());
        return selectCount(queryWrapper);
    }
}
