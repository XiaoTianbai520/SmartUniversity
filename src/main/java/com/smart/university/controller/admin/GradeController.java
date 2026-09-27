package com.smart.university.controller.admin;

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
import org.springframework.web.bind.annotation.RestController;

/**
 * 教务端年级管理接口
 */
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
    @GetMapping("/{gradeId}")
    public Result<GradeRespDTO> getGradeDetail(@PathVariable Long gradeId) {
        return Result.success(gradeService.getGradeDetail(gradeId));
    }

    /**
     * 新增年级
     *
     * @param requestParam 年级入参
     * @return 年级 ID
     */
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
    @PutMapping("/{gradeId}")
    public Result<Long> updateGrade(@PathVariable Long gradeId, @Valid @RequestBody GradeSaveReqDTO requestParam) {
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
    @PatchMapping("/{gradeId}/status")
    public Result<Void> updateGradeStatus(@PathVariable Long gradeId,
                                          @Valid @RequestBody StatusUpdateReqDTO requestParam) {
        gradeService.updateGradeStatus(gradeId, requestParam);
        return Result.success("状态修改成功", null);
    }
}
