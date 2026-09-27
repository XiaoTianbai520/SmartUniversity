package com.smart.university.controller.student;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.CourseSelectReqDTO;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.dto.resp.CourseSelectionRespDTO;
import com.smart.university.domain.dto.resp.StudentSelectionRespDTO;
import com.smart.university.service.CourseSelectionService;
import com.smart.university.web.annotation.RequireRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生端选课接口
 */
@RestController
@RequestMapping("/api/v1/student/selections")
@RequireRole(RoleEnum.STUDENT)
@RequiredArgsConstructor
public class StudentSelectionController {

    private final CourseSelectionService courseSelectionService;

    /**
     * 学生选课，学生 ID 与批次由后端从 Token 推导
     *
     * @param requestParam 选课入参
     * @return 选课结果
     */
    @PostMapping
    public Result<CourseSelectionRespDTO> selectCourse(@Valid @RequestBody CourseSelectReqDTO requestParam) {
        return Result.success("选课成功", courseSelectionService.selectCourse(requestParam));
    }

    /**
     * 查询我的课程
     *
     * @param requestParam 查询条件
     * @return 我的课程集合
     */
    @GetMapping
    public Result<List<StudentSelectionRespDTO>> listSelection(StudentSelectionQueryReqDTO requestParam) {
        return Result.success(courseSelectionService.listStudentSelection(requestParam));
    }

    /**
     * 学生退课
     *
     * @param selectionId 选课记录 ID
     * @return 空响应
     */
    @DeleteMapping("/{selectionId}")
    public Result<Void> withdrawCourse(@PathVariable Long selectionId) {
        courseSelectionService.withdrawCourse(selectionId);
        return Result.success("退课成功", null);
    }
}
