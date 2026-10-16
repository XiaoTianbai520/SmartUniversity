package com.smart.university.controller.admin;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.common.enums.StatTypeEnum;
import com.smart.university.common.util.EnumParseUtil;
import com.smart.university.domain.dto.req.ClassroomUsageQueryReqDTO;
import com.smart.university.domain.dto.req.CourseSelectionAnalysisQueryReqDTO;
import com.smart.university.domain.dto.req.TeacherWorkloadQueryReqDTO;
import com.smart.university.domain.dto.resp.ClassroomUsagePageRespDTO;
import com.smart.university.domain.dto.resp.CourseSelectionAnalysisPageRespDTO;
import com.smart.university.domain.dto.resp.SnapshotRefreshRespDTO;
import com.smart.university.domain.dto.resp.TeacherWorkloadPageRespDTO;
import com.smart.university.service.ClassroomUsageService;
import com.smart.university.service.CourseSelectionAnalysisService;
import com.smart.university.service.StatisticsSnapshotService;
import com.smart.university.service.TeacherWorkloadService;
import com.smart.university.web.annotation.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 教务端统计接口（37.1 ~ 37.4）
 */
@Tag(name = "教务端-统计分析", description = "教室利用率、教师工作量、课程选课分析与统计快照刷新")
@RestController
@RequestMapping("/api/v1/admin/statistics")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final ClassroomUsageService classroomUsageService;
    private final TeacherWorkloadService teacherWorkloadService;
    private final CourseSelectionAnalysisService courseSelectionAnalysisService;
    private final StatisticsSnapshotService statisticsSnapshotService;

    /**
     * 教室利用率统计
     *
     * @param requestParam 查询条件
     * @return 教室利用率分页结果与口径回显
     */
    @Operation(summary = "教室利用率统计", description = "分母 = 教室数 × 每周天数 × 每天节次 × 学期周数，口径在 extra 中回显")
    @GetMapping("/classroom-usage")
    public Result<ClassroomUsagePageRespDTO> pageClassroomUsage(ClassroomUsageQueryReqDTO requestParam) {
        return Result.success(classroomUsageService.pageClassroomUsage(requestParam));
    }

    /**
     * 教师工作量统计
     *
     * @param requestParam 查询条件
     * @return 教师工作量分页结果与口径回显
     */
    @Operation(summary = "教师工作量统计", description = "1 节课 = 1 课时，不引入课程系数")
    @GetMapping("/teacher-workload")
    public Result<TeacherWorkloadPageRespDTO> pageTeacherWorkload(TeacherWorkloadQueryReqDTO requestParam) {
        return Result.success(teacherWorkloadService.pageTeacherWorkload(requestParam));
    }

    /**
     * 课程选课分析
     *
     * @param requestParam 查询条件
     * @return 课程选课分析分页结果与口径回显
     */
    @Operation(summary = "课程选课分析", description = "已选人数取 DB 权威口径 COUNT(course_selection WHERE status='SELECTED')")
    @GetMapping("/course-selection")
    public Result<CourseSelectionAnalysisPageRespDTO> pageCourseSelectionAnalysis(
            CourseSelectionAnalysisQueryReqDTO requestParam) {
        return Result.success(courseSelectionAnalysisService.pageCourseSelectionAnalysis(requestParam));
    }

    /**
     * 手动触发统计快照刷新
     *
     * @param semesterId 学期 ID，缺省当前学期
     * @param statTypes  统计类型，逗号分隔，缺省全部
     * @return 刷新结果
     */
    @Operation(summary = "手动触发统计快照刷新", description = "覆盖写且幂等，失败保留上一版快照")
    @PostMapping("/snapshots/refresh")
    public Result<SnapshotRefreshRespDTO> refreshSnapshot(
            @Parameter(example = "1") @RequestParam(required = false) Long semesterId,
            @Parameter(example = "CLASSROOM_USAGE,TEACHER_WORKLOAD") @RequestParam(required = false) String statTypes) {
        List<StatTypeEnum> statTypeList = parseStatTypes(statTypes);
        return Result.success("刷新完成", statisticsSnapshotService.refreshSnapshot(semesterId, statTypeList));
    }

    /**
     * 解析统计类型，非法值忽略
     *
     * @param statTypes 逗号分隔的统计类型
     * @return 统计类型集合
     */
    private List<StatTypeEnum> parseStatTypes(String statTypes) {
        if (statTypes == null || statTypes.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(statTypes.split(","))
                .map(each -> EnumParseUtil.parseOrNull(StatTypeEnum.class, each.trim()))
                .filter(each -> each != null)
                .toList();
    }
}
