package com.smart.university.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.domain.dto.req.NotificationPageQueryReqDTO;
import com.smart.university.domain.entity.NotificationDO;

import java.time.LocalDateTime;

/**
 * 通知持久层，负责按当前登录用户隔离通知数据
 */
public interface NotificationMapper extends BaseMapper<NotificationDO> {

    /**
     * 根据 ID 查询当前用户的通知
     *
     * @param notificationId 通知 ID
     * @return 通知信息
     */
    default NotificationDO getNotificationById(Long notificationId) {
        return selectOne(Wrappers.<NotificationDO>lambdaQuery()
                .eq(NotificationDO::getId, notificationId)
                .eq(NotificationDO::getReceiverUserId, UserContextHolder.getUserId()));
    }

    /**
     * 按条件分页查询当前用户的通知
     *
     * @param page         分页参数
     * @param requestParam 查询条件
     * @return 分页结果
     */
    default IPage<NotificationDO> listNotificationByCondition(IPage<NotificationDO> page,
                                                               NotificationPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<NotificationDO> queryWrapper = buildQueryWrapper(requestParam);
        queryWrapper.orderByDesc(NotificationDO::getCreatedAt)
                .orderByDesc(NotificationDO::getId);
        return selectPage(page, queryWrapper);
    }

    /**
     * 按条件统计当前用户的通知数量
     *
     * @param requestParam 查询条件
     * @return 通知数量
     */
    default long countNotificationByCondition(NotificationPageQueryReqDTO requestParam) {
        return selectCount(buildQueryWrapper(requestParam));
    }

    /**
     * 统计指定接收人的未读通知数量
     *
     * @param receiverUserId 接收人用户 ID
     * @return 未读通知数量
     */
    default long countUnreadByReceiverUserId(Long receiverUserId) {
        return selectCount(Wrappers.<NotificationDO>lambdaQuery()
                .eq(NotificationDO::getReceiverUserId, receiverUserId)
                .eq(NotificationDO::getIsRead, 0));
    }

    /**
     * 保存通知
     *
     * @param requestParam 通知数据对象
     * @return 影响行数
     */
    default int saveNotification(NotificationDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新通知
     *
     * @param requestParam 通知数据对象
     * @return 影响行数
     */
    default int updateNotification(NotificationDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 将指定接收人的全部未读通知更新为已读
     *
     * @param receiverUserId 接收人用户 ID
     * @param readAt         读取时间
     * @return 影响行数
     */
    default int updateReadStatusByReceiverUserId(Long receiverUserId, LocalDateTime readAt) {
        LambdaUpdateWrapper<NotificationDO> updateWrapper = Wrappers.<NotificationDO>lambdaUpdate()
                .set(NotificationDO::getIsRead, 1)
                .set(NotificationDO::getReadAt, readAt)
                .eq(NotificationDO::getReceiverUserId, receiverUserId)
                .eq(NotificationDO::getIsRead, 0);
        return update(null, updateWrapper);
    }

    /**
     * 按通知类型、业务 ID 与接收人统计通知数量
     *
     * @param noticeType     通知类型
     * @param bizId          业务 ID
     * @param receiverUserId 接收人用户 ID
     * @return 通知数量
     */
    default long countNotificationByDedup(String noticeType, Long bizId, Long receiverUserId) {
        LambdaQueryWrapper<NotificationDO> queryWrapper = Wrappers.<NotificationDO>lambdaQuery()
                .eq(NotificationDO::getNoticeType, noticeType)
                .eq(NotificationDO::getReceiverUserId, receiverUserId);
        if (bizId == null) {
            queryWrapper.isNull(NotificationDO::getBizId);
        } else {
            queryWrapper.eq(NotificationDO::getBizId, bizId);
        }
        return selectCount(queryWrapper);
    }

    /**
     * 构建当前用户的通知查询条件
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<NotificationDO> buildQueryWrapper(NotificationPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<NotificationDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(NotificationDO::getReceiverUserId, UserContextHolder.getUserId());
        queryWrapper.eq(StrUtil.isNotBlank(requestParam.getNoticeType()),
                NotificationDO::getNoticeType, requestParam.getNoticeType());
        queryWrapper.eq(requestParam.getIsRead() != null,
                NotificationDO::getIsRead, requestParam.getIsRead());
        return queryWrapper;
    }
}
