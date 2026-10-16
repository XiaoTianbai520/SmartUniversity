package com.smart.university.controller.admin;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.resp.DashboardOverviewRespDTO;
import com.smart.university.service.DashboardService;
import com.smart.university.web.annotation.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教务端数据看板接口（38.1）
 */
@Tag(name = "教务端-数据看板", description = "教务首页数据看板，优先命中快照")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    /**
     * 查询教务首页数据看板
     *
     * @param semesterId 学期 ID，缺省当前学期
     * @return 看板聚合结果
     */
    @Operation(summary = "查询教务首页数据看板",
            description = "studentCount 为在册学生总数，selectedStudentCount 为本学期已选课学生数，二者口径不同，数值不等属预期")
    @GetMapping("/overview")
    public Result<DashboardOverviewRespDTO> getDashboardOverview(
            @Parameter(example = "1") @RequestParam(required = false) Long semesterId) {
        return Result.success(dashboardService.getDashboardOverview(semesterId));
    }
}
