package com.smart.university.controller.admin;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.AcademicClassPageQueryReqDTO;
import com.smart.university.domain.dto.req.AcademicClassSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.AcademicClassRespDTO;
import com.smart.university.service.AcademicClassService;
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
 * 教务端行政班管理接口
 */
@RestController
@RequestMapping("/api/v1/admin/academic-classes")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AcademicClassController {

    private final AcademicClassService academicClassService;

    /**
     * 分页查询行政班
     *
     * @param requestParam 查询条件
     * @return 行政班分页结果
     */
    @GetMapping
    public Result<PageResult<AcademicClassRespDTO>> pageAcademicClass(AcademicClassPageQueryReqDTO requestParam) {
        return Result.success(academicClassService.pageAcademicClass(requestParam));
    }

    /**
     * 查询行政班详情
     *
     * @param classId 班级 ID
     * @return 行政班信息
     */
    @GetMapping("/{classId}")
    public Result<AcademicClassRespDTO> getAcademicClassDetail(@PathVariable Long classId) {
        return Result.success(academicClassService.getAcademicClassDetail(classId));
    }

    /**
     * 新增行政班
     *
     * @param requestParam 行政班入参
     * @return 班级 ID
     */
    @PostMapping
    public Result<Long> saveAcademicClass(@Valid @RequestBody AcademicClassSaveReqDTO requestParam) {
        return Result.success("保存成功", academicClassService.saveAcademicClass(requestParam));
    }

    /**
     * 修改行政班
     *
     * @param classId      班级 ID
     * @param requestParam 行政班入参
     * @return 班级 ID
     */
    @PutMapping("/{classId}")
    public Result<Long> updateAcademicClass(@PathVariable Long classId,
                                            @Valid @RequestBody AcademicClassSaveReqDTO requestParam) {
        requestParam.setId(classId);
        return Result.success("保存成功", academicClassService.saveAcademicClass(requestParam));
    }

    /**
     * 修改行政班状态
     *
     * @param classId      班级 ID
     * @param requestParam 状态入参
     * @return 空响应
     */
    @PatchMapping("/{classId}/status")
    public Result<Void> updateAcademicClassStatus(@PathVariable Long classId,
                                                  @Valid @RequestBody StatusUpdateReqDTO requestParam) {
        academicClassService.updateAcademicClassStatus(classId, requestParam);
        return Result.success("状态修改成功", null);
    }
}
