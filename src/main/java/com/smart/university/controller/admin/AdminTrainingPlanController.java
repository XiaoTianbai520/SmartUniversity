package com.smart.university.controller.admin;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.req.TrainingPlanPageQueryReqDTO;
import com.smart.university.domain.dto.req.TrainingPlanSaveReqDTO;
import com.smart.university.domain.dto.resp.TrainingPlanRespDTO;
import com.smart.university.service.TrainingPlanService;
import com.smart.university.web.annotation.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教务端培养方案管理接口（35.1 ~ 35.5）
 */
@Tag(name = "教务端-培养方案管理", description = "培养方案的增删改查与启停")
@RestController
@RequestMapping("/api/v1/admin/training-plans")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AdminTrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    /**
     * 分页查询培养方案
     *
     * @param requestParam 查询条件
     * @return 培养方案分页结果
     */
    @Operation(summary = "分页查询培养方案")
    @GetMapping
    public Result<PageResult<TrainingPlanRespDTO>> pageTrainingPlan(TrainingPlanPageQueryReqDTO requestParam) {
        return Result.success(trainingPlanService.pageTrainingPlan(requestParam));
    }

    /**
     * 查询培养方案详情
     *
     * @param planId 培养方案 ID
     * @return 培养方案信息
     */
    @Operation(summary = "查询培养方案详情")
    @GetMapping("/{planId}")
    public Result<TrainingPlanRespDTO> getTrainingPlanDetail(@Parameter(example = "1") @PathVariable Long planId) {
        return Result.success(trainingPlanService.getTrainingPlanDetail(planId));
    }

    /**
     * 新增培养方案
     *
     * @param requestParam 培养方案入参
     * @return 培养方案 ID
     */
    @Operation(summary = "新增培养方案")
    @PostMapping
    public Result<Long> saveTrainingPlan(@Valid @RequestBody TrainingPlanSaveReqDTO requestParam) {
        return Result.success("保存成功", trainingPlanService.saveTrainingPlan(requestParam));
    }

    /**
     * 修改培养方案
     *
     * @param planId       培养方案 ID
     * @param requestParam 培养方案入参
     * @return 培养方案 ID
     */
    @Operation(summary = "修改培养方案")
    @PutMapping("/{planId}")
    public Result<Long> updateTrainingPlan(@Parameter(example = "1") @PathVariable Long planId,
                                           @Valid @RequestBody TrainingPlanSaveReqDTO requestParam) {
        requestParam.setId(planId);
        return Result.success("保存成功", trainingPlanService.saveTrainingPlan(requestParam));
    }

    /**
     * 修改培养方案启停状态
     *
     * @param planId       培养方案 ID
     * @param requestParam 状态入参
     * @return 空响应
     */
    @Operation(summary = "修改培养方案状态")
    @PatchMapping("/{planId}/status")
    public Result<Void> updateTrainingPlanStatus(@Parameter(example = "1") @PathVariable Long planId,
                                                 @Valid @RequestBody StatusUpdateReqDTO requestParam) {
        trainingPlanService.updateTrainingPlanStatus(planId, requestParam);
        return Result.success("状态修改成功", null);
    }
}
