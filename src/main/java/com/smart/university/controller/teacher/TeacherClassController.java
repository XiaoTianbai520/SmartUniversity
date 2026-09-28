package com.smart.university.controller.teacher;
import io.swagger.v3.oas.annotations.Parameter;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.TeacherStudentPageQueryReqDTO;
import com.smart.university.domain.dto.resp.TeacherClassStudentRespDTO;
import com.smart.university.domain.dto.resp.TeacherTeachingClassRespDTO;
import com.smart.university.domain.dto.resp.TeachingClassRespDTO;
import com.smart.university.service.TeachingClassService;
import com.smart.university.web.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 教师端教学班接口，教师身份一律从 Token 获取
 */
@Tag(name = "教师端-教学班", description = "教师查询本人教学班、详情与学生名单")
@RestController
@RequestMapping("/api/v1/teacher/teaching-classes")
@RequireRole(RoleEnum.TEACHER)
@RequiredArgsConstructor
public class TeacherClassController {

    private final TeachingClassService teachingClassService;

    /**
     * 查询我的教学班
     *
     * @param semesterId 学期 ID，不传时使用当前学期
     * @return 教学班集合
     */
    @Operation(summary = "查询我的教学班", description = "返回当前教师名下的教学班，不传学期则取当前学期")
    @GetMapping
    public Result<List<TeacherTeachingClassRespDTO>> listTeachingClass(
            @Parameter(example = "1") @RequestParam(required = false) Long semesterId) {
        return Result.success(teachingClassService.listTeacherTeachingClass(semesterId));
    }

    /**
     * 查询教学班详情，后端校验教学班归属
     *
     * @param teachingClassId 教学班 ID
     * @return 教学班信息
     */
    @Operation(summary = "查询教学班详情", description = "后端校验教学班归属后再返回")
    @GetMapping("/{teachingClassId}")
    public Result<TeachingClassRespDTO> getTeachingClassDetail(@Parameter(example = "1") @PathVariable Long teachingClassId) {
        return Result.success(teachingClassService.getTeachingClassDetail(teachingClassId));
    }

    /**
     * 查询教学班学生名单
     *
     * @param teachingClassId 教学班 ID
     * @param requestParam    查询条件
     * @return 学生名单分页结果
     */
    @Operation(summary = "查询教学班学生名单")
    @GetMapping("/{teachingClassId}/students")
    public Result<PageResult<TeacherClassStudentRespDTO>> pageClassStudent(
            @Parameter(example = "1") @PathVariable Long teachingClassId, TeacherStudentPageQueryReqDTO requestParam) {
        return Result.success(teachingClassService.pageTeacherClassStudent(teachingClassId, requestParam));
    }
}
