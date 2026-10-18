package com.smart.university.controller.admin;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.resp.DataAuditIssueRespDTO;
import com.smart.university.domain.entity.DataAuditIssueDO;
import com.smart.university.mapper.DataAuditIssueMapper;
import com.smart.university.service.SemesterService;
import com.smart.university.web.annotation.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 教务端异常数据巡检结果查询接口（42.2）
 *
 * <p>只读查询，直接复用 {@link DataAuditIssueMapper} 的学期维度查询方法，不改动任何业务数据。
 * 巡检结果由定时任务 {@code DataAuditTask} 落库，本接口仅做展示。
 */
@Tag(name = "教务端-数据巡检", description = "异常数据巡检结果查询，只读，不修改业务数据")
@RestController
@RequestMapping("/api/v1/admin")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AdminDataAuditController {

    private final DataAuditIssueMapper dataAuditIssueMapper;

    private final SemesterService semesterService;

    /**
     * 查询指定学期的异常数据巡检结果
     *
     * @param semesterId 学期 ID，缺省取当前学期
     * @return 巡检结果列表（容量 / 排课冲突 / 成绩未发布 / 未编排排课等）
     */
    @Operation(summary = "异常数据巡检结果", description = "按学期返回容量 / 排课冲突 / 成绩未发布等异常项")
    @GetMapping("/data-audit/issues")
    public Result<List<DataAuditIssueRespDTO>> listDataAuditIssues(
            @Parameter(example = "1") @RequestParam(required = false) Long semesterId) {
        Long targetSemesterId = semesterId == null
                ? semesterService.getCurrentSemester().getId()
                : semesterId;
        List<DataAuditIssueDO> issueDOList = dataAuditIssueMapper.listDataAuditIssueBySemesterId(targetSemesterId);
        List<DataAuditIssueRespDTO> result = issueDOList.stream().map(each -> {
            DataAuditIssueRespDTO dto = new DataAuditIssueRespDTO();
            dto.setIssueId(each.getId());
            dto.setRuleCode(each.getRuleCode());
            dto.setRuleName(each.getRuleName());
            dto.setBizType(each.getBizType());
            dto.setBizId(each.getBizId());
            dto.setSeverity(each.getSeverity() == null ? null : each.getSeverity().name());
            dto.setDescription(each.getDescription());
            dto.setSemesterId(each.getSemesterId());
            dto.setStatDate(each.getStatDate());
            return dto;
        }).collect(Collectors.toList());
        return Result.success(result);
    }
}
