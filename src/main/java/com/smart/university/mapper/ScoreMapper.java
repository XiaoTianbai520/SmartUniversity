package com.smart.university.mapper;

import com.smart.university.domain.entity.ScoreDO;
import com.smart.university.domain.enums.ScoreStatusEnum;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 成绩持久层
 */
public interface ScoreMapper {

    /**
     * 根据 ID 查询成绩
     *
     * @param scoreId 成绩 ID
     * @return 成绩信息
     */
    ScoreDO getScoreById(Long scoreId);

    /**
     * 根据选课记录 ID 查询成绩
     *
     * @param courseSelectionId 选课记录 ID
     * @return 成绩信息
     */
    ScoreDO getScoreBySelectionId(Long courseSelectionId);

    /**
     * 批量查询成绩
     *
     * @param courseSelectionIds 选课记录 ID 集合
     * @return 成绩信息集合
     */
    List<ScoreDO> listScoreBySelectionIds(List<Long> courseSelectionIds);

    /**
     * 查询教学班下的全部成绩
     *
     * @param teachingClassId 教学班 ID
     * @return 成绩信息集合
     */
    List<ScoreDO> listScoreByTeachingClassId(Long teachingClassId);

    /**
     * 查询指定学生已发布的成绩
     *
     * @param requestParam 查询条件，已由后端填充 studentId 与 semesterId
     * @return 成绩信息集合
     */
    List<ScoreDO> listPublishedScoreByStudent(StudentSelectionQueryReqDTO requestParam);

    /**
     * 保存成绩
     *
     * @param requestParam 成绩数据对象
     * @return 影响行数
     */
    int saveScore(ScoreDO requestParam);

    /**
     * 更新成绩
     *
     * @param requestParam 成绩数据对象
     * @return 影响行数
     */
    int updateScore(ScoreDO requestParam);

    /**
     * 批量发布成绩
     *
     * @param courseSelectionIds 选课记录 ID 集合
     * @param status             目标成绩状态
     * @param publishedAt        发布时间
     * @return 影响行数
     */
    int updateScoreStatusBySelectionIds(@Param("courseSelectionIds") List<Long> courseSelectionIds,
                                        @Param("status") ScoreStatusEnum status,
                                        @Param("publishedAt") LocalDateTime publishedAt);
}
