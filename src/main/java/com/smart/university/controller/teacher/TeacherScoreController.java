package com.smart.university.controller.teacher;
import io.swagger.v3.oas.annotations.Parameter;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.ScoreSaveReqDTO;
import com.smart.university.domain.dto.resp.ScorePublishRespDTO;
import com.smart.university.domain.dto.resp.ScoreSaveRespDTO;
import com.smart.university.domain.dto.resp.TeacherScoreRespDTO;
import com.smart.university.service.ScoreService;
import com.smart.university.web.annotation.RequireRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 教师端成绩管理接口
 */
@Tag(name = "教师端-成绩管理", description = "教学班成绩录入、发布与查询")
@RestController
@RequestMapping("/api/v1/teacher/teaching-classes")
@RequireRole(RoleEnum.TEACHER)
@RequiredArgsConstructor
public class TeacherScoreController {

    private final ScoreService scoreService;

    /**
     * 查询教学班成绩列表
     *
     * @param teachingClassId 教学班 ID
     * @return 成绩列表
     */
    @Operation(summary = "查询教学班成绩列表")
    @GetMapping("/{teachingClassId}/scores")
    public Result<List<TeacherScoreRespDTO>> listScore(@Parameter(example = "1") @PathVariable Long teachingClassId) {
        return Result.success(scoreService.listTeacherScore(teachingClassId));
    }

    /**
     * 保存学生成绩
     *
     * @param teachingClassId 教学班 ID
     * @param selectionId     选课记录 ID
     * @param requestParam    成绩入参
     * @return 成绩保存结果
     */
    @Operation(summary = "保存学生成绩", description = "录入或修改单个学生的成绩，保存后仍为未发布状态")
    @PutMapping("/{teachingClassId}/scores/{selectionId}")
    public Result<ScoreSaveRespDTO> saveScore(@Parameter(example = "1") @PathVariable Long teachingClassId,
                                              @Parameter(example = "1") @PathVariable Long selectionId,
                                              @Valid @RequestBody ScoreSaveReqDTO requestParam) {
        return Result.success("成绩保存成功", scoreService.saveScore(teachingClassId, selectionId, requestParam));
    }

    /**
     * 发布教学班成绩
     *
     * @param teachingClassId 教学班 ID
     * @return 发布结果
     */
    @Operation(summary = "发布教学班成绩", description = "发布后学生端可见，并触发成绩发布通知")
    @PostMapping("/{teachingClassId}/scores/publish")
    public Result<ScorePublishRespDTO> publishScore(@Parameter(example = "1") @PathVariable Long teachingClassId) {
        return Result.success("成绩发布成功", scoreService.publishScore(teachingClassId));
    }
}
