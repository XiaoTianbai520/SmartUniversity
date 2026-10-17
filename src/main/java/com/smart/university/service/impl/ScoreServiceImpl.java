package com.smart.university.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.event.NoticeEvent;
import com.smart.university.domain.event.NoticeEventPublisher;
import com.smart.university.domain.event.NoticeTargetResolver;
import com.smart.university.domain.event.ScorePublishedEvent;
import com.smart.university.domain.enums.NoticeTypeEnum;
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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.university.service.ScoreService;
import com.smart.university.service.SemesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
public class ScoreServiceImpl extends ServiceImpl<ScoreMapper, ScoreDO> implements ScoreService {

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

    private final NoticeEventPublisher noticeEventPublisher;

    private final NoticeTargetResolver noticeTargetResolver;

    private final ApplicationEventPublisher applicationEventPublisher;

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
        // 以教学班全部「已选」学生为基准，保证未录入成绩的学生也一并返回（见接口文档 §7.5）
        CourseSelectionDO selectionParam = new CourseSelectionDO();
        selectionParam.setTeachingClassId(teachingClassId);
        selectionParam.setStatus(SelectionStatusEnum.SELECTED);
        List<CourseSelectionDO> selectionList = courseSelectionMapper.listSelectionByCondition(selectionParam);
        if (CollUtil.isEmpty(selectionList)) {
            return List.of();
        }
        // 已录入的成绩按选课记录 ID 建立索引，便于未录入成绩的学生映射为 null
        List<Long> selectionIds = selectionList.stream().map(CourseSelectionDO::getId).toList();
        Map<Long, ScoreDO> scoreMap = scoreMapper.listScoreBySelectionIds(selectionIds).stream()
                .collect(Collectors.toMap(ScoreDO::getCourseSelectionId, Function.identity(), (a, b) -> a));
        // 学生信息映射
        Map<Long, StudentDO> studentMap = studentMapper.listStudentByIds(
                        selectionList.stream().map(CourseSelectionDO::getStudentId).distinct().toList()).stream()
                .collect(Collectors.toMap(StudentDO::getId, Function.identity(), (a, b) -> a));
        return selectionList.stream().map(each -> {
            StudentDO studentDO = studentMap.get(each.getStudentId());
            ScoreDO scoreDO = scoreMap.get(each.getId());
            TeacherScoreRespDTO result = new TeacherScoreRespDTO();
            result.setSelectionId(each.getId());
            result.setStudentNo(studentDO == null ? null : studentDO.getStudentNo());
            result.setStudentName(studentDO == null ? null : studentDO.getStudentName());
            result.setScore(scoreDO == null ? null : scoreDO.getScoreValue());
            result.setStatus(scoreDO == null || scoreDO.getStatus() == null ? null : scoreDO.getStatus().name());
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
        publishScoreNotice(teachingClassId, selectionIds, publishedCount, now);
        // A-06：成绩发布后发布统计事件，由监听器在事务提交后触发自动统计，不改动发布主逻辑
        TeachingClassDO publishedClassDO = teachingClassMapper.getTeachingClassById(teachingClassId);
        applicationEventPublisher.publishEvent(ScorePublishedEvent.builder()
                .teachingClassId(teachingClassId)
                .selectionIds(selectionIds)
                .publishedCount(publishedCount)
                .publishedAt(now)
                .semesterId(publishedClassDO == null ? null : publishedClassDO.getSemesterId())
                .build());
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
     * 发布成绩通知，仅通知本次真正被发布的学生，通知失败不影响成绩发布结果
     *
     * @param teachingClassId 教学班 ID
     * @param selectionIds    本次参与发布的选课记录 ID 集合
     * @param publishedCount  本次发布成功的条数
     * @param publishedAt     本次发布时间
     */
    private void publishScoreNotice(Long teachingClassId, List<Long> selectionIds, int publishedCount,
                                    LocalDateTime publishedAt) {
        if (publishedCount <= 0) {
            return;
        }
        try {
            List<Long> publishedSelectionIds = scoreMapper.listScoreBySelectionIds(selectionIds).stream()
                    .filter(each -> ScoreStatusEnum.PUBLISHED == each.getStatus()
                            && publishedAt.equals(each.getPublishedAt()))
                    .map(ScoreDO::getCourseSelectionId)
                    .toList();
            if (CollUtil.isEmpty(publishedSelectionIds)) {
                return;
            }
            String courseName = getCourseName(teachingClassId);
            noticeEventPublisher.publish(NoticeEvent.builder()
                    .noticeType(NoticeTypeEnum.SCORE_PUBLISHED)
                    .title(NoticeTypeEnum.SCORE_PUBLISHED.getDefaultTitle())
                    .content(StrUtil.format("《{}》成绩已发布，请前往查看。", courseName))
                    .receiverUserIds(noticeTargetResolver.listUserIdsBySelectionIds(publishedSelectionIds))
                    .receiverRole(RoleEnum.STUDENT)
                    .bizId(teachingClassId)
                    .build());
        } catch (Exception ex) {
            log.error("成绩发布通知发布失败，teachingClassId：{}", teachingClassId, ex);
        }
    }

    /**
     * 获取教学班所属课程名称，课程缺失时返回空串
     *
     * @param teachingClassId 教学班 ID
     * @return 课程名称
     */
    private String getCourseName(Long teachingClassId) {
        TeachingClassDO teachingClassDO = teachingClassMapper.getTeachingClassById(teachingClassId);
        if (teachingClassDO == null) {
            return StrUtil.EMPTY;
        }
        CourseDO courseDO = courseMapper.getCourseById(teachingClassDO.getCourseId());
        return courseDO == null ? StrUtil.EMPTY : courseDO.getCourseName();
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
