package com.smart.university.service;

import com.smart.university.domain.dto.req.CourseSelectionAnalysisQueryReqDTO;
import com.smart.university.domain.dto.resp.CourseSelectionAnalysisPageRespDTO;
import com.smart.university.domain.entity.StatCourseSelectionDO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 课程选课分析服务（S-05）
 */
public interface CourseSelectionAnalysisService {

    /**
     * 分页查询课程选课分析
     *
     * @param requestParam 查询条件
     * @return 课程选课分析分页结果与口径回显
     */
    CourseSelectionAnalysisPageRespDTO pageCourseSelectionAnalysis(CourseSelectionAnalysisQueryReqDTO requestParam);

    /**
     * 查询本学期整体满班率
     *
     * @param semesterId 学期 ID，为空取当前学期
     * @return 满班率百分比，保留 1 位小数
     */
    BigDecimal getFullClassRate(Long semesterId);

    /**
     * 构建课程选课分析快照数据
     *
     * @param semesterId 学期 ID
     * @return 课程选课分析快照集合
     */
    List<StatCourseSelectionDO> listCourseSelectionSnapshot(Long semesterId);
}
