package com.smart.university.controller.admin;
import io.swagger.v3.oas.annotations.Parameter;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.GradePageQueryReqDTO;
import com.smart.university.domain.dto.req.GradeSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.GradeRespDTO;
import com.smart.university.service.GradeService;
import com.smart.university.web.annotation.RequireRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教务端年级管理接口
 */
@Tag(name = "教务端-年级管理", description = "年级的增删改查与状态调整")
@RestController
@RequestMapping("/api/v1/admin/grades")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    /**
     * 分页查询年级
     *
     * @param requestParam 查询条件
     * @return 年级分页结果
     */
    @Operation(summary = "分页查询年级")
    @GetMapping
    public Result<PageResult<GradeRespDTO>> pageGrade(GradePageQueryReqDTO requestParam) {
        return Result.success(gradeService.pageGrade(requestParam));
    }

    /**
     * 查询年级详情
     *
     * @param gradeId 年级 ID
     * @return 年级信息
     */
    @Operation(summary = "查询年级详情")
    @GetMapping("/{gradeId}")
    public Result<GradeRespDTO> getGradeDetail(@Parameter(example = "1") @PathVariable Long gradeId) {
        return Result.success(gradeService.getGradeDetail(gradeId));
    }

    /**
     * 新增年级
     *
     * @param requestParam 年级入参
     * @return 年级 ID
     */
    @Operation(summary = "新增年级")
    @PostMapping
    public Result<Long> saveGrade(@Valid @RequestBody GradeSaveReqDTO requestParam) {
        return Result.success("保存成功", gradeService.saveGrade(requestParam));
    }

    /**
     * 修改年级
     *
     * @param gradeId      年级 ID
     * @param requestParam 年级入参
     * @return 年级 ID
     */
    @Operation(summary = "修改年级")
    @PutMapping("/{gradeId}")
    public Result<Long> updateGrade(@Parameter(example = "1") @PathVariable Long gradeId, @Valid @RequestBody GradeSaveReqDTO requestParam) {
        requestParam.setId(gradeId);
        return Result.success("保存成功", gradeService.saveGrade(requestParam));
    }

    /**
     * 修改年级状态
     *
     * @param gradeId      年级 ID
     * @param requestParam 状态入参
     * @return 空响应
     */
    @Operation(summary = "修改年级状态")
    @PatchMapping("/{gradeId}/status")
    public Result<Void> updateGradeStatus(@Parameter(example = "1") @PathVariable Long gradeId,
                                          @Valid @RequestBody StatusUpdateReqDTO requestParam) {
        gradeService.updateGradeStatus(gradeId, requestParam);
        return Result.success("状态修改成功", null);
    }
}
