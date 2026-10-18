package com.smart.university.controller.admin;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 教务端学期数据归档接口（42.3）
 *
 * <p>复用 {@link SemesterArchiveTask#archiveSemester(Long, String)}，按「选课 / 成绩 / 看板总览」
 * 三类覆盖写归档，幂等；仅归档 {@code status='FINISHED'} 的学期，未结束学期归档时由任务内部抛出
 * {@code SEMESTER_ARCHIVE_NOT_ALLOWED}。
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
     * 手动归档指定学期，缺省归档全部三类数据；指定 archiveType 时仅归档该类型
     *
     * @param semesterId  学期 ID
     * @param archiveType 归档类型，可选 SELECTION / SCORE / OVERVIEW
     * @return 归档结果摘要
     */
    @Operation(summary = "手动归档学期数据", description = "仅归档已结束学期；未结束学期归档将返回 SEMESTER_ARCHIVE_NOT_ALLOWED")
    @PostMapping("/semesters/{semesterId}/archive")
    public Result<Map<String, Object>> archiveSemester(
            @Parameter(example = "1") @PathVariable Long semesterId,
            @Parameter(example = "SELECTION") @RequestParam(required = false) String archiveType) {
        List<String> archivedTypes = new ArrayList<>();
        if (archiveType == null || archiveType.isBlank()) {
            for (String type : List.of(ARCHIVE_SELECTION, ARCHIVE_SCORE, ARCHIVE_OVERVIEW)) {
                semesterArchiveTask.archiveSemester(semesterId, type);
                archivedTypes.add(type);
            }
        } else {
            semesterArchiveTask.archiveSemester(semesterId, archiveType);
            archivedTypes.add(archiveType);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("semesterId", semesterId);
        result.put("archivedTypes", archivedTypes);
        return Result.success("归档完成", result);
    }
}
