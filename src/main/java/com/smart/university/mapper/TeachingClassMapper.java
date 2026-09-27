package com.smart.university.mapper;

import com.smart.university.common.util.EnumParseUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.StudentTeachingClassPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeachingClassPageQueryReqDTO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.enums.TeachingClassStatusEnum;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 教学班持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力；
 * 涉及课程名模糊匹配与学生可见范围判定的联表查询保留在 XML 中，由分页插件完成物理分页。
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface TeachingClassMapper extends BaseMapper<TeachingClassDO> {

    /**
     * 根据 ID 查询教学班
     *
     * @param teachingClassId 教学班 ID
     * @return 教学班信息
     */
    default TeachingClassDO getTeachingClassById(Long teachingClassId) {
        return selectById(teachingClassId);
    }

    /**
     * 根据教学班编号查询教学班
     *
     * @param classCode 教学班编号
     * @return 教学班信息
     */
    default TeachingClassDO getTeachingClassByClassCode(String classCode) {
        return selectOne(Wrappers.<TeachingClassDO>lambdaQuery()
                .eq(TeachingClassDO::getClassCode, classCode)
                .last("LIMIT 1"));
    }

    /**
     * 根据 ID 集合批量查询教学班
     *
     * @param teachingClassIds 教学班 ID 集合
     * @return 教学班信息集合
     */
    default List<TeachingClassDO> listTeachingClassByIds(List<Long> teachingClassIds) {
        return selectList(Wrappers.<TeachingClassDO>lambdaQuery().in(TeachingClassDO::getId, teachingClassIds));
    }

    /**
     * 查询教师的教学班
     *
     * @param teachingClassDO 查询条件，包含 teacherId 与 semesterId
     * @return 教学班信息集合
     */
    default List<TeachingClassDO> listTeachingClassByTeacher(TeachingClassDO teachingClassDO) {
        LambdaQueryWrapper<TeachingClassDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(teachingClassDO.getTeacherId() != null, TeachingClassDO::getTeacherId,
                teachingClassDO.getTeacherId());
        queryWrapper.eq(teachingClassDO.getSemesterId() != null, TeachingClassDO::getSemesterId,
                teachingClassDO.getSemesterId());
        queryWrapper.orderByDesc(TeachingClassDO::getId);
        return selectList(queryWrapper);
    }

    /**
     * 按条件统计教学班数量，课程名模糊匹配由 XML 中的联表语句完成
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    long countTeachingClassByCondition(@Param("requestParam") TeachingClassPageQueryReqDTO requestParam);

    /**
     * 按条件分页查询教学班，课程名模糊匹配由 XML 中的联表语句完成
     *
     * @param page         分页参数
     * @param requestParam 查询条件
     * @return 分页结果
     */
    IPage<TeachingClassDO> listTeachingClassByCondition(IPage<TeachingClassDO> page,
                                                        @Param("requestParam") TeachingClassPageQueryReqDTO requestParam);

    /**
     * 统计学生可见的教学班数量，包含学期、批次、专业、年级多重校验
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    long countStudentVisibleTeachingClass(@Param("requestParam") StudentTeachingClassPageQueryReqDTO requestParam);

    /**
     * 分页查询学生可见的教学班，包含学期、批次、专业、年级多重校验
     *
     * @param page         分页参数
     * @param requestParam 查询条件
     * @return 分页结果
     */
    IPage<TeachingClassDO> listStudentVisibleTeachingClass(IPage<TeachingClassDO> page,
                                                           @Param("requestParam") StudentTeachingClassPageQueryReqDTO requestParam);

    /**
     * 保存教学班
     *
     * @param requestParam 教学班数据对象
     * @return 影响行数
     */
    default int saveTeachingClass(TeachingClassDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新教学班
     *
     * @param requestParam 教学班数据对象
     * @return 影响行数
     */
    default int updateTeachingClass(TeachingClassDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 构建教学班查询条件，用于单表场景
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<TeachingClassDO> buildQueryWrapper(TeachingClassPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<TeachingClassDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(requestParam.getSemesterId() != null, TeachingClassDO::getSemesterId,
                requestParam.getSemesterId());
        queryWrapper.eq(requestParam.getCourseId() != null, TeachingClassDO::getCourseId, requestParam.getCourseId());
        queryWrapper.eq(requestParam.getTeacherId() != null, TeachingClassDO::getTeacherId,
                requestParam.getTeacherId());
        TeachingClassStatusEnum status = EnumParseUtil.parseOrNull(TeachingClassStatusEnum.class,
                requestParam.getStatus());
        queryWrapper.eq(status != null, TeachingClassDO::getStatus, status);
        queryWrapper.orderByDesc(TeachingClassDO::getId);
        return queryWrapper;
    }
}
