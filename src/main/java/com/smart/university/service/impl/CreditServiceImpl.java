package com.smart.university.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.ScoreDO;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.dto.resp.CreditSummaryRespDTO;
import com.smart.university.domain.enums.CourseTypeEnum;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.domain.enums.ScoreStatusEnum;
import com.smart.university.mapper.CourseMapper;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.ScoreMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.service.CreditService;
import com.smart.university.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学分统计服务实现
 */
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final CourseSelectionMapper courseSelectionMapper;

    private final TeachingClassMapper teachingClassMapper;

    private final CourseMapper courseMapper;

    private final ScoreMapper scoreMapper;

    private final SemesterService semesterService;

    @Override
    public CreditSummaryRespDTO getCreditSummary(Long semesterId) {
        Long studentId = UserContextHolder.getStudentId();
        if (studentId == null) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_EXIST);
        }
        Long targetSemesterId = semesterId == null ? semesterService.getCurrentSemester().getId() : semesterId;
        SemesterDO semesterDO = semesterService.getSemesterById(targetSemesterId);

        StudentSelectionQueryReqDTO queryParam = new StudentSelectionQueryReqDTO();
        queryParam.setStudentId(studentId);
        queryParam.setSemesterId(targetSemesterId);
        queryParam.setStatus(SelectionStatusEnum.SELECTED.name());
        List<CourseSelectionDO> selectionDOList = courseSelectionMapper.listSelectionByStudent(queryParam);
        BigDecimal currentSelectedCredit = sumCreditOfSelection(selectionDOList);

        List<ScoreDO> publishedScoreDOList = scoreMapper.listPublishedScoreByStudent(queryParam).stream()
                .filter(each -> ScoreStatusEnum.PUBLISHED == each.getStatus())
                .toList();
        Map<Long, CourseDO> courseMap = buildCourseMapOfSelection(
                courseSelectionMapper.listSelectionByStudent(buildAllSelectionParam(studentId, targetSemesterId)));
        BigDecimal completedCredit = BigDecimal.ZERO;
        BigDecimal requiredCredit = BigDecimal.ZERO;
        BigDecimal majorElectiveCredit = BigDecimal.ZERO;
        BigDecimal publicElectiveCredit = BigDecimal.ZERO;
        for (ScoreDO each : publishedScoreDOList) {
            CourseSelectionDO selectionDO = courseSelectionMapper.getSelectionById(each.getCourseSelectionId());
            if (selectionDO == null) {
                continue;
            }
            TeachingClassDO teachingClassDO = teachingClassMapper.getTeachingClassById(selectionDO.getTeachingClassId());
            CourseDO courseDO = teachingClassDO == null ? null : courseMap.get(teachingClassDO.getCourseId());
            if (courseDO == null || courseDO.getCredit() == null) {
                continue;
            }
            completedCredit = completedCredit.add(courseDO.getCredit());
            if (CourseTypeEnum.REQUIRED == courseDO.getCourseType()) {
                requiredCredit = requiredCredit.add(courseDO.getCredit());
            } else if (CourseTypeEnum.MAJOR_ELECTIVE == courseDO.getCourseType()) {
                majorElectiveCredit = majorElectiveCredit.add(courseDO.getCredit());
            } else if (CourseTypeEnum.PUBLIC_ELECTIVE == courseDO.getCourseType()) {
                publicElectiveCredit = publicElectiveCredit.add(courseDO.getCredit());
            }
        }

        CreditSummaryRespDTO result = new CreditSummaryRespDTO();
        result.setSemesterId(targetSemesterId);
        result.setMaxSelectionCredit(semesterDO.getMaxSelectionCredit());
        result.setCurrentSelectedCredit(currentSelectedCredit);
        result.setCompletedCredit(completedCredit);
        result.setRequiredCredit(requiredCredit);
        result.setMajorElectiveCredit(majorElectiveCredit);
        result.setPublicElectiveCredit(publicElectiveCredit);
        return result;
    }

    private StudentSelectionQueryReqDTO buildAllSelectionParam(Long studentId, Long semesterId) {
        StudentSelectionQueryReqDTO result = new StudentSelectionQueryReqDTO();
        result.setStudentId(studentId);
        result.setSemesterId(semesterId);
        return result;
    }

    /**
     * 统计选课记录对应课程的学分总和
     *
     * @param selectionDOList 选课记录集合
     * @return 学分总和
     */
    private BigDecimal sumCreditOfSelection(List<CourseSelectionDO> selectionDOList) {
        if (CollUtil.isEmpty(selectionDOList)) {
            return BigDecimal.ZERO;
        }
        List<Long> teachingClassIds = selectionDOList.stream()
                .map(CourseSelectionDO::getTeachingClassId).distinct().toList();
        List<TeachingClassDO> teachingClassDOList = teachingClassMapper.listTeachingClassByIds(teachingClassIds);
        List<Long> courseIds = teachingClassDOList.stream().map(TeachingClassDO::getCourseId).distinct().toList();
        if (CollUtil.isEmpty(courseIds)) {
            return BigDecimal.ZERO;
        }
        return courseMapper.listCourseByIds(courseIds).stream()
                .map(CourseDO::getCredit)
                .filter(each -> each != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 构建选课记录关联课程映射
     *
     * @param selectionDOList 选课记录集合
     * @return 课程 ID 与课程信息的映射
     */
    private Map<Long, CourseDO> buildCourseMapOfSelection(List<CourseSelectionDO> selectionDOList) {
        if (CollUtil.isEmpty(selectionDOList)) {
            return Map.of();
        }
        List<Long> teachingClassIds = selectionDOList.stream()
                .map(CourseSelectionDO::getTeachingClassId).distinct().toList();
        List<Long> courseIds = teachingClassMapper.listTeachingClassByIds(teachingClassIds).stream()
                .map(TeachingClassDO::getCourseId).distinct().toList();
        if (CollUtil.isEmpty(courseIds)) {
            return Map.of();
        }
        return courseMapper.listCourseByIds(courseIds).stream()
                .collect(Collectors.toMap(CourseDO::getId, Function.identity()));
    }
}
