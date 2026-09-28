package com.smart.university.controller.teacher;
import io.swagger.v3.oas.annotations.Parameter;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.resp.ScoreStatisticsRespDTO;
import com.smart.university.service.ScoreStatisticsService;
import com.smart.university.web.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端成绩统计接口
 */
@Tag(name = "教师端-成绩统计", description = "教学班成绩统计查询")
@RestController
@RequestMapping("/api/v1/teacher/teaching-classes")
@RequireRole(RoleEnum.TEACHER)
@RequiredArgsConstructor
public class TeacherScoreStatisticsController {

    private final ScoreStatisticsService scoreStatisticsService;

    /**
     * 查询教学班成绩统计
     *
     * @param teachingClassId 教学班 ID
     * @return 教学班成绩统计
     */
    @Operation(summary = "查询教学班成绩统计", description = "按教学班聚合最高分、平均分、及格率等统计指标")
    @GetMapping("/{teachingClassId}/score-statistics")
    public Result<ScoreStatisticsRespDTO> getScoreStatistics(@Parameter(example = "1") @PathVariable Long teachingClassId) {
        return Result.success(scoreStatisticsService.getScoreStatistics(teachingClassId));
    }
}
