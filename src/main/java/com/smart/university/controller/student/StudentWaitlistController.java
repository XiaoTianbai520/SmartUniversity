package com.smart.university.controller.student;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.WaitlistPageQueryReqDTO;
import com.smart.university.domain.dto.resp.WaitlistRespDTO;
import com.smart.university.service.WaitlistService;
import com.smart.university.web.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生端候补选课接口
 */
@RestController
@RequestMapping("/api/v1/student")
@RequireRole(RoleEnum.STUDENT)
@RequiredArgsConstructor
public class StudentWaitlistController {

    private final WaitlistService waitlistService;

    /**
     * 加入候补队列
     *
     * @param teachingClassId 教学班 ID
     * @return 候补位次信息
     */
    @PostMapping("/teaching-classes/{teachingClassId}/waitlist")
    public Result<WaitlistRespDTO> joinWaitlist(@PathVariable Long teachingClassId) {
        return Result.success("加入候补成功", waitlistService.joinWaitlist(teachingClassId));
    }

    /**
     * 取消候补
     *
     * @param selectionId 候补选课记录 ID
     * @return 空响应
     */
    @DeleteMapping("/waitlist/{selectionId}")
    public Result<Void> cancelWaitlist(@PathVariable Long selectionId) {
        waitlistService.cancelWaitlist(selectionId);
        return Result.success("取消候补成功", null);
    }

    /**
     * 分页查询我的候补列表
     *
     * @param requestParam 分页查询条件
     * @return 候补列表分页结果
     */
    @GetMapping("/waitlist")
    public Result<PageResult<WaitlistRespDTO>> pageWaitlist(WaitlistPageQueryReqDTO requestParam) {
        return Result.success(waitlistService.pageWaitlist(requestParam));
    }
}
