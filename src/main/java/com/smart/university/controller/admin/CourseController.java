package com.smart.university.controller.admin;
import io.swagger.v3.oas.annotations.Parameter;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.CoursePageQueryReqDTO;
import com.smart.university.domain.dto.req.CourseSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.CourseRespDTO;
import com.smart.university.service.CourseService;
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
 * 教务端课程管理接口
 */
@Tag(name = "教务端-课程管理", description = "课程的增删改查与状态调整")
@RestController
@RequestMapping("/api/v1/admin/courses")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * 分页查询课程
     *
     * @param requestParam 查询条件
     * @return 课程分页结果
     */
    @Operation(summary = "分页查询课程")
    @GetMapping
    public Result<PageResult<CourseRespDTO>> pageCourse(CoursePageQueryReqDTO requestParam) {
        return Result.success(courseService.pageCourse(requestParam));
    }

    /**
     * 查询课程详情
     *
     * @param courseId 课程 ID
     * @return 课程信息
     */
    @Operation(summary = "查询课程详情")
    @GetMapping("/{courseId}")
    public Result<CourseRespDTO> getCourseDetail(@Parameter(example = "1") @PathVariable Long courseId) {
        return Result.success(courseService.getCourseDetail(courseId));
    }

    /**
     * 新增课程
     *
     * @param requestParam 课程入参
     * @return 课程 ID
     */
    @Operation(summary = "新增课程")
    @PostMapping
    public Result<Long> saveCourse(@Valid @RequestBody CourseSaveReqDTO requestParam) {
        return Result.success("保存成功", courseService.saveCourse(requestParam));
    }

    /**
     * 修改课程
     *
     * @param courseId     课程 ID
     * @param requestParam 课程入参
     * @return 课程 ID
     */
    @Operation(summary = "修改课程")
    @PutMapping("/{courseId}")
    public Result<Long> updateCourse(@Parameter(example = "1") @PathVariable Long courseId, @Valid @RequestBody CourseSaveReqDTO requestParam) {
        requestParam.setId(courseId);
        return Result.success("保存成功", courseService.saveCourse(requestParam));
    }

    /**
     * 修改课程状态
     *
     * @param courseId     课程 ID
     * @param requestParam 状态入参
     * @return 空响应
     */
    @Operation(summary = "修改课程状态")
    @PatchMapping("/{courseId}/status")
    public Result<Void> updateCourseStatus(@Parameter(example = "1") @PathVariable Long courseId,
                                           @Valid @RequestBody StatusUpdateReqDTO requestParam) {
        courseService.updateCourseStatus(courseId, requestParam);
        return Result.success("状态修改成功", null);
    }
}
