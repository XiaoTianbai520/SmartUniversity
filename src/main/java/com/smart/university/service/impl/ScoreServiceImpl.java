package com.smart.university.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.ScoreDO;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.dto.req.ScoreSaveReqDTO;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.dto.resp.ScorePublishRespDTO;
import com.smart.university.domain.dto.resp.ScoreSaveRespDTO;
import com.smart.university.domain.dto.resp.StudentScoreRespDTO;
import com.smart.university.domain.dto.resp.TeacherScoreRespDTO;
import com.smart.university.domain.enums.ScoreStatusEnum;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.mapper.CourseMapper;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.ScoreMapper;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.mq.message.ScorePublishMessage;
import com.smart.university.mq.producer.ScorePublishProducer;
import com.smart.university.service.ScoreService;
import com.smart.university.service.SemesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 成绩服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {

    /**
     * 成绩最低分
     */
    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;

    /**
     * 成绩最高分
     */
    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    private final ScoreMapper scoreMapper;

    private final CourseSelectionMapper courseSelectionMapper;

    private final TeachingClassMapper teachingClassMapper;

    private final StudentMapper studentMapper;

    private final CourseMapper courseMapper;

    private final SemesterService semesterService;

    /**
     * 成绩发布消息生产者，未开启消息队列时容器中不存在该 Bean
     */
    private final ObjectProvider<ScorePublishProducer> scorePublishProducerProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScoreSaveRespDTO saveScore(Long teachingClassId, Long selectionId, ScoreSaveReqDTO requestParam) {
        checkTeachingClassOwner(teachingClassId);
        CourseSelectionDO selectionDO = courseSelectionMapper.getSelectionById(selectionId);
        if (selectionDO == null || !selectionDO.getTeachingClassId().equals(teachingClassId)) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_IN_CLASS);
        }
        if (SelectionStatusEnum.SELECTED != selectionDO.getStatus()) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "选课状态不是已选，不能录入成绩");
        }
        if (requestParam.getScore().compareTo(MIN_SCORE) < 0
                || requestParam.getScore().compareTo(MAX_SCORE) > 0) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "成绩必须在 0 到 100 之间");
        }
        ScoreDO existScoreDO = scoreMapper.getScoreBySelectionId(selectionId);
        if (existScoreDO != null && ScoreStatusEnum.PUBLISHED == existScoreDO.getStatus()) {
            throw new BizException(ResultCodeEnum.SCORE_PUBLISHED);
        }
        if (existScoreDO == null) {
            ScoreDO scoreDO = new ScoreDO();
            scoreDO.setCourseSelectionId(selectionId);
            scoreDO.setScoreValue(requestParam.getScore());
            scoreDO.setStatus(ScoreStatusEnum.UNPUBLISHED);
            scoreMapper.saveScore(scoreDO);
            existScoreDO = scoreDO;
        } else {
            ScoreDO updateScoreDO = new ScoreDO();
            updateScoreDO.setId(existScoreDO.getId());
            updateScoreDO.setScoreValue(requestParam.getScore());
            scoreMapper.updateScore(updateScoreDO);
            existScoreDO.setScoreValue(requestParam.getScore());
        }
        ScoreSaveRespDTO result = new ScoreSaveRespDTO();
        result.setScoreId(existScoreDO.getId());
        result.setSelectionId(selectionId);
        result.setScore(existScoreDO.getScoreValue());
        result.setStatus(ScoreStatusEnum.UNPUBLISHED.name());
        return result;
    }

    @Override
    public List<TeacherScoreRespDTO> listTeacherScore(Long teachingClassId) {
        checkTeachingClassOwner(teachingClassId);
        List<ScoreDO> scoreDOList = scoreMapper.listScoreByTeachingClassId(teachingClassId);
        if (CollUtil.isEmpty(scoreDOList)) {
            return List.of();
        }
        Map<Long, StudentDO> studentMap = buildStudentMap(scoreDOList);
        return scoreDOList.stream().map(each -> {
            CourseSelectionDO selectionDO = courseSelectionMapper.getSelectionById(each.getCourseSelectionId());
            StudentDO studentDO = selectionDO == null ? null : studentMap.get(selectionDO.getStudentId());
            TeacherScoreRespDTO result = new TeacherScoreRespDTO();
            result.setSelectionId(each.getCourseSelectionId());
            result.setStudentNo(studentDO == null ? null : studentDO.getStudentNo());
            result.setStudentName(studentDO == null ? null : studentDO.getStudentName());
            result.setScore(each.getScoreValue());
            result.setStatus(each.getStatus() == null ? null : each.getStatus().name());
            return result;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScorePublishRespDTO publishScore(Long teachingClassId) {
        checkTeachingClassOwner(teachingClassId);
        CourseSelectionDO selectionParam = new CourseSelectionDO();
        selectionParam.setTeachingClassId(teachingClassId);
        selectionParam.setStatus(SelectionStatusEnum.SELECTED);
        List<Long> selectionIds = courseSelectionMapper.listSelectionByCondition(selectionParam).stream()
                .map(CourseSelectionDO::getId).toList();
        if (CollUtil.isEmpty(selectionIds)) {
            ScorePublishRespDTO emptyResult = new ScorePublishRespDTO();
            emptyResult.setPublishedCount(0);
            return emptyResult;
        }
        LocalDateTime now = LocalDateTime.now();
        int publishedCount = scoreMapper.updateScoreStatusBySelectionIds(
                selectionIds, ScoreStatusEnum.PUBLISHED, now);
        ScorePublishProducer producer = scorePublishProducerProvider.getIfAvailable();
        if (producer != null) {
            try {
                producer.sendScorePublishMessage(ScorePublishMessage.builder()
                        .teachingClassId(teachingClassId)
                        .selectionIds(selectionIds)
                        .publishedCount(publishedCount)
                        .publishedAt(now)
                        .build());
            } catch (Exception ex) {
                log.error("成绩发布消息发送失败，teachingClassId：{}", teachingClassId, ex);
            }
        }
        ScorePublishRespDTO result = new ScorePublishRespDTO();
        result.setPublishedCount(publishedCount);
        return result;
    }

    @Override
    public List<StudentScoreRespDTO> listStudentScore(Long semesterId) {
        Long studentId = UserContextHolder.getStudentId();
        if (studentId == null) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_EXIST);
        }
        Long targetSemesterId = semesterId == null ? semesterService.getCurrentSemester().getId() : semesterId;
        StudentSelectionQueryReqDTO queryParam = new StudentSelectionQueryReqDTO();
        queryParam.setStudentId(studentId);
        queryParam.setSemesterId(targetSemesterId);
        List<ScoreDO> scoreDOList = scoreMapper.listPublishedScoreByStudent(queryParam);
        if (CollUtil.isEmpty(scoreDOList)) {
            return List.of();
        }
        Map<Long, CourseDO> courseMap = buildCourseMap(scoreDOList);
        return scoreDOList.stream().map(each -> {
            CourseSelectionDO selectionDO = courseSelectionMapper.getSelectionById(each.getCourseSelectionId());
            TeachingClassDO teachingClassDO = selectionDO == null ? null
                    : teachingClassMapper.getTeachingClassById(selectionDO.getTeachingClassId());
            CourseDO courseDO = teachingClassDO == null ? null : courseMap.get(teachingClassDO.getCourseId());
            StudentScoreRespDTO result = new StudentScoreRespDTO();
            result.setCourseName(courseDO == null ? null : courseDO.getCourseName());
            result.setCredit(courseDO == null ? null : courseDO.getCredit());
            result.setScore(each.getScoreValue());
            result.setPublishedAt(each.getPublishedAt());
            return result;
        }).toList();
    }

    /**
     * 校验教学班归属当前登录教师
     *
     * @param teachingClassId 教学班 ID
     */
    private void checkTeachingClassOwner(Long teachingClassId) {
        Long teacherId = UserContextHolder.getTeacherId();
        if (teacherId == null) {
            throw new BizException(ResultCodeEnum.NO_TEACHING_CLASS_PERMISSION);
        }
        TeachingClassDO teachingClassDO = teachingClassMapper.getTeachingClassById(teachingClassId);
        if (teachingClassDO == null) {
            throw new BizException(ResultCodeEnum.TEACHING_CLASS_NOT_EXIST);
        }
        if (!teacherId.equals(teachingClassDO.getTeacherId())) {
            throw new BizException(ResultCodeEnum.NO_TEACHING_CLASS_PERMISSION);
        }
    }

    /**
     * 构建成绩关联学生映射
     *
     * @param scoreDOList 成绩集合
     * @return 学生 ID 与学生信息的映射
     */
    private Map<Long, StudentDO> buildStudentMap(List<ScoreDO> scoreDOList) {
        List<Long> selectionIds = scoreDOList.stream().map(ScoreDO::getCourseSelectionId).toList();
        List<Long> studentIds = selectionIds.stream()
                .map(each -> courseSelectionMapper.getSelectionById(each))
                .filter(each -> each != null)
                .map(CourseSelectionDO::getStudentId).distinct().toList();
        if (CollUtil.isEmpty(studentIds)) {
            return Map.of();
        }
        return studentMapper.listStudentByIds(studentIds).stream()
                .collect(Collectors.toMap(StudentDO::getId, Function.identity()));
    }

    /**
     * 构建成绩关联课程映射
     *
     * @param scoreDOList 成绩集合
     * @return 课程 ID 与课程信息的映射
     */
    private Map<Long, CourseDO> buildCourseMap(List<ScoreDO> scoreDOList) {
        List<Long> courseIds = scoreDOList.stream()
                .map(each -> courseSelectionMapper.getSelectionById(each.getCourseSelectionId()))
                .filter(each -> each != null)
                .map(CourseSelectionDO::getTeachingClassId)
                .map(each -> teachingClassMapper.getTeachingClassById(each))
                .filter(each -> each != null)
                .map(TeachingClassDO::getCourseId).distinct().toList();
        if (CollUtil.isEmpty(courseIds)) {
            return Map.of();
        }
        return courseMapper.listCourseByIds(courseIds).stream()
                .collect(Collectors.toMap(CourseDO::getId, Function.identity()));
    }
}
