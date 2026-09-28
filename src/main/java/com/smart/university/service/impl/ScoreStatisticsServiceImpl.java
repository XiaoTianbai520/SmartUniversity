package com.smart.university.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.dto.resp.ScoreDistributionItemDTO;
import com.smart.university.domain.dto.resp.ScoreStatisticsRespDTO;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.ScoreDO;
import com.smart.university.domain.entity.TeacherDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.mapper.CourseMapper;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.ScoreMapper;
import com.smart.university.mapper.TeacherMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.service.ScoreStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 成绩统计服务实现，统计口径以单个教学班为单位，数据量级在数百条以内，在内存中完成聚合
 */
@Service
@RequiredArgsConstructor
public class ScoreStatisticsServiceImpl implements ScoreStatisticsService {

    /**
     * 及格分数线
     */
    private static final BigDecimal PASS_SCORE_LINE = new BigDecimal("60");

    /**
     * 优秀分数线
     */
    private static final BigDecimal EXCELLENT_SCORE_LINE = new BigDecimal("85");

    /**
     * 百分比基数
     */
    private static final BigDecimal PERCENT_BASE = new BigDecimal("100");

    /**
     * 平均分与比率统一保留的小数位
     */
    private static final int STATISTICS_SCALE = 1;

    /**
     * 无成绩时统一返回的零值，避免出现 null 导致前端画图异常
     */
    private static final BigDecimal ZERO_STATISTICS = new BigDecimal("0.0");

    /**
     * 成绩分布区间定义，每项为 { 区间名称, 下界, 上界 }
     */
    private static final String[][] DISTRIBUTION_RANGES = {
            {"0-59", "0", "59"},
            {"60-69", "60", "69"},
            {"70-79", "70", "79"},
            {"80-89", "80", "89"},
            {"90-100", "90", "100"}
    };

    private final CourseSelectionMapper courseSelectionMapper;

    private final ScoreMapper scoreMapper;

    private final TeachingClassMapper teachingClassMapper;

    private final CourseMapper courseMapper;

    private final TeacherMapper teacherMapper;

    @Override
    public ScoreStatisticsRespDTO getScoreStatistics(Long teachingClassId) {
        TeachingClassDO teachingClassDO = getOwnedTeachingClass(teachingClassId);
        CourseSelectionDO selectionParam = new CourseSelectionDO();
        selectionParam.setTeachingClassId(teachingClassId);
        selectionParam.setStatus(SelectionStatusEnum.SELECTED);
        List<CourseSelectionDO> selectedList = courseSelectionMapper.listSelectionByCondition(selectionParam);
        int selectedCount = selectedList.size();
        List<Long> selectionIds = selectedList.stream().map(CourseSelectionDO::getId).toList();
        List<BigDecimal> scoreValues = listScoredValues(selectionIds);
        int scoredCount = scoreValues.size();
        ScoreStatisticsRespDTO result = new ScoreStatisticsRespDTO();
        result.setTeachingClassId(teachingClassDO.getId());
        result.setTeachingClassName(teachingClassDO.getClassName());
        result.setCourseName(getCourseName(teachingClassDO.getCourseId()));
        result.setTeacherName(getTeacherName(teachingClassDO.getTeacherId()));
        result.setSelectedCount(selectedCount);
        result.setScoredCount(scoredCount);
        result.setUnscoredCount(selectedCount - scoredCount);
        result.setAverageScore(calcAverageScore(scoreValues));
        result.setMaxScore(calcMaxScore(scoreValues));
        result.setMinScore(calcMinScore(scoreValues));
        result.setPassRate(calcRate(countScoreGreaterOrEqual(scoreValues, PASS_SCORE_LINE), scoredCount));
        result.setExcellentRate(calcRate(countScoreGreaterOrEqual(scoreValues, EXCELLENT_SCORE_LINE), scoredCount));
        result.setDistribution(buildDistribution(scoreValues));
        return result;
    }

    /**
     * 查询归属当前登录教师的教学班
     *
     * @param teachingClassId 教学班 ID
     * @return 教学班信息
     */
    private TeachingClassDO getOwnedTeachingClass(Long teachingClassId) {
        Long teacherId = UserContextHolder.getTeacherId();
        if (teacherId == null) {
            throw new BizException(ResultCodeEnum.NO_TEACHING_CLASS_PERMISSION);
        }
        TeachingClassDO result = teachingClassMapper.getTeachingClassById(teachingClassId);
        if (result == null) {
            throw new BizException(ResultCodeEnum.TEACHING_CLASS_NOT_EXIST);
        }
        if (!teacherId.equals(result.getTeacherId())) {
            throw new BizException(ResultCodeEnum.NO_TEACHING_CLASS_PERMISSION);
        }
        return result;
    }

    /**
     * 查询已录入的成绩值，成绩值为空的记录视为未录入
     *
     * @param selectionIds 选课记录 ID 集合
     * @return 成绩值集合
     */
    private List<BigDecimal> listScoredValues(List<Long> selectionIds) {
        if (CollUtil.isEmpty(selectionIds)) {
            return List.of();
        }
        return scoreMapper.listScoreBySelectionIds(selectionIds).stream()
                .map(ScoreDO::getScoreValue)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 计算平均分
     *
     * @param scoreValues 成绩值集合
     * @return 平均分，无成绩时返回 0
     */
    private BigDecimal calcAverageScore(List<BigDecimal> scoreValues) {
        if (CollUtil.isEmpty(scoreValues)) {
            return ZERO_STATISTICS;
        }
        BigDecimal totalScore = scoreValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalScore.divide(BigDecimal.valueOf(scoreValues.size()), STATISTICS_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 计算最高分
     *
     * @param scoreValues 成绩值集合
     * @return 最高分，无成绩时返回 0
     */
    private BigDecimal calcMaxScore(List<BigDecimal> scoreValues) {
        if (CollUtil.isEmpty(scoreValues)) {
            return ZERO_STATISTICS;
        }
        return scoreValues.stream().max(BigDecimal::compareTo).orElse(ZERO_STATISTICS);
    }

    /**
     * 计算最低分
     *
     * @param scoreValues 成绩值集合
     * @return 最低分，无成绩时返回 0
     */
    private BigDecimal calcMinScore(List<BigDecimal> scoreValues) {
        if (CollUtil.isEmpty(scoreValues)) {
            return ZERO_STATISTICS;
        }
        return scoreValues.stream().min(BigDecimal::compareTo).orElse(ZERO_STATISTICS);
    }

    /**
     * 计算占比，分母为 0 时返回 0
     *
     * @param count       分子数量
     * @param scoredCount 已录入成绩人数
     * @return 百分比占比，保留一位小数
     */
    private BigDecimal calcRate(int count, int scoredCount) {
        if (scoredCount <= 0) {
            return ZERO_STATISTICS;
        }
        return BigDecimal.valueOf(count).multiply(PERCENT_BASE)
                .divide(BigDecimal.valueOf(scoredCount), STATISTICS_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 统计达到指定分数线的人数
     *
     * @param scoreValues 成绩值集合
     * @param scoreLine   分数线
     * @return 人数
     */
    private int countScoreGreaterOrEqual(List<BigDecimal> scoreValues, BigDecimal scoreLine) {
        return (int) scoreValues.stream().filter(each -> each.compareTo(scoreLine) >= 0).count();
    }

    /**
     * 构建成绩分布，无论是否有成绩都固定返回五段
     *
     * @param scoreValues 成绩值集合
     * @return 成绩分布集合
     */
    private List<ScoreDistributionItemDTO> buildDistribution(List<BigDecimal> scoreValues) {
        int scoredCount = scoreValues.size();
        List<ScoreDistributionItemDTO> result = new ArrayList<>(DISTRIBUTION_RANGES.length);
        for (String[] each : DISTRIBUTION_RANGES) {
            BigDecimal lowerScore = new BigDecimal(each[1]);
            BigDecimal upperScore = new BigDecimal(each[2]);
            int count = (int) scoreValues.stream()
                    .filter(score -> score.compareTo(lowerScore) >= 0 && score.compareTo(upperScore) <= 0)
                    .count();
            ScoreDistributionItemDTO distributionItem = new ScoreDistributionItemDTO();
            distributionItem.setRange(each[0]);
            distributionItem.setCount(count);
            distributionItem.setRate(calcRate(count, scoredCount));
            result.add(distributionItem);
        }
        return result;
    }

    /**
     * 获取课程名称
     *
     * @param courseId 课程 ID
     * @return 课程名称
     */
    private String getCourseName(Long courseId) {
        if (courseId == null) {
            return null;
        }
        CourseDO result = courseMapper.getCourseById(courseId);
        return result == null ? null : result.getCourseName();
    }

    /**
     * 获取教师姓名
     *
     * @param teacherId 教师 ID
     * @return 教师姓名
     */
    private String getTeacherName(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        TeacherDO result = teacherMapper.getTeacherById(teacherId);
        return result == null ? null : result.getTeacherName();
    }
}
