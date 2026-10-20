package com.smart.university.controller.student;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.resp.SelectionBatchRespDTO;
import com.smart.university.service.SelectionBatchService;
import com.smart.university.web.annotation.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生端选课批次接口
 */
@Tag(name = "学生端-选课批次", description = "学生查询当前选课批次概要")
@RestController
@RequestMapping("/api/v1/student")
@RequireRole(RoleEnum.STUDENT)
@RequiredArgsConstructor
public class StudentBatchController {

    private final SelectionBatchService selectionBatchService;

    /**
     * 查询当前选课批次，供学生工作台顶部横幅展示
     * <p>返回当前学期处于选课时间内、状态为 IN_PROGRESS 的批次；无活跃批次时返回 null。
     *
     * @return 选课批次出参，无活跃批次时为 null
     */
    @Operation(summary = "查询当前选课批次", description = "返回当前学期处于选课时间内的进行中批次；无活跃批次时返回 null")
    @GetMapping("/batch")
    public Result<SelectionBatchRespDTO> getCurrentBatch() {
        return Result.success(selectionBatchService.getCurrentBatch());
    }
}
