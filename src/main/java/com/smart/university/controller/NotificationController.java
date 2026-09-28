package com.smart.university.controller;
import io.swagger.v3.oas.annotations.Parameter;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.NotificationPageQueryReqDTO;
import com.smart.university.domain.dto.resp.NotificationReadAllRespDTO;
import com.smart.university.domain.dto.resp.NotificationRespDTO;
import com.smart.university.domain.dto.resp.NotificationUnreadCountRespDTO;
import com.smart.university.service.NotificationService;
import com.smart.university.web.annotation.RequireRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统通知接口
 */
@Tag(name = "系统通知", description = "当前用户通知的分页查询、未读统计与已读标记")
@RestController
@RequestMapping("/api/v1/notifications")
@RequireRole({RoleEnum.STUDENT, RoleEnum.TEACHER, RoleEnum.ADMIN})
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 分页查询当前用户的通知
     *
     * @param requestParam 查询条件
     * @return 通知分页结果
     */
    @Operation(summary = "分页查询我的通知")
    @GetMapping
    public Result<PageResult<NotificationRespDTO>> pageNotification(
            @Valid NotificationPageQueryReqDTO requestParam) {
        return Result.success(notificationService.pageNotification(requestParam));
    }

    /**
     * 查询当前用户的未读通知数量
     *
     * @return 未读通知数量
     */
    @Operation(summary = "查询未读通知数量")
    @GetMapping("/unread-count")
    public Result<NotificationUnreadCountRespDTO> countUnread() {
        return Result.success(notificationService.countUnread());
    }

    /**
     * 将当前用户的指定通知标记为已读
     *
     * @param notificationId 通知 ID
     * @return 空响应
     */
    @Operation(summary = "标记单条通知已读")
    @PutMapping("/{notificationId}/read")
    public Result<Void> markRead(@Parameter(example = "1") @PathVariable Long notificationId) {
        notificationService.markRead(notificationId);
        return Result.success();
    }

    /**
     * 将当前用户的全部未读通知标记为已读
     *
     * @return 更新结果
     */
    @Operation(summary = "标记全部通知已读")
    @PutMapping("/read-all")
    public Result<NotificationReadAllRespDTO> markAllRead() {
        return Result.success(notificationService.markAllRead());
    }
}
