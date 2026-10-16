package com.smart.university.controller.admin;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.ClassroomRecommendQueryReqDTO;
import com.smart.university.domain.dto.req.EnrollmentForecastQueryReqDTO;
import com.smart.university.domain.dto.req.GraduationRiskQueryReqDTO;
import com.smart.university.domain.dto.req.ScoreAnomalyQueryReqDTO;
import com.smart.university.domain.dto.resp.ClassroomRecommendRespDTO;
import com.smart.university.domain.dto.resp.EnrollmentForecastRespDTO;
import com.smart.university.domain.dto.resp.GraduationRiskPageRespDTO;
import com.smart.university.domain.dto.resp.ScoreAnomalyPageRespDTO;
import com.smart.university.service.ClassroomRecommendService;
import com.smart.university.service.EnrollmentForecastService;
import com.smart.university.service.GraduationRiskService;
import com.smart.university.service.ScoreAnomalyService;
import com.smart.university.web.annotation.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教务端智能分析接口（40.1 ~ 40.4）
 */
@Tag(name = "教务端-智能分析", description = "毕业风险名单、选课热度预测、成绩分布异常与教室推荐，均为规则 + 统计推断")
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final GraduationRiskService graduationRiskService;
    private final EnrollmentForecastService enrollmentForecastService;
    private final ScoreAnomalyService scoreAnomalyService;
    private final ClassroomRecommendService classroomRecommendService;

    /**
     * 毕业风险名单
     *
     * @param requestParam 查询条件
     * @return 毕业风险名单分页结果与口径回显
     */
    @Operation(summary = "毕业风险名单", description = "completedCredit 与 36.1 / 36.2 同源同值，为跨学期累计口径")
    @GetMapping("/graduation-risks")
    public Result<GraduationRiskPageRespDTO> pageGraduationRisk(GraduationRiskQueryReqDTO requestParam) {
        return Result.success(graduationRiskService.pageGraduationRisk(requestParam));
    }

    /**
     * 选课热度预测与扩容建议
     *
     * @param requestParam 查询条件
     * @return 预测结果集合
     */
    @Operation(summary = "选课热度预测与扩容建议", description = "传入 courseId 表示显式要求预测，历史样本不足时返回 40944")
    @GetMapping("/enrollment-forecast")
    public Result<EnrollmentForecastRespDTO> listEnrollmentForecast(EnrollmentForecastQueryReqDTO requestParam) {
        return Result.success(enrollmentForecastService.listEnrollmentForecast(requestParam));
    }

    /**
     * 成绩分布异常班级
     *
     * @param requestParam 查询条件
     * @return 成绩异常班级分页结果与口径回显
     */
    @Operation(summary = "成绩分布异常班级", description = "基线为同课程历史及格率均值，无历史时取全校本学期均值")
    @GetMapping("/score-anomalies")
    public Result<ScoreAnomalyPageRespDTO> pageScoreAnomaly(ScoreAnomalyQueryReqDTO requestParam) {
        return Result.success(scoreAnomalyService.pageScoreAnomaly(requestParam));
    }

    /**
     * 教室推荐
     *
     * @param requestParam 查询条件
     * @return 教室推荐结果
     */
    @Operation(summary = "教室推荐", description = "只做推荐，不做自动排课求解")
    @GetMapping("/classroom-recommendations")
    public Result<ClassroomRecommendRespDTO> listClassroomRecommend(ClassroomRecommendQueryReqDTO requestParam) {
        return Result.success(classroomRecommendService.listClassroomRecommend(requestParam));
    }
}
