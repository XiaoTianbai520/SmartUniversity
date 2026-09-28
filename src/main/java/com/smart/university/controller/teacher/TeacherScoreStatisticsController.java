package com.smart.university.controller.teacher;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.resp.ScoreStatisticsRespDTO;
import com.smart.university.service.ScoreStatisticsService;
import com.smart.university.web.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端成绩统计接口
 */
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
    @GetMapping("/{teachingClassId}/score-statistics")
    public Result<ScoreStatisticsRespDTO> getScoreStatistics(@PathVariable Long teachingClassId) {
        return Result.success(scoreStatisticsService.getScoreStatistics(teachingClassId));
    }
}
