package com.smart.university.controller.admin;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.SemesterPageQueryReqDTO;
import com.smart.university.domain.dto.req.SemesterSaveReqDTO;
import com.smart.university.domain.dto.req.SemesterStatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.SemesterRespDTO;
import com.smart.university.service.SemesterService;
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
 * 教务端学期管理接口
 */
@RestController
@RequestMapping("/api/v1/admin/semesters")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterService semesterService;

    /**
     * 分页查询学期
     *
     * @param requestParam 查询条件
     * @return 学期分页结果
     */
    @GetMapping
    public Result<PageResult<SemesterRespDTO>> pageSemester(SemesterPageQueryReqDTO requestParam) {
        return Result.success(semesterService.pageSemester(requestParam));
    }

    /**
     * 查询学期详情
     *
     * @param semesterId 学期 ID
     * @return 学期信息
     */
    @GetMapping("/{semesterId}")
    public Result<SemesterRespDTO> getSemesterDetail(@PathVariable Long semesterId) {
        return Result.success(semesterService.getSemesterDetail(semesterId));
    }

    /**
     * 新增学期
     *
     * @param requestParam 学期入参
     * @return 学期 ID
     */
    @PostMapping
    public Result<Long> saveSemester(@Valid @RequestBody SemesterSaveReqDTO requestParam) {
        return Result.success("保存成功", semesterService.saveSemester(requestParam));
    }

    /**
     * 修改学期
     *
     * @param semesterId   学期 ID
     * @param requestParam 学期入参
     * @return 学期 ID
     */
    @PutMapping("/{semesterId}")
    public Result<Long> updateSemester(@PathVariable Long semesterId,
                                       @Valid @RequestBody SemesterSaveReqDTO requestParam) {
        requestParam.setId(semesterId);
        return Result.success("保存成功", semesterService.saveSemester(requestParam));
    }

    /**
     * 修改学期状态
     *
     * @param semesterId   学期 ID
     * @param requestParam 状态入参
     * @return 空响应
     */
    @PatchMapping("/{semesterId}/status")
    public Result<Void> updateSemesterStatus(@PathVariable Long semesterId,
                                             @Valid @RequestBody SemesterStatusUpdateReqDTO requestParam) {
        semesterService.updateSemesterStatus(semesterId, requestParam);
        return Result.success("状态修改成功", null);
    }
}
