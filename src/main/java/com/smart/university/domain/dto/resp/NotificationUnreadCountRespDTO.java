package com.smart.university.domain.dto.resp;

import lombok.Data;

/**
 * 通知未读数量出参
 */
@Data
public class NotificationUnreadCountRespDTO {

    /**
     * 未读通知数量
     */
    private Long unreadCount;
}
