package com.smart.university.domain.event;

import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.enums.NoticeTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 通知事件模型，业务模块通过 {@link NoticeEventPublisher} 发布，
 * 由通知模块在事务提交后监听落库
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeEvent {

    /**
     * 通知类型
     */
    private NoticeTypeEnum noticeType;

    /**
     * 通知标题，为空时使用通知类型的默认标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 接收人 sys_user.id 列表
     */
    private List<Long> receiverUserIds;

    /**
     * 接收角色快照
     */
    private RoleEnum receiverRole;

    /**
     * 关联业务 ID，用于去重与跳转
     */
    private Long bizId;

    /**
     * 获取接收人列表，为空时返回空集合，避免下游遍历出现空指针
     *
     * @return 接收人 ID 列表，不会返回 null
     */
    public List<Long> getReceiverUserIds() {
        return receiverUserIds == null ? Collections.emptyList() : receiverUserIds;
    }
}
