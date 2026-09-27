package com.smart.university.controller.admin;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.MajorPageQueryReqDTO;
import com.smart.university.domain.dto.req.MajorSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.MajorRespDTO;
import com.smart.university.service.MajorService;
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
 * 教务端专业管理接口
 */
@RestController
@RequestMapping("/api/v1/admin/majors")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class MajorController {

    private final MajorService majorService;

    /**
     * 分页查询专业
     *
     * @param requestParam 查询条件
     * @return 专业分页结果
     */
    @GetMapping
    public Result<PageResult<MajorRespDTO>> pageMajor(MajorPageQueryReqDTO requestParam) {
        return Result.success(majorService.pageMajor(requestParam));
    }

    /**
     * 查询专业详情
     *
     * @param majorId 专业 ID
     * @return 专业信息
     */
    @GetMapping("/{majorId}")
    public Result<MajorRespDTO> getMajorDetail(@PathVariable Long majorId) {
        return Result.success(majorService.getMajorDetail(majorId));
    }

    /**
     * 新增专业
     *
     * @param requestParam 专业入参
     * @return 专业 ID
     */
    @PostMapping
    public Result<Long> saveMajor(@Valid @RequestBody MajorSaveReqDTO requestParam) {
        return Result.success("保存成功", majorService.saveMajor(requestParam));
    }

    /**
     * 修改专业
     *
     * @param majorId      专业 ID
     * @param requestParam 专业入参
     * @return 专业 ID
     */
    @PutMapping("/{majorId}")
    public Result<Long> updateMajor(@PathVariable Long majorId, @Valid @RequestBody MajorSaveReqDTO requestParam) {
        requestParam.setId(majorId);
        return Result.success("保存成功", majorService.saveMajor(requestParam));
    }

    /**
     * 修改专业状态
     *
     * @param majorId      专业 ID
     * @param requestParam 状态入参
     * @return 空响应
     */
    @PatchMapping("/{majorId}/status")
    public Result<Void> updateMajorStatus(@PathVariable Long majorId,
                                          @Valid @RequestBody StatusUpdateReqDTO requestParam) {
        majorService.updateMajorStatus(majorId, requestParam);
        return Result.success("状态修改成功", null);
    }
}
