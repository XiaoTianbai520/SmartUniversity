package com.smart.university.controller.student;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.resp.GraduationRiskRespDTO;
import com.smart.university.service.GraduationRiskService;
import com.smart.university.web.annotation.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生端毕业风险预测接口（36.2）
 */
@Tag(name = "学生端-毕业风险", description = "学生查询本人毕业风险预测，学分口径与 36.1 同源")
@RestController
@RequestMapping("/api/v1/student")
@RequireRole(RoleEnum.STUDENT)
@RequiredArgsConstructor
public class StudentGraduationRiskController {

    private final GraduationRiskService graduationRiskService;

    /**
     * 查询本人毕业风险预测
     *
     * @param semesterId 统计截止学期 ID，缺省取当前学期
     * @return 毕业风险预测结果
     */
    @Operation(summary = "查询本人毕业风险预测", description = "completedCredit 与 36.1 同源同值，为跨学期累计口径")
    @GetMapping("/graduation-risk")
    public Result<GraduationRiskRespDTO> getMyGraduationRisk(
            @Parameter(example = "1") @RequestParam(required = false) Long semesterId) {
        return Result.success(graduationRiskService.getMyGraduationRisk(semesterId));
    }
}
