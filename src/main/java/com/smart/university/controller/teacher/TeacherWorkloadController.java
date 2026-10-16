package com.smart.university.controller.teacher;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.resp.TeacherWorkloadDetailRespDTO;
import com.smart.university.service.TeacherWorkloadService;
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
 * 教师端工作量接口（39.1）
 */
@Tag(name = "教师端-工作量", description = "教师查询本人工作量，身份从登录态取")
@RestController
@RequestMapping("/api/v1/teacher")
@RequireRole(RoleEnum.TEACHER)
@RequiredArgsConstructor
public class TeacherWorkloadController {

    private final TeacherWorkloadService teacherWorkloadService;

    /**
     * 查询本人工作量
     *
     * @param semesterId 学期 ID，缺省当前学期
     * @return 本人工作量
     */
    @Operation(summary = "查询本人工作量", description = "教师身份从登录态取，接口不接受 teacherId 入参")
    @GetMapping("/workload")
    public Result<TeacherWorkloadDetailRespDTO> getMyWorkload(
            @Parameter(example = "1") @RequestParam(required = false) Long semesterId) {
        return Result.success(teacherWorkloadService.getMyWorkload(semesterId));
    }
}
