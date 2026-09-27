package com.smart.university.controller.admin;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.ClassroomPageQueryReqDTO;
import com.smart.university.domain.dto.req.ClassroomSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.ClassroomRespDTO;
import com.smart.university.service.ClassroomService;
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
 * 教务端教室管理接口
 */
@RestController
@RequestMapping("/api/v1/admin/classrooms")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService classroomService;

    /**
     * 分页查询教室
     *
     * @param requestParam 查询条件
     * @return 教室分页结果
     */
    @GetMapping
    public Result<PageResult<ClassroomRespDTO>> pageClassroom(ClassroomPageQueryReqDTO requestParam) {
        return Result.success(classroomService.pageClassroom(requestParam));
    }

    /**
     * 查询教室详情
     *
     * @param classroomId 教室 ID
     * @return 教室信息
     */
    @GetMapping("/{classroomId}")
    public Result<ClassroomRespDTO> getClassroomDetail(@PathVariable Long classroomId) {
        return Result.success(classroomService.getClassroomDetail(classroomId));
    }

    /**
     * 新增教室
     *
     * @param requestParam 教室入参
     * @return 教室 ID
     */
    @PostMapping
    public Result<Long> saveClassroom(@Valid @RequestBody ClassroomSaveReqDTO requestParam) {
        return Result.success("保存成功", classroomService.saveClassroom(requestParam));
    }

    /**
     * 修改教室
     *
     * @param classroomId  教室 ID
     * @param requestParam 教室入参
     * @return 教室 ID
     */
    @PutMapping("/{classroomId}")
    public Result<Long> updateClassroom(@PathVariable Long classroomId,
                                        @Valid @RequestBody ClassroomSaveReqDTO requestParam) {
        requestParam.setId(classroomId);
        return Result.success("保存成功", classroomService.saveClassroom(requestParam));
    }

    /**
     * 修改教室状态
     *
     * @param classroomId  教室 ID
     * @param requestParam 状态入参
     * @return 空响应
     */
    @PatchMapping("/{classroomId}/status")
    public Result<Void> updateClassroomStatus(@PathVariable Long classroomId,
                                              @Valid @RequestBody StatusUpdateReqDTO requestParam) {
        classroomService.updateClassroomStatus(classroomId, requestParam);
        return Result.success("状态修改成功", null);
    }
}
