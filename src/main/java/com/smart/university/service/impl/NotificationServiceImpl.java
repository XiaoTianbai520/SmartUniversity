package com.smart.university.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.university.common.base.PageResult;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.dto.req.NotificationPageQueryReqDTO;
import com.smart.university.domain.dto.resp.NotificationReadAllRespDTO;
import com.smart.university.domain.dto.resp.NotificationRespDTO;
import com.smart.university.domain.dto.resp.NotificationUnreadCountRespDTO;
import com.smart.university.domain.entity.NotificationDO;
import com.smart.university.mapper.NotificationMapper;
import com.smart.university.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务实现
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public PageResult<NotificationRespDTO> pageNotification(NotificationPageQueryReqDTO requestParam) {
        getCurrentUserId();
        Page<NotificationDO> page = Page.of(requestParam.getCurrentPage(), requestParam.getLimit());
        IPage<NotificationDO> pageResult = notificationMapper.listNotificationByCondition(page, requestParam);
        List<NotificationRespDTO> records = pageResult.getRecords().stream()
                .map(this::convertToRespDTO)
                .toList();
        return new PageResult<>(records, requestParam.getCurrentPage(), requestParam.getLimit(), pageResult.getTotal());
    }

    @Override
    public NotificationUnreadCountRespDTO countUnread() {
        Long receiverUserId = getCurrentUserId();
        NotificationUnreadCountRespDTO result = new NotificationUnreadCountRespDTO();
        result.setUnreadCount(notificationMapper.countUnreadByReceiverUserId(receiverUserId));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long notificationId) {
        getCurrentUserId();
        NotificationDO notificationDO = notificationMapper.getNotificationById(notificationId);
        if (notificationDO == null) {
            throw new BizException(ResultCodeEnum.NOTIFICATION_NOT_EXIST);
        }
        if (Integer.valueOf(1).equals(notificationDO.getIsRead())) {
            return;
        }
        NotificationDO updateNotificationDO = new NotificationDO();
        updateNotificationDO.setId(notificationDO.getId());
        updateNotificationDO.setIsRead(1);
        updateNotificationDO.setReadAt(LocalDateTime.now());
        notificationMapper.updateNotification(updateNotificationDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationReadAllRespDTO markAllRead() {
        Long receiverUserId = getCurrentUserId();
        int updatedCount = notificationMapper.updateReadStatusByReceiverUserId(receiverUserId, LocalDateTime.now());
        NotificationReadAllRespDTO result = new NotificationReadAllRespDTO();
        result.setUpdatedCount(updatedCount);
        return result;
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 当前登录用户 ID
     */
    private Long getCurrentUserId() {
        Long result = UserContextHolder.getUserId();
        if (result == null) {
            throw new BizException(ResultCodeEnum.NOT_LOGIN);
        }
        return result;
    }

    /**
     * 转换通知出参
     *
     * @param notificationDO 通知数据对象
     * @return 通知出参
     */
    private NotificationRespDTO convertToRespDTO(NotificationDO notificationDO) {
        NotificationRespDTO result = new NotificationRespDTO();
        result.setNotificationId(notificationDO.getId());
        result.setNoticeType(notificationDO.getNoticeType());
        result.setTitle(notificationDO.getTitle());
        result.setContent(notificationDO.getContent());
        result.setBizId(notificationDO.getBizId());
        result.setIsRead(notificationDO.getIsRead());
        result.setCreatedAt(notificationDO.getCreatedAt());
        return result;
    }
}
