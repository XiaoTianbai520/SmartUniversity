package com.smart.university.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.ScheduleConflictQueryReqDTO;
import com.smart.university.domain.entity.TeachingClassScheduleDO;

import java.util.List;

/**
 * 教学班排课持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 冲突判定中的同学期过滤通过子查询实现，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface TeachingClassScheduleMapper extends BaseMapper<TeachingClassScheduleDO> {

    /**
     * 根据 ID 查询排课
     *
     * @param scheduleId 排课 ID
     * @return 排课信息
     */
    default TeachingClassScheduleDO getScheduleById(Long scheduleId) {
        return selectById(scheduleId);
    }

    /**
     * 查询教学班下的全部排课
     *
     * @param teachingClassId 教学班 ID
     * @return 排课信息集合
     */
    default List<TeachingClassScheduleDO> listScheduleByTeachingClassId(Long teachingClassId) {
        return selectList(Wrappers.<TeachingClassScheduleDO>lambdaQuery()
                .eq(TeachingClassScheduleDO::getTeachingClassId, teachingClassId)
                .orderByAsc(TeachingClassScheduleDO::getWeekday)
                .orderByAsc(TeachingClassScheduleDO::getStartSection));
    }

    /**
     * 批量查询多个教学班的排课
     *
     * @param teachingClassIds 教学班 ID 集合
     * @return 排课信息集合
     */
    default List<TeachingClassScheduleDO> listScheduleByTeachingClassIds(List<Long> teachingClassIds) {
        return selectList(Wrappers.<TeachingClassScheduleDO>lambdaQuery()
                .in(TeachingClassScheduleDO::getTeachingClassId, teachingClassIds)
                .orderByAsc(TeachingClassScheduleDO::getWeekday)
                .orderByAsc(TeachingClassScheduleDO::getStartSection));
    }

    /**
     * 查询指定学期下的全部排课，学期过滤通过教学班子查询实现
     *
     * @param semesterId 学期 ID
     * @return 排课信息集合
     */
    default List<TeachingClassScheduleDO> listScheduleBySemesterId(Long semesterId) {
        return selectList(Wrappers.<TeachingClassScheduleDO>lambdaQuery()
                .inSql(TeachingClassScheduleDO::getTeachingClassId,
                        "SELECT id FROM teaching_class WHERE semester_id = " + semesterId)
                .orderByAsc(TeachingClassScheduleDO::getWeekday)
                .orderByAsc(TeachingClassScheduleDO::getStartSection));
    }

    /**
     * 查询教室下的全部排课
     *
     * @param classroomId 教室 ID
     * @return 排课信息集合
     */
    default List<TeachingClassScheduleDO> listScheduleByClassroomId(Long classroomId) {
        return selectList(Wrappers.<TeachingClassScheduleDO>lambdaQuery()
                .eq(TeachingClassScheduleDO::getClassroomId, classroomId)
                .orderByAsc(TeachingClassScheduleDO::getWeekday)
                .orderByAsc(TeachingClassScheduleDO::getStartSection));
    }

    /**
     * 批量查询多个教室下的全部排课
     *
     * @param classroomIds 教室 ID 集合
     * @return 排课信息集合
     */
    default List<TeachingClassScheduleDO> listScheduleByClassroomIds(List<Long> classroomIds) {
        if (classroomIds == null || classroomIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<TeachingClassScheduleDO>lambdaQuery()
                .in(TeachingClassScheduleDO::getClassroomId, classroomIds)
                .orderByAsc(TeachingClassScheduleDO::getWeekday)
                .orderByAsc(TeachingClassScheduleDO::getStartSection));
    }

    /**
     * 查询指定学期下指定星期的全部排课
     *
     * @param semesterId 学期 ID
     * @param weekday    星期：1周一...7周日
     * @return 排课信息集合
     */
    default List<TeachingClassScheduleDO> listScheduleBySemesterIdAndWeekday(Long semesterId, Integer weekday) {
        return selectList(Wrappers.<TeachingClassScheduleDO>lambdaQuery()
                .inSql(TeachingClassScheduleDO::getTeachingClassId,
                        "SELECT id FROM teaching_class WHERE semester_id = " + semesterId)
                .eq(TeachingClassScheduleDO::getWeekday, weekday)
                .orderByAsc(TeachingClassScheduleDO::getStartSection));
    }

    /**
     * 保存排课
     *
     * @param requestParam 排课数据对象
     * @return 影响行数
     */
    default int saveSchedule(TeachingClassScheduleDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新排课
     *
     * @param requestParam 排课数据对象
     * @return 影响行数
     */
    default int updateSchedule(TeachingClassScheduleDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 删除排课
     *
     * @param scheduleId 排课 ID
     * @return 影响行数
     */
    default int removeSchedule(Long scheduleId) {
        return deleteById(scheduleId);
    }

    /**
     * 统计与当前排课存在教师时间冲突的排课数量
     *
     * @param requestParam 冲突查询条件
     * @return 数量
     */
    default long countTeacherConflictSchedule(ScheduleConflictQueryReqDTO requestParam) {
        return selectCount(buildConflictWrapper(requestParam,
                "SELECT id FROM teaching_class WHERE teacher_id = " + requestParam.getTeacherId()
                        + buildSemesterSubQuery(requestParam.getTeachingClassId())));
    }

    /**
     * 统计与当前排课存在教室时间冲突的排课数量
     *
     * @param requestParam 冲突查询条件
     * @return 数量
     */
    default long countClassroomConflictSchedule(ScheduleConflictQueryReqDTO requestParam) {
        LambdaQueryWrapper<TeachingClassScheduleDO> queryWrapper = buildConflictWrapper(requestParam,
                "SELECT id FROM teaching_class WHERE 1 = 1" + buildSemesterSubQuery(requestParam.getTeachingClassId()));
        queryWrapper.eq(requestParam.getClassroomId() != null, TeachingClassScheduleDO::getClassroomId,
                requestParam.getClassroomId());
        return selectCount(queryWrapper);
    }

    /**
     * 构建排课冲突查询条件：星期相同、教学周有交集、节次有交集，并排除记录自身
     *
     * @param requestParam      冲突查询条件
     * @param teachingClassSql  限定教学班子查询
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<TeachingClassScheduleDO> buildConflictWrapper(ScheduleConflictQueryReqDTO requestParam,
                                                                             String teachingClassSql) {
        LambdaQueryWrapper<TeachingClassScheduleDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.inSql(TeachingClassScheduleDO::getTeachingClassId, teachingClassSql);
        queryWrapper.ne(requestParam.getExcludeScheduleId() != null, TeachingClassScheduleDO::getId,
                requestParam.getExcludeScheduleId());
        queryWrapper.eq(requestParam.getWeekday() != null, TeachingClassScheduleDO::getWeekday,
                requestParam.getWeekday());
        queryWrapper.le(requestParam.getEndWeek() != null, TeachingClassScheduleDO::getStartWeek,
                requestParam.getEndWeek());
        queryWrapper.ge(requestParam.getStartWeek() != null, TeachingClassScheduleDO::getEndWeek,
                requestParam.getStartWeek());
        queryWrapper.le(requestParam.getEndSection() != null, TeachingClassScheduleDO::getStartSection,
                requestParam.getEndSection());
        queryWrapper.ge(requestParam.getStartSection() != null, TeachingClassScheduleDO::getEndSection,
                requestParam.getStartSection());
        return queryWrapper;
    }

    /**
     * 构建同学期过滤子查询
     *
     * @param teachingClassId 当前教学班 ID
     * @return 子查询片段
     */
    default String buildSemesterSubQuery(Long teachingClassId) {
        if (teachingClassId == null) {
            return "";
        }
        return " AND semester_id = (SELECT semester_id FROM teaching_class WHERE id = " + teachingClassId + ")";
    }
}
