package com.smart.university.controller.teacher;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.ScoreAnomalyQueryReqDTO;
import com.smart.university.domain.dto.resp.ScoreAnomalyRespDTO;
import com.smart.university.service.ScoreAnomalyService;
import com.smart.university.web.annotation.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端成绩异常预警接口（39.2）
 */
@Tag(name = "教师端-成绩异常预警", description = "教师查询本人任教教学班的成绩异常预警")
@RestController
@RequestMapping("/api/v1/teacher")
@RequireRole(RoleEnum.TEACHER)
@RequiredArgsConstructor
public class TeacherScoreWarningController {

    private final ScoreAnomalyService scoreAnomalyService;

    /**
     * 查询本人任教教学班的成绩异常预警
     *
     * @param requestParam 查询条件
     * @return 成绩异常预警分页结果
     */
    @Operation(summary = "查询本人成绩异常预警", description = "只返回本人任教教学班的预警，身份从登录态取")
    @GetMapping("/score-warnings")
    public Result<PageResult<ScoreAnomalyRespDTO>> pageMyScoreAnomaly(ScoreAnomalyQueryReqDTO requestParam) {
        return Result.success(scoreAnomalyService.pageMyScoreAnomaly(requestParam));
    }
}
