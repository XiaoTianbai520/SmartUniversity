package com.smart.university.domain.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知出参
 */
@Data
public class NotificationRespDTO {

    /**
     * 通知 ID
     */
    private Long notificationId;

    /**
     * 通知类型
     */
    private String noticeType;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 关联业务 ID
     */
    private Long bizId;

    /**
     * 是否已读：0 未读，1 已读
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
