package com.smart.university.mapper;

import com.smart.university.common.util.EnumParseUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.enums.SelectionStatusEnum;

import java.util.List;

/**
 * 选课记录持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface CourseSelectionMapper extends BaseMapper<CourseSelectionDO> {

    /**
     * 根据 ID 查询选课记录
     *
     * @param selectionId 选课记录 ID
     * @return 选课记录信息
     */
    default CourseSelectionDO getSelectionById(Long selectionId) {
        return selectById(selectionId);
    }

    /**
     * 根据学生与教学班查询选课记录，用于重复选课校验
     *
     * @param requestParam 选课记录数据对象
     * @return 选课记录信息
     */
    default CourseSelectionDO getSelectionByStudentAndClass(CourseSelectionDO requestParam) {
        return selectOne(Wrappers.<CourseSelectionDO>lambdaQuery()
                .eq(CourseSelectionDO::getStudentId, requestParam.getStudentId())
                .eq(CourseSelectionDO::getTeachingClassId, requestParam.getTeachingClassId())
                .last("LIMIT 1"));
    }

    /**
     * 按条件统计选课记录数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    default long countSelectionByCondition(CourseSelectionDO requestParam) {
        return selectCount(buildQueryWrapper(requestParam));
    }

    /**
     * 按条件查询选课记录
     *
     * @param requestParam 查询条件
     * @return 选课记录集合
     */
    default List<CourseSelectionDO> listSelectionByCondition(CourseSelectionDO requestParam) {
        return selectList(buildQueryWrapper(requestParam));
    }

    /**
     * 查询学生在指定学期下的选课记录
     *
     * @param requestParam 查询条件
     * @return 选课记录集合
     */
    default List<CourseSelectionDO> listSelectionByStudent(StudentSelectionQueryReqDTO requestParam) {
        return selectList(buildStudentQueryWrapper(requestParam));
    }

    /**
     * 统计教学班已选人数
     *
     * @param teachingClassId 教学班 ID
     * @return 已选人数
     */
    default long countSelectedByTeachingClassId(Long teachingClassId) {
        return selectCount(Wrappers.<CourseSelectionDO>lambdaQuery()
                .eq(CourseSelectionDO::getTeachingClassId, teachingClassId)
                .eq(CourseSelectionDO::getStatus, SelectionStatusEnum.SELECTED));
    }

    /**
     * 统计学生在指定学期下满足条件的选课数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    default long countSelectedByStudent(StudentSelectionQueryReqDTO requestParam) {
        return selectCount(buildStudentQueryWrapper(requestParam));
    }

    /**
     * 保存选课记录
     *
     * @param requestParam 选课记录数据对象
     * @return 影响行数
     */
    default int saveSelection(CourseSelectionDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新选课记录
     *
     * @param requestParam 选课记录数据对象
     * @return 影响行数
     */
    default int updateSelection(CourseSelectionDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 构建选课记录查询条件
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<CourseSelectionDO> buildQueryWrapper(CourseSelectionDO requestParam) {
        LambdaQueryWrapper<CourseSelectionDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(requestParam.getStudentId() != null, CourseSelectionDO::getStudentId, requestParam.getStudentId());
        queryWrapper.eq(requestParam.getTeachingClassId() != null, CourseSelectionDO::getTeachingClassId,
                requestParam.getTeachingClassId());
        queryWrapper.eq(requestParam.getBatchId() != null, CourseSelectionDO::getBatchId, requestParam.getBatchId());
        queryWrapper.eq(requestParam.getStatus() != null, CourseSelectionDO::getStatus, requestParam.getStatus());
        queryWrapper.orderByAsc(CourseSelectionDO::getId);
        return queryWrapper;
    }

    /**
     * 构建学生维度选课查询条件，学期过滤通过教学班子查询实现
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<CourseSelectionDO> buildStudentQueryWrapper(StudentSelectionQueryReqDTO requestParam) {
        LambdaQueryWrapper<CourseSelectionDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(requestParam.getStudentId() != null, CourseSelectionDO::getStudentId, requestParam.getStudentId());
        SelectionStatusEnum status = EnumParseUtil.parseOrNull(SelectionStatusEnum.class, requestParam.getStatus());
        queryWrapper.eq(status != null, CourseSelectionDO::getStatus, status);
        if (requestParam.getSemesterId() != null) {
            queryWrapper.inSql(CourseSelectionDO::getTeachingClassId,
                    "SELECT id FROM teaching_class WHERE semester_id = " + requestParam.getSemesterId());
        }
        queryWrapper.orderByDesc(CourseSelectionDO::getId);
        return queryWrapper;
    }
}
