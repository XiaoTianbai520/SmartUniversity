package com.smart.university.controller.admin;

import com.smart.university.common.base.PageResult;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 教务端异常数据巡检结果查询接口（42.2，V2.0 / A-08）
 *
 * <p>只读查询，直接复用 {@link DataAuditIssueMapper} 的学期维度查询方法，不改动任何业务数据；
 * 巡检结果由定时任务 {@code DataAuditTask} 落库。支持按 ruleCode / severity / bizType 在内存二次过滤与分页，
 * 对齐接口清单 §42.2 的 {@code records / page / pageSize / total} 结构。
 * 响应中的 {@code extra} 汇总计数（highCount 等）作为 T9 联调阶段增强项，本实现复用既有
 * {@link DataAuditIssueRespDTO} 返回核心字段。
 */
@Slf4j
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
     * @param ruleCode   规则编码，可选 CAPACITY_ZERO / SCHEDULE_CONFLICT / SCORE_UNPUBLISHED / NO_SCHEDULE
     * @param severity   严重级别，可选 LOW / MEDIUM / HIGH
     * @param bizType    业务类型，可选 TEACHING_CLASS / CLASSROOM / TEACHER / SCORE / SEMESTER
     * @param page       页码，从 1 开始，默认 1
     * @param pageSize   每页条数，默认 20
     * @return 巡检结果分页列表
     */
    @Operation(summary = "异常数据巡检结果", description = "按学期返回容量 / 排课冲突 / 成绩未发布等异常项，支持规则 / 级别 / 业务类型过滤与分页")
    @GetMapping("/data-audit/issues")
    public Result<PageResult<DataAuditIssueRespDTO>> listDataAuditIssues(
            @Parameter(example = "1") @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) String ruleCode,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String bizType,
            @Parameter(example = "1") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(example = "20") @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        Long targetSemesterId = semesterId == null
                ? semesterService.getCurrentSemester().getId()
                : semesterId;
        List<DataAuditIssueRespDTO> dtoList = dataAuditIssueMapper
                .listDataAuditIssueBySemesterId(targetSemesterId).stream()
                .filter(each -> ruleCode == null || ruleCode.equals(each.getRuleCode()))
                .filter(each -> severity == null || (each.getSeverity() != null
                        && severity.equalsIgnoreCase(each.getSeverity().name())))
                .filter(each -> bizType == null || bizType.equals(each.getBizType()))
                .map(AdminDataAuditController::convert)
                .collect(Collectors.toList());

        int currentPage = page == null || page < 1 ? 1 : page;
        int currentPageSize = pageSize == null || pageSize < 1 ? 20 : pageSize;
        long total = dtoList.size();
        int from = (int) ((long) (currentPage - 1) * currentPageSize);
        List<DataAuditIssueRespDTO> pageRecords = from >= total
                ? List.of()
                : dtoList.subList(from, (int) Math.min(from + currentPageSize, total));
        return Result.success(new PageResult<>(pageRecords, currentPage, currentPageSize, total));
    }

    /**
     * 将巡检结果数据对象转换为出参
     *
     * @param each 巡检结果数据对象
     * @return 出参
     */
    private static DataAuditIssueRespDTO convert(DataAuditIssueDO each) {
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
    }
}
