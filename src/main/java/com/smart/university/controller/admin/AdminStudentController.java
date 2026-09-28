package com.smart.university.controller.admin;
import io.swagger.v3.oas.annotations.Parameter;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.req.StudentPageQueryReqDTO;
import com.smart.university.domain.dto.req.StudentSaveReqDTO;
import com.smart.university.domain.dto.resp.StudentRespDTO;
import com.smart.university.service.StudentService;
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
 * 教务端学生管理接口
 */
@Tag(name = "教务端-学生管理", description = "学生的增删改查与状态调整")
@RestController
@RequestMapping("/api/v1/admin/students")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AdminStudentController {

    private final StudentService studentService;

    /**
     * 分页查询学生
     *
     * @param requestParam 查询条件
     * @return 学生分页结果
     */
    @Operation(summary = "分页查询学生")
    @GetMapping
    public Result<PageResult<StudentRespDTO>> pageStudent(StudentPageQueryReqDTO requestParam) {
        return Result.success(studentService.pageStudent(requestParam));
    }

    /**
     * 查询学生详情
     *
     * @param studentId 学生 ID
     * @return 学生信息
     */
    @Operation(summary = "查询学生详情")
    @GetMapping("/{studentId}")
    public Result<StudentRespDTO> getStudentDetail(@Parameter(example = "1") @PathVariable Long studentId) {
        return Result.success(studentService.getStudentDetail(studentId));
    }

    /**
     * 新增学生
     *
     * @param requestParam 学生入参
     * @return 学生 ID
     */
    @Operation(summary = "新增学生")
    @PostMapping
    public Result<Long> saveStudent(@Valid @RequestBody StudentSaveReqDTO requestParam) {
        return Result.success("新增成功", studentService.saveStudent(requestParam));
    }

    /**
     * 修改学生
     *
     * @param studentId    学生 ID
     * @param requestParam 学生入参
     * @return 空响应
     */
    @Operation(summary = "修改学生")
    @PutMapping("/{studentId}")
    public Result<Void> updateStudent(@Parameter(example = "1") @PathVariable Long studentId,
                                      @Valid @RequestBody StudentSaveReqDTO requestParam) {
        requestParam.setId(studentId);
        studentService.updateStudent(requestParam);
        return Result.success("修改成功", null);
    }

    /**
     * 修改学生状态
     *
     * @param studentId    学生 ID
     * @param requestParam 状态入参
     * @return 空响应
     */
    @Operation(summary = "修改学生状态")
    @PatchMapping("/{studentId}/status")
    public Result<Void> updateStudentStatus(@Parameter(example = "1") @PathVariable Long studentId,
                                            @Valid @RequestBody StatusUpdateReqDTO requestParam) {
        studentService.updateStudentStatus(studentId, requestParam);
        return Result.success("状态修改成功", null);
    }
}
