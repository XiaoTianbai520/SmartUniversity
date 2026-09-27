package com.smart.university.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.entity.ScoreDO;
import com.smart.university.domain.enums.ScoreStatusEnum;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 成绩持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 跨表过滤通过子查询实现，方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface ScoreMapper extends BaseMapper<ScoreDO> {

    /**
     * 根据 ID 查询成绩
     *
     * @param scoreId 成绩 ID
     * @return 成绩信息
     */
    default ScoreDO getScoreById(Long scoreId) {
        return selectById(scoreId);
    }

    /**
     * 根据选课记录 ID 查询成绩
     *
     * @param courseSelectionId 选课记录 ID
     * @return 成绩信息
     */
    default ScoreDO getScoreBySelectionId(Long courseSelectionId) {
        return selectOne(Wrappers.<ScoreDO>lambdaQuery()
                .eq(ScoreDO::getCourseSelectionId, courseSelectionId)
                .last("LIMIT 1"));
    }

    /**
     * 根据选课记录 ID 集合批量查询成绩
     *
     * @param courseSelectionIds 选课记录 ID 集合
     * @return 成绩信息集合
     */
    default List<ScoreDO> listScoreBySelectionIds(List<Long> courseSelectionIds) {
        return selectList(Wrappers.<ScoreDO>lambdaQuery().in(ScoreDO::getCourseSelectionId, courseSelectionIds));
    }

    /**
     * 查询教学班下的全部成绩
     *
     * @param teachingClassId 教学班 ID
     * @return 成绩信息集合
     */
    default List<ScoreDO> listScoreByTeachingClassId(Long teachingClassId) {
        return selectList(Wrappers.<ScoreDO>lambdaQuery()
                .inSql(ScoreDO::getCourseSelectionId,
                        "SELECT id FROM course_selection WHERE teaching_class_id = " + teachingClassId)
                .orderByAsc(ScoreDO::getId));
    }

    /**
     * 查询学生在指定学期下已发布的成绩
     *
     * @param requestParam 查询条件
     * @return 成绩信息集合
     */
    default List<ScoreDO> listPublishedScoreByStudent(StudentSelectionQueryReqDTO requestParam) {
        LambdaQueryWrapper<ScoreDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(ScoreDO::getStatus, ScoreStatusEnum.PUBLISHED);
        if (requestParam.getStudentId() != null) {
            queryWrapper.inSql(ScoreDO::getCourseSelectionId,
                    "SELECT id FROM course_selection WHERE student_id = " + requestParam.getStudentId());
        }
        if (requestParam.getSemesterId() != null) {
            queryWrapper.inSql(ScoreDO::getCourseSelectionId,
                    "SELECT cs.id FROM course_selection cs "
                            + "JOIN teaching_class tc ON tc.id = cs.teaching_class_id "
                            + "WHERE tc.semester_id = " + requestParam.getSemesterId());
        }
        queryWrapper.orderByDesc(ScoreDO::getPublishedAt);
        return selectList(queryWrapper);
    }

    /**
     * 保存成绩
     *
     * @param requestParam 成绩数据对象
     * @return 影响行数
     */
    default int saveScore(ScoreDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新成绩
     *
     * @param requestParam 成绩数据对象
     * @return 影响行数
     */
    default int updateScore(ScoreDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 批量发布成绩，仅处理未发布的记录
     *
     * @param courseSelectionIds 选课记录 ID 集合
     * @param status             目标成绩状态
     * @param publishedAt        发布时间
     * @return 影响行数
     */
    default int updateScoreStatusBySelectionIds(List<Long> courseSelectionIds, ScoreStatusEnum status,
                                                LocalDateTime publishedAt) {
        LambdaUpdateWrapper<ScoreDO> updateWrapper = Wrappers.<ScoreDO>lambdaUpdate()
                .set(ScoreDO::getStatus, status)
                .set(ScoreDO::getPublishedAt, publishedAt)
                .eq(ScoreDO::getStatus, ScoreStatusEnum.UNPUBLISHED)
                .in(ScoreDO::getCourseSelectionId, courseSelectionIds);
        return update(null, updateWrapper);
    }
}
