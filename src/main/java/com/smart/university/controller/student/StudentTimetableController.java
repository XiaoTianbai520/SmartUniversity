package com.smart.university.controller.student;
import io.swagger.v3.oas.annotations.Parameter;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.resp.CreditSummaryRespDTO;
import com.smart.university.domain.dto.resp.TimetableRespDTO;
import com.smart.university.service.CreditService;
import com.smart.university.service.TimetableService;
import com.smart.university.web.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生端课表与学分接口，V1 均通过选课记录动态生成
 */
@Tag(name = "学生端-课表与学分", description = "学生课表查询与学分统计")
@RestController
@RequestMapping("/api/v1/student")
@RequireRole(RoleEnum.STUDENT)
@RequiredArgsConstructor
public class StudentTimetableController {

    private final TimetableService timetableService;

    private final CreditService creditService;

    /**
     * 查询我的课表
     *
     * @param semesterId 学期 ID，不传时使用当前学期
     * @return 课表集合
     */
    @Operation(summary = "查询我的课表", description = "按学期聚合选课记录生成课表，不传学期则取当前学期")
    @GetMapping("/timetable")
    public Result<List<TimetableRespDTO>> listTimetable(@Parameter(example = "1") @RequestParam(required = false) Long semesterId) {
        return Result.success(timetableService.listTimetable(semesterId));
    }

    /**
     * 查询学分统计
     *
     * @param semesterId 学期 ID，不传时使用当前学期
     * @return 学分统计
     */
    @Operation(summary = "查询学分统计", description = "统计已选课程学分，不传学期则取当前学期")
    @GetMapping("/credits")
    public Result<CreditSummaryRespDTO> getCreditSummary(@Parameter(example = "1") @RequestParam(required = false) Long semesterId) {
        return Result.success(creditService.getCreditSummary(semesterId));
    }
}
