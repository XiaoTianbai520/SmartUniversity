package com.smart.university.controller.student;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.resp.StudentScoreRespDTO;
import com.smart.university.service.ScoreService;
import com.smart.university.web.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生端成绩查询接口，只返回已发布成绩
 */
@RestController
@RequestMapping("/api/v1/student")
@RequireRole(RoleEnum.STUDENT)
@RequiredArgsConstructor
public class StudentScoreController {

    private final ScoreService scoreService;

    /**
     * 查询我的成绩
     *
     * @param semesterId 学期 ID，不传时使用当前学期
     * @return 成绩集合
     */
    @GetMapping("/scores")
    public Result<List<StudentScoreRespDTO>> listScore(@RequestParam(required = false) Long semesterId) {
        return Result.success(scoreService.listStudentScore(semesterId));
    }
}
