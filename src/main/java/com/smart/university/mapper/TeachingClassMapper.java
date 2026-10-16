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
import java.util.Objects;

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
     * 已选人数加一，带容量校验，未超容量才更新，返回影响行数
     *
     * @param teachingClassId 教学班 ID
     * @param capacity        教学班容量
     * @return 影响行数，为 0 表示已满或未匹配到教学班
     */
    default int updateSelectedCountWithCapacityCheck(Long teachingClassId, Integer capacity) {
        return update(null, Wrappers.<TeachingClassDO>lambdaUpdate()
                .setSql("selected_count = selected_count + 1")
                .eq(TeachingClassDO::getId, teachingClassId)
                .lt(TeachingClassDO::getSelectedCount, capacity));
    }

    /**
     * 已选人数减一，最低减到 0，不会出现负数
     *
     * @param teachingClassId 教学班 ID
     * @return 影响行数
     */
    default int restoreSelectedCount(Long teachingClassId) {
        return update(null, Wrappers.<TeachingClassDO>lambdaUpdate()
                .setSql("selected_count = IF(selected_count > 0, selected_count - 1, 0)")
                .eq(TeachingClassDO::getId, teachingClassId));
    }

    /**
     * 将已选人数重置为指定值，用于缓存一致性对账
     *
     * @param teachingClassId 教学班 ID
     * @param targetCount     目标已选人数
     * @return 影响行数
     */
    default int resetSelectedCount(Long teachingClassId, Integer targetCount) {
        return update(null, Wrappers.<TeachingClassDO>lambdaUpdate()
                .set(TeachingClassDO::getSelectedCount, targetCount)
                .eq(TeachingClassDO::getId, teachingClassId));
    }

    /**
     * 查询指定学期下的全部教学班
     *
     * @param semesterId 学期 ID
     * @return 教学班集合
     */
    default List<TeachingClassDO> listTeachingClassBySemesterId(Long semesterId) {
        return selectList(Wrappers.<TeachingClassDO>lambdaQuery()
                .eq(TeachingClassDO::getSemesterId, semesterId)
                .orderByAsc(TeachingClassDO::getId));
    }

    /**
     * 统计指定学期下指定状态的教学班数量
     *
     * @param semesterId 学期 ID
     * @param status     教学班状态，为空时只按学期过滤
     * @return 数量
     */
    default long countTeachingClassBySemesterAndStatus(Long semesterId, TeachingClassStatusEnum status) {
        return selectCount(Wrappers.<TeachingClassDO>lambdaQuery()
                .eq(TeachingClassDO::getSemesterId, semesterId)
                .eq(status != null, TeachingClassDO::getStatus, status));
    }

    /**
     * 查询指定学期下已排课的课程 ID 集合
     *
     * @param semesterId 学期 ID
     * @return 课程 ID 集合
     */
    default List<Long> listCourseIdBySemesterId(Long semesterId) {
        List<Object> records = selectObjs(Wrappers.<TeachingClassDO>lambdaQuery()
                .select(TeachingClassDO::getCourseId)
                .eq(TeachingClassDO::getSemesterId, semesterId)
                .isNotNull(TeachingClassDO::getCourseId)
                .groupBy(TeachingClassDO::getCourseId));
        if (records == null) {
            return List.of();
        }
        return records.stream()
                .filter(Objects::nonNull)
                .map(each -> ((Number) each).longValue())
                .distinct()
                .toList();
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
