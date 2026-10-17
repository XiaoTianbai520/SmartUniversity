package com.smart.university.controller.admin;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.ScheduleTaskLogQueryReqDTO;
import com.smart.university.domain.dto.resp.ScheduleTaskLogRespDTO;
import com.smart.university.domain.dto.resp.ScheduleTaskRespDTO;
import com.smart.university.domain.dto.resp.ScheduleTaskRunRespDTO;
import com.smart.university.service.ScheduleTaskManageService;
import com.smart.university.web.annotation.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 教务端定时任务管理接口（41.1 ~ 41.3）
 */
@Tag(name = "教务端-定时任务管理", description = "定时任务列表、执行日志与手动触发")
@RestController
@RequestMapping("/api/v1/admin/schedule-tasks")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AdminScheduleTaskController {

    private final ScheduleTaskManageService scheduleTaskManageService;

    /**
     * 查询定时任务列表
     *
     * @return 定时任务集合
     */
    @Operation(summary = "查询定时任务列表", description = "任务开关与调度表达式读取配置项，最近一次执行情况取执行日志")
    @GetMapping
    public Result<List<ScheduleTaskRespDTO>> listScheduleTask() {
        return Result.success(scheduleTaskManageService.listScheduleTask());
    }

    /**
     * 查询指定任务的执行日志
     *
     * @param taskCode     任务编码
     * @param requestParam 查询条件
     * @return 执行日志分页结果
     */
    @Operation(summary = "查询任务执行日志")
    @GetMapping("/{taskCode}/logs")
    public Result<PageResult<ScheduleTaskLogRespDTO>> pageScheduleTaskLog(
            @Parameter(example = "BATCH_STATUS_TASK") @PathVariable String taskCode,
            ScheduleTaskLogQueryReqDTO requestParam) {
        return Result.success(scheduleTaskManageService.pageScheduleTaskLog(taskCode, requestParam));
    }

    /**
     * 手动触发指定任务
     *
     * @param taskCode 任务编码
     * @return 手动触发结果
     */
    @Operation(summary = "手动触发任务", description = "同样抢分布式锁，任务执行中返回 40942，任务停用返回 40941")
    @PostMapping("/{taskCode}/run")
    public Result<ScheduleTaskRunRespDTO> runScheduleTask(
            @Parameter(example = "WAITLIST_SCAN_TASK") @PathVariable String taskCode) {
        return Result.success("触发成功", scheduleTaskManageService.runScheduleTask(taskCode));
    }
}
