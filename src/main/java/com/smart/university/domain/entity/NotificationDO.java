package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * notification 表
 */
@Data
@TableName("notification")
public class NotificationDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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
     * 接收人 sys_user.id
     */
    private Long receiverUserId;

    /**
     * 接收角色快照
     */
    private String receiverRole;

    /**
     * 关联业务 ID
     */
    private Long bizId;

    /**
     * 是否已读：0 未读，1 已读
     */
    private Integer isRead;

    /**
     * 读取时间
     */
    private LocalDateTime readAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
