package com.smart.university.controller.student;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.StudentTeachingClassPageQueryReqDTO;
import com.smart.university.domain.dto.resp.StudentTeachingClassDetailRespDTO;
import com.smart.university.domain.dto.resp.StudentTeachingClassRespDTO;
import com.smart.university.service.TeachingClassService;
import com.smart.university.web.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生端课程查询接口
 */
@RestController
@RequestMapping("/api/v1/student/teaching-classes")
@RequireRole(RoleEnum.STUDENT)
@RequiredArgsConstructor
public class StudentCourseController {

    private final TeachingClassService teachingClassService;

    /**
     * 查询可选课程，专业、年级、学期、批次均由后端推导
     *
     * @param requestParam 查询条件
     * @return 可选课程分页结果
     */
    @GetMapping
    public Result<PageResult<StudentTeachingClassRespDTO>> pageTeachingClass(
            StudentTeachingClassPageQueryReqDTO requestParam) {
        return Result.success(teachingClassService.pageStudentVisibleTeachingClass(requestParam));
    }

    /**
     * 查询课程详情，后端再次校验查看权限
     *
     * @param teachingClassId 教学班 ID
     * @return 课程详情
     */
    @GetMapping("/{teachingClassId}")
    public Result<StudentTeachingClassDetailRespDTO> getTeachingClassDetail(@PathVariable Long teachingClassId) {
        return Result.success(
                teachingClassService.getStudentTeachingClassDetail(UserContextHolder.getStudentId(), teachingClassId));
    }
}
