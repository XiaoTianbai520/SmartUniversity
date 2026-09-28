package com.smart.university.controller.admin;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.resp.WaitlistPromoteRespDTO;
import com.smart.university.service.WaitlistService;
import com.smart.university.web.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教务端候补递补接口
 */
@RestController
@RequestMapping("/api/v1/admin/teaching-classes")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AdminWaitlistController {

    private final WaitlistService waitlistService;

    /**
     * 手动触发指定教学班的候补递补
     *
     * @param teachingClassId 教学班 ID
     * @return 候补递补结果
     */
    @PostMapping("/{teachingClassId}/waitlist/promote")
    public Result<WaitlistPromoteRespDTO> promoteWaitlist(@PathVariable Long teachingClassId) {
        return Result.success(waitlistService.tryPromote(teachingClassId));
    }
}
