package com.smart.university.controller.admin;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.ScheduleSaveReqDTO;
import com.smart.university.domain.dto.req.TeachingClassPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeachingClassSaveReqDTO;
import com.smart.university.domain.dto.req.TeachingClassStatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.ScheduleRespDTO;
import com.smart.university.domain.dto.resp.TeachingClassRespDTO;
import com.smart.university.service.ScheduleService;
import com.smart.university.service.TeachingClassService;
import com.smart.university.web.annotation.RequireRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 教务端教学班与排课管理接口
 */
@RestController
@RequestMapping("/api/v1/admin/teaching-classes")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class TeachingClassController {

    private final TeachingClassService teachingClassService;

    private final ScheduleService scheduleService;

    /**
     * 分页查询教学班
     *
     * @param requestParam 查询条件
     * @return 教学班分页结果
     */
    @GetMapping
    public Result<PageResult<TeachingClassRespDTO>> pageTeachingClass(TeachingClassPageQueryReqDTO requestParam) {
        return Result.success(teachingClassService.pageTeachingClass(requestParam));
    }

    /**
     * 查询教学班详情
     *
     * @param teachingClassId 教学班 ID
     * @return 教学班信息
     */
    @GetMapping("/{teachingClassId}")
    public Result<TeachingClassRespDTO> getTeachingClassDetail(@PathVariable Long teachingClassId) {
        return Result.success(teachingClassService.getTeachingClassDetail(teachingClassId));
    }

    /**
     * 创建教学班
     *
     * @param requestParam 教学班入参
     * @return 教学班 ID
     */
    @PostMapping
    public Result<Long> saveTeachingClass(@Valid @RequestBody TeachingClassSaveReqDTO requestParam) {
        return Result.success("保存成功", teachingClassService.saveTeachingClass(requestParam));
    }

    /**
     * 修改教学班
     *
     * @param teachingClassId 教学班 ID
     * @param requestParam    教学班入参
     * @return 教学班 ID
     */
    @PutMapping("/{teachingClassId}")
    public Result<Long> updateTeachingClass(@PathVariable Long teachingClassId,
                                            @Valid @RequestBody TeachingClassSaveReqDTO requestParam) {
        requestParam.setId(teachingClassId);
        return Result.success("保存成功", teachingClassService.saveTeachingClass(requestParam));
    }

    /**
     * 修改教学班状态
     *
     * @param teachingClassId 教学班 ID
     * @param requestParam    状态入参
     * @return 空响应
     */
    @PatchMapping("/{teachingClassId}/status")
    public Result<Void> updateTeachingClassStatus(@PathVariable Long teachingClassId,
                                                  @Valid @RequestBody TeachingClassStatusUpdateReqDTO requestParam) {
        teachingClassService.updateTeachingClassStatus(teachingClassId, requestParam);
        return Result.success("状态修改成功", null);
    }

    /**
     * 查询教学班排课
     *
     * @param teachingClassId 教学班 ID
     * @return 排课集合
     */
    @GetMapping("/{teachingClassId}/schedules")
    public Result<List<ScheduleRespDTO>> listSchedule(@PathVariable Long teachingClassId) {
        return Result.success(scheduleService.listSchedule(teachingClassId));
    }

    /**
     * 新增排课
     *
     * @param teachingClassId 教学班 ID
     * @param requestParam    排课入参
     * @return 排课 ID
     */
    @PostMapping("/{teachingClassId}/schedules")
    public Result<Long> saveSchedule(@PathVariable Long teachingClassId,
                                     @Valid @RequestBody ScheduleSaveReqDTO requestParam) {
        return Result.success("保存成功", scheduleService.saveSchedule(teachingClassId, requestParam));
    }

    /**
     * 修改排课
     *
     * @param teachingClassId 教学班 ID
     * @param requestParam    排课入参
     * @return 空响应
     */
    @PutMapping("/{teachingClassId}/schedules")
    public Result<Void> updateSchedule(@PathVariable Long teachingClassId,
                                       @Valid @RequestBody ScheduleSaveReqDTO requestParam) {
        scheduleService.updateSchedule(teachingClassId, requestParam);
        return Result.success("保存成功", null);
    }

    /**
     * 删除排课
     *
     * @param teachingClassId 教学班 ID
     * @param scheduleId      排课 ID
     * @return 空响应
     */
    @DeleteMapping("/{teachingClassId}/schedules/{scheduleId}")
    public Result<Void> removeSchedule(@PathVariable Long teachingClassId, @PathVariable Long scheduleId) {
        scheduleService.removeSchedule(teachingClassId, scheduleId);
        return Result.success("删除成功", null);
    }
}
