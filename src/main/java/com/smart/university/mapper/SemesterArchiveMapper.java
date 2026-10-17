package com.smart.university.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.entity.SemesterArchiveDO;

import java.util.List;

/**
 * 学期归档结果持久层，对外只暴露 default 方法，
 * 方法名遵循 get / list / count / save / update / remove 前缀规范
 */
public interface SemesterArchiveMapper extends BaseMapper<SemesterArchiveDO> {

    /**
     * 根据 ID 查询归档结果
     *
     * @param archiveId 归档 ID
     * @return 归档结果
     */
    default SemesterArchiveDO getSemesterArchiveById(Long archiveId) {
        return selectById(archiveId);
    }

    /**
     * 查询指定学期与归档类型的归档结果，唯一键保证至多一条
     *
     * @param semesterId  学期 ID
     * @param archiveType 归档类型
     * @return 归档结果，无记录时返回 null
     */
    default SemesterArchiveDO getSemesterArchiveByType(Long semesterId, String archiveType) {
        return selectOne(Wrappers.<SemesterArchiveDO>lambdaQuery()
                .eq(SemesterArchiveDO::getSemesterId, semesterId)
                .eq(SemesterArchiveDO::getArchiveType, archiveType)
                .last("LIMIT 1"));
    }

    /**
     * 查询指定学期的归档结果
     *
     * @param semesterId 学期 ID
     * @return 归档结果集合
     */
    default List<SemesterArchiveDO> listSemesterArchiveBySemesterId(Long semesterId) {
        return selectList(Wrappers.<SemesterArchiveDO>lambdaQuery()
                .eq(SemesterArchiveDO::getSemesterId, semesterId)
                .orderByAsc(SemesterArchiveDO::getArchiveType));
    }

    /**
     * 保存归档结果
     *
     * @param requestParam 归档结果数据对象
     * @return 影响行数
     */
    default int saveSemesterArchive(SemesterArchiveDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新归档结果
     *
     * @param requestParam 归档结果数据对象
     * @return 影响行数
     */
    default int updateSemesterArchive(SemesterArchiveDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 删除指定学期的归档结果
     *
     * @param semesterId 学期 ID
     * @return 影响行数
     */
    default int removeSemesterArchiveBySemesterId(Long semesterId) {
        return delete(Wrappers.<SemesterArchiveDO>lambdaQuery()
                .eq(SemesterArchiveDO::getSemesterId, semesterId));
    }

    /**
     * 构建学期与归档类型的查询条件
     *
     * @param semesterId  学期 ID
     * @param archiveType 归档类型
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<SemesterArchiveDO> buildSemesterAndTypeWrapper(Long semesterId, String archiveType) {
        return Wrappers.<SemesterArchiveDO>lambdaQuery()
                .eq(SemesterArchiveDO::getSemesterId, semesterId)
                .eq(SemesterArchiveDO::getArchiveType, archiveType);
    }
}
