package com.smart.university.controller.admin;

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
import org.springframework.web.bind.annotation.RestController;

/**
 * 教务端课程管理接口
 */
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
    @GetMapping("/{courseId}")
    public Result<CourseRespDTO> getCourseDetail(@PathVariable Long courseId) {
        return Result.success(courseService.getCourseDetail(courseId));
    }

    /**
     * 新增课程
     *
     * @param requestParam 课程入参
     * @return 课程 ID
     */
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
    @PutMapping("/{courseId}")
    public Result<Long> updateCourse(@PathVariable Long courseId, @Valid @RequestBody CourseSaveReqDTO requestParam) {
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
    @PatchMapping("/{courseId}/status")
    public Result<Void> updateCourseStatus(@PathVariable Long courseId,
                                           @Valid @RequestBody StatusUpdateReqDTO requestParam) {
        courseService.updateCourseStatus(courseId, requestParam);
        return Result.success("状态修改成功", null);
    }
}
