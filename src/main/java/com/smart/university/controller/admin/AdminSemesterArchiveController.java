package com.smart.university.controller.admin;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.resp.SemesterArchiveRespDTO;
import com.smart.university.service.impl.SemesterArchiveTask;
import com.smart.university.web.annotation.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 教务端学期数据归档接口（42.3，V2.0 / A-09）
 *
 * <p>复用 {@link SemesterArchiveTask#archiveSemester(Long, String)}，按「选课 / 成绩 / 看板总览」
 * 三类覆盖写归档，幂等；仅归档 {@code status='FINISHED'} 的学期，未结束学期归档时由任务内部抛出
 * {@code SEMESTER_ARCHIVE_NOT_ALLOWED}（归入 failCount 并记入日志，不阻断其余类型）。
 * 单个类型归档失败时不影响整体响应，结果通过 successCount / failCount 回显。
 */
@Slf4j
@Tag(name = "教务端-学期归档", description = "手动归档已结束学期的选课 / 成绩 / 看板数据")
@RestController
@RequestMapping("/api/v1/admin")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AdminSemesterArchiveController {

    /**
     * 归档类型：选课
     */
    private static final String ARCHIVE_SELECTION = "SELECTION";

    /**
     * 归档类型：成绩
     */
    private static final String ARCHIVE_SCORE = "SCORE";

    /**
     * 归档类型：看板总览
     */
    private static final String ARCHIVE_OVERVIEW = "OVERVIEW";

    private final SemesterArchiveTask semesterArchiveTask;

    /**
     * 手动归档指定学期；archiveTypes 缺省归档全部三类，指定时仅归档所给类型
     *
     * @param semesterId   学期 ID
     * @param archiveTypes 归档类型，逗号分隔 SELECTION / SCORE / OVERVIEW，可选
     * @return 归档结果（学期 ID / 实际归档类型 / 成功数 / 失败数 / 归档时间）
     */
    @Operation(summary = "手动归档学期数据", description = "仅归档已结束学期；未结束学期归档将返回 SEMESTER_ARCHIVE_NOT_ALLOWED；archiveTypes 缺省归档全部三类")
    @PostMapping("/semesters/{semesterId}/archive")
    public Result<SemesterArchiveRespDTO> archiveSemester(
            @Parameter(example = "1") @PathVariable Long semesterId,
            @Parameter(example = "SELECTION,SCORE,OVERVIEW") @RequestParam(required = false) String archiveTypes) {
        List<String> types = resolveArchiveTypes(archiveTypes);
        int successCount = 0;
        int failCount = 0;
        for (String type : types) {
            try {
                semesterArchiveTask.archiveSemester(semesterId, type);
                successCount++;
            } catch (Exception ex) {
                failCount++;
                log.error("学期归档失败，semesterId={}，archiveType={}", semesterId, type, ex);
            }
        }
        SemesterArchiveRespDTO result = new SemesterArchiveRespDTO();
        result.setSemesterId(semesterId);
        result.setArchiveTypes(types);
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setArchivedAt(LocalDateTime.now());
        return Result.success("归档完成", result);
    }

    /**
     * 解析归档类型参数，缺省返回全部三类
     *
     * @param archiveTypes 逗号分隔的类型串
     * @return 归档类型列表
     */
    private List<String> resolveArchiveTypes(String archiveTypes) {
        if (archiveTypes == null || archiveTypes.isBlank()) {
            return List.of(ARCHIVE_SELECTION, ARCHIVE_SCORE, ARCHIVE_OVERVIEW);
        }
        return Arrays.stream(archiveTypes.split(","))
                .map(String::trim)
                .filter(each -> !each.isEmpty())
                .collect(Collectors.toList());
    }
}
