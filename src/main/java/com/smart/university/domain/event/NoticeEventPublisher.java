package com.smart.university.domain.event;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 通知事件发布器，业务模块只依赖本组件，不感知通知表结构
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 发布通知事件，发布失败仅记录日志，不影响业务主流程
     *
     * @param event 通知事件
     */
    public void publish(NoticeEvent event) {
        if (event == null || event.getNoticeType() == null || CollUtil.isEmpty(event.getReceiverUserIds())) {
            return;
        }
        if (StrUtil.isBlank(event.getTitle())) {
            event.setTitle(event.getNoticeType().getDefaultTitle());
        }
        try {
            applicationEventPublisher.publishEvent(event);
        } catch (Exception ex) {
            log.error("通知事件发布失败，通知类型：{}，业务 ID：{}", event.getNoticeType(), event.getBizId(), ex);
        }
    }
}
