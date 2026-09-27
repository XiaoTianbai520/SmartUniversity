package com.smart.university.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.entity.TeachingClassMajorDO;

import java.util.List;

/**
 * 教学班适用专业持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface TeachingClassMajorMapper extends BaseMapper<TeachingClassMajorDO> {

    /**
     * 保存教学班适用专业
     *
     * @param requestParam 教学班适用专业数据对象
     * @return 影响行数
     */
    default int saveTeachingClassMajor(TeachingClassMajorDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 删除教学班下的全部适用专业
     *
     * @param teachingClassId 教学班 ID
     * @return 影响行数
     */
    default int removeTeachingClassMajorByTeachingClassId(Long teachingClassId) {
        return delete(Wrappers.<TeachingClassMajorDO>lambdaQuery()
                .eq(TeachingClassMajorDO::getTeachingClassId, teachingClassId));
    }

    /**
     * 查询教学班开放的专业 ID 集合
     *
     * @param teachingClassId 教学班 ID
     * @return 专业 ID 集合
     */
    default List<Long> listMajorIdByTeachingClassId(Long teachingClassId) {
        return selectObjs(Wrappers.<TeachingClassMajorDO>lambdaQuery()
                        .select(TeachingClassMajorDO::getMajorId)
                        .eq(TeachingClassMajorDO::getTeachingClassId, teachingClassId))
                .stream()
                .map(each -> each == null ? null : Long.valueOf(each.toString()))
                .toList();
    }

    /**
     * 统计指定教学班与专业的绑定数量
     *
     * @param requestParam 查询条件，包含 teachingClassId 与 majorId
     * @return 数量
     */
    default long countTeachingClassMajor(TeachingClassMajorDO requestParam) {
        LambdaQueryWrapper<TeachingClassMajorDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(requestParam.getTeachingClassId() != null, TeachingClassMajorDO::getTeachingClassId,
                requestParam.getTeachingClassId());
        queryWrapper.eq(requestParam.getMajorId() != null, TeachingClassMajorDO::getMajorId,
                requestParam.getMajorId());
        return selectCount(queryWrapper);
    }
}
