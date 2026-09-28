package com.smart.university.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.smart.university.domain.entity.NotificationDO;
import com.smart.university.domain.event.NoticeEvent;
import com.smart.university.mapper.NotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 通知事件监听器，在业务事务提交后幂等落库
 */
@Slf4j
@Component
public class NotificationEventListener {

    private final NotificationMapper notificationMapper;

    private final TransactionTemplate transactionTemplate;

    public NotificationEventListener(NotificationMapper notificationMapper,
                                     PlatformTransactionManager transactionManager) {
        this.notificationMapper = notificationMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 处理通知事件，每个接收人使用独立新事务，避免单条失败影响整批
     *
     * @param event 通知事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void saveNotificationByEvent(NoticeEvent event) {
        if (event == null || event.getNoticeType() == null || CollUtil.isEmpty(event.getReceiverUserIds())) {
            return;
        }
        try {
            saveNotificationBatch(event);
        } catch (Exception ex) {
            log.error("通知事件处理失败，通知类型：{}，业务 ID：{}",
                    event.getNoticeType(), event.getBizId(), ex);
        }
    }

    private void saveNotificationBatch(NoticeEvent event) {
        String title = StrUtil.isBlank(event.getTitle())
                ? event.getNoticeType().getDefaultTitle() : event.getTitle();
        Set<Long> receiverUserIdSet = new LinkedHashSet<>(event.getReceiverUserIds());
        int savedCount = 0;
        int skippedCount = event.getReceiverUserIds().size() - receiverUserIdSet.size();
        log.info("收到通知事件，通知类型：{}，业务 ID：{}，接收人数：{}",
                event.getNoticeType(), event.getBizId(), event.getReceiverUserIds().size());
        for (Long each : receiverUserIdSet) {
            if (each == null) {
                skippedCount++;
                continue;
            }
            try {
                if (saveNotificationInNewTransaction(event, title, each)) {
                    savedCount++;
                } else {
                    skippedCount++;
                }
            } catch (DataIntegrityViolationException ignored) {
                skippedCount++;
            } catch (Exception ex) {
                skippedCount++;
                log.error("单条通知落库失败，通知类型：{}，业务 ID：{}，接收人用户 ID：{}",
                        event.getNoticeType(), event.getBizId(), each, ex);
            }
        }
        log.info("通知事件处理完成，通知类型：{}，业务 ID：{}，实际落库条数：{}，跳过条数：{}",
                event.getNoticeType(), event.getBizId(), savedCount, skippedCount);
    }

    private boolean saveNotificationInNewTransaction(NoticeEvent event, String title, Long receiverUserId) {
        Boolean result = transactionTemplate.execute(transactionStatus -> {
            String noticeType = event.getNoticeType().getCode();
            // 业务 ID 为空的通知（如课程调整）语义为每次变更都要触达接收人，直接跳过查重
            if (event.getBizId() != null
                    && notificationMapper.countNotificationByDedup(noticeType, event.getBizId(), receiverUserId) > 0) {
                return false;
            }
            NotificationDO notificationDO = new NotificationDO();
            notificationDO.setNoticeType(noticeType);
            notificationDO.setTitle(title);
            notificationDO.setContent(event.getContent());
            notificationDO.setReceiverUserId(receiverUserId);
            notificationDO.setReceiverRole(event.getReceiverRole() == null ? null : event.getReceiverRole().getCode());
            notificationDO.setBizId(event.getBizId());
            notificationDO.setIsRead(0);
            notificationMapper.saveNotification(notificationDO);
            return true;
        });
        return Boolean.TRUE.equals(result);
    }
}
