package com.smart.university.controller.student;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.resp.CreditProgressRespDTO;
import com.smart.university.service.CreditProgressService;
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
 * 学生端学分进度接口（36.1），跨学期累计口径（C8）
 */
@Tag(name = "学生端-学分进度", description = "学生查询本人跨学期累计学分进度")
@RestController
@RequestMapping("/api/v1/student")
@RequireRole(RoleEnum.STUDENT)
@RequiredArgsConstructor
public class StudentCreditProgressController {

    private final CreditProgressService creditProgressService;

    /**
     * 查询本人学分进度，学生身份从登录态取
     *
     * @param semesterId 统计截止学期 ID，缺省取当前学期，不参与学分累计过滤
     * @return 学分进度
     */
    @Operation(summary = "查询本人学分进度", description = "completedCredit 为跨学期累计口径，与 GET /student/credits 的本学期口径不同")
    @GetMapping("/credit-progress")
    public Result<CreditProgressRespDTO> getCreditProgress(
            @Parameter(example = "1") @RequestParam(required = false) Long semesterId) {
        return Result.success(creditProgressService.getCreditProgress(semesterId));
    }
}
