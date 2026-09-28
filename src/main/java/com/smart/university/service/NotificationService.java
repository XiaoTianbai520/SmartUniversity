package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.dto.req.NotificationPageQueryReqDTO;
import com.smart.university.domain.dto.resp.NotificationReadAllRespDTO;
import com.smart.university.domain.dto.resp.NotificationRespDTO;
import com.smart.university.domain.dto.resp.NotificationUnreadCountRespDTO;

/**
 * 通知服务
 */
public interface NotificationService {

    /**
     * 分页查询当前用户的通知
     *
     * @param requestParam 查询条件
     * @return 通知分页结果
     */
    PageResult<NotificationRespDTO> pageNotification(NotificationPageQueryReqDTO requestParam);

    /**
     * 统计当前用户的未读通知数量
     *
     * @return 未读通知数量
     */
    NotificationUnreadCountRespDTO countUnread();

    /**
     * 将当前用户的指定通知标记为已读
     *
     * @param notificationId 通知 ID
     */
    void markRead(Long notificationId);

    /**
     * 将当前用户的全部未读通知标记为已读
     *
     * @return 更新结果
     */
    NotificationReadAllRespDTO markAllRead();
}
