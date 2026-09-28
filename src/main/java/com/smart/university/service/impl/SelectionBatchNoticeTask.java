package com.smart.university.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.entity.SelectionBatchDO;
import com.smart.university.domain.enums.NoticeTypeEnum;
import com.smart.university.domain.enums.SelectionBatchStatusEnum;
import com.smart.university.domain.event.NoticeEvent;
import com.smart.university.domain.event.NoticeEventPublisher;
import com.smart.university.domain.event.NoticeTargetResolver;
import com.smart.university.mapper.SelectionBatchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 选课开始 / 结束通知定时任务，给时间窗已到达但未经过状态变更接口的批次补发通知，
 * 每轮重复投递由 uk_notice_dedup 唯一键兜底去重
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelectionBatchNoticeTask {

    /**
     * 扫描时间窗，单位为小时，用于避免把历史批次翻出来重复刷屏
     */
    private static final long SCAN_WINDOW_HOURS = 24;

    /**
     * 选课时间展示格式
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SelectionBatchMapper selectionBatchMapper;

    private final NoticeEventPublisher noticeEventPublisher;

    private final NoticeTargetResolver noticeTargetResolver;

    /**
     * 扫描处于开始 / 结束时间窗内的选课批次并发布通知
     */
    @Scheduled(fixedDelay = 60000)
    public void execute() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<SelectionBatchDO> batchDOList = listBatchInNoticeWindow(now);
            if (CollUtil.isEmpty(batchDOList)) {
                return;
            }
            for (SelectionBatchDO each : batchDOList) {
                publishBatchNotice(each, now);
            }
        } catch (Exception ex) {
            log.error("选课批次通知定时任务执行失败", ex);
        }
    }

    /**
     * 查询处于通知时间窗内的选课批次，已开始且结束时间距今不超过一天
     *
     * @param now 当前时间
     * @return 选课批次集合
     */
    private List<SelectionBatchDO> listBatchInNoticeWindow(LocalDateTime now) {
        return selectionBatchMapper.selectList(Wrappers.<SelectionBatchDO>lambdaQuery()
                .le(SelectionBatchDO::getStartTime, now)
                .ge(SelectionBatchDO::getEndTime, now.minusHours(SCAN_WINDOW_HOURS))
                .orderByAsc(SelectionBatchDO::getStartTime));
    }

    /**
     * 按批次当前状态发布开始 / 结束通知，同一批次同一学生只落库一次
     *
     * @param batchDO 选课批次
     * @param now     当前时间
     */
    private void publishBatchNotice(SelectionBatchDO batchDO, LocalDateTime now) {
        if (SelectionBatchStatusEnum.IN_PROGRESS == batchDO.getStatus()
                && batchDO.getStartTime() != null
                && !batchDO.getStartTime().isAfter(now)
                && batchDO.getStartTime().isAfter(now.minusHours(SCAN_WINDOW_HOURS))) {
            publishNotice(NoticeTypeEnum.SELECTION_START, batchDO);
        }
        boolean ended = SelectionBatchStatusEnum.ENDED == batchDO.getStatus()
                || (SelectionBatchStatusEnum.IN_PROGRESS == batchDO.getStatus()
                && batchDO.getEndTime() != null && batchDO.getEndTime().isBefore(now));
        if (ended) {
            publishNotice(NoticeTypeEnum.SELECTION_END, batchDO);
        }
    }

    /**
     * 发布选课批次通知，接收人按学生角色全量解析
     *
     * @param noticeTypeEnum 通知类型
     * @param batchDO        选课批次
     */
    private void publishNotice(NoticeTypeEnum noticeTypeEnum, SelectionBatchDO batchDO) {
        try {
            String actionText = NoticeTypeEnum.SELECTION_START == noticeTypeEnum ? "已经开始" : "已经结束";
            noticeEventPublisher.publish(NoticeEvent.builder()
                    .noticeType(noticeTypeEnum)
                    .title(noticeTypeEnum.getDefaultTitle())
                    .content(StrUtil.format("选课批次【{}】{}，选课时间：{}，请及时登录系统完成选课。",
                            batchDO.getBatchName(), actionText, buildBatchTimeText(batchDO)))
                    .receiverUserIds(noticeTargetResolver.listUserIdsByRole(RoleEnum.STUDENT))
                    .receiverRole(RoleEnum.STUDENT)
                    .bizId(batchDO.getId())
                    .build());
        } catch (Exception ex) {
            log.error("选课批次通知发布失败，batchId：{}，通知类型：{}", batchDO.getId(), noticeTypeEnum, ex);
        }
    }

    /**
     * 拼接批次选课时间段的中文描述
     *
     * @param batchDO 选课批次
     * @return 选课时间描述
     */
    private String buildBatchTimeText(SelectionBatchDO batchDO) {
        String startText = batchDO.getStartTime() == null ? StrUtil.EMPTY : batchDO.getStartTime().format(TIME_FORMATTER);
        String endText = batchDO.getEndTime() == null ? StrUtil.EMPTY : batchDO.getEndTime().format(TIME_FORMATTER);
        return StrUtil.format("{} 至 {}", startText, endText);
    }
}
