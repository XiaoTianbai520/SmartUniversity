package com.smart.university.service;

import com.smart.university.domain.dto.req.ScoreSaveReqDTO;
import com.smart.university.domain.dto.resp.ScorePublishRespDTO;
import com.smart.university.domain.dto.resp.ScoreSaveRespDTO;
import com.smart.university.domain.dto.resp.StudentScoreRespDTO;
import com.smart.university.domain.dto.resp.TeacherScoreRespDTO;

import java.util.List;

/**
 * 成绩服务
 */
public interface ScoreService {

    /**
     * 教师保存学生成绩
     *
     * @param teachingClassId 教学班 ID
     * @param selectionId     选课记录 ID
     * @param requestParam    成绩入参
     * @return 成绩保存结果
     */
    ScoreSaveRespDTO saveScore(Long teachingClassId, Long selectionId, ScoreSaveReqDTO requestParam);

    /**
     * 查询教学班成绩列表
     *
     * @param teachingClassId 教学班 ID
     * @return 成绩列表
     */
    List<TeacherScoreRespDTO> listTeacherScore(Long teachingClassId);

    /**
     * 发布教学班成绩
     *
     * @param teachingClassId 教学班 ID
     * @return 发布结果
     */
    ScorePublishRespDTO publishScore(Long teachingClassId);

    /**
     * 查询我的已发布成绩
     *
     * @param semesterId 学期 ID，为空时使用当前学期
     * @return 成绩集合
     */
    List<StudentScoreRespDTO> listStudentScore(Long semesterId);
}
