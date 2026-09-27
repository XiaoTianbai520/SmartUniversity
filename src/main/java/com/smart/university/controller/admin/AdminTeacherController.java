package com.smart.university.controller.admin;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.req.TeacherPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeacherSaveReqDTO;
import com.smart.university.domain.dto.resp.TeacherRespDTO;
import com.smart.university.service.TeacherService;
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
 * 教务端教师管理接口
 */
@RestController
@RequestMapping("/api/v1/admin/teachers")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AdminTeacherController {

    private final TeacherService teacherService;

    /**
     * 分页查询教师
     *
     * @param requestParam 查询条件
     * @return 教师分页结果
     */
    @GetMapping
    public Result<PageResult<TeacherRespDTO>> pageTeacher(TeacherPageQueryReqDTO requestParam) {
        return Result.success(teacherService.pageTeacher(requestParam));
    }

    /**
     * 查询教师详情
     *
     * @param teacherId 教师 ID
     * @return 教师信息
     */
    @GetMapping("/{teacherId}")
    public Result<TeacherRespDTO> getTeacherDetail(@PathVariable Long teacherId) {
        return Result.success(teacherService.getTeacherDetail(teacherId));
    }

    /**
     * 新增教师
     *
     * @param requestParam 教师入参
     * @return 教师 ID
     */
    @PostMapping
    public Result<Long> saveTeacher(@Valid @RequestBody TeacherSaveReqDTO requestParam) {
        return Result.success("新增成功", teacherService.saveTeacher(requestParam));
    }

    /**
     * 修改教师
     *
     * @param teacherId    教师 ID
     * @param requestParam 教师入参
     * @return 空响应
     */
    @PutMapping("/{teacherId}")
    public Result<Void> updateTeacher(@PathVariable Long teacherId,
                                      @Valid @RequestBody TeacherSaveReqDTO requestParam) {
        requestParam.setId(teacherId);
        teacherService.updateTeacher(requestParam);
        return Result.success("修改成功", null);
    }

    /**
     * 修改教师状态
     *
     * @param teacherId    教师 ID
     * @param requestParam 状态入参
     * @return 空响应
     */
    @PatchMapping("/{teacherId}/status")
    public Result<Void> updateTeacherStatus(@PathVariable Long teacherId,
                                            @Valid @RequestBody StatusUpdateReqDTO requestParam) {
        teacherService.updateTeacherStatus(teacherId, requestParam);
        return Result.success("状态修改成功", null);
    }
}
