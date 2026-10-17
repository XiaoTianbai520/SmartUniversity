package com.smart.university.service.impl;

import com.smart.university.common.context.TaskContext;
import com.smart.university.common.enums.TaskCodeEnum;
import com.smart.university.common.enums.TriggerTypeEnum;
import com.smart.university.common.util.ScheduleTaskLogUtil;
import com.smart.university.domain.dto.req.SelectionBatchStatusUpdateReqDTO;
import com.smart.university.domain.entity.SelectionBatchDO;
import com.smart.university.domain.enums.SelectionBatchStatusEnum;
import com.smart.university.mapper.SelectionBatchMapper;
import com.smart.university.service.SelectionBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 选课批次状态自动流转定时任务（A-01）
 *
 * <p>状态机沿用 V1.1 既有语义：`NOT_STARTED → IN_PROGRESS → ENDED` 单向推进，
 * 状态变更与选课开始 / 结束通知统一复用 {@link SelectionBatchService#updateSelectionBatchStatus}，
 * 不在本任务内另造状态流转逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelectionBatchStatusTask {

    private final SelectionBatchMapper selectionBatchMapper;

    private final SelectionBatchService selectionBatchService;

    private final ScheduleTaskLogUtil scheduleTaskLogUtil;

    /**
     * 任务开关
     */
    @Value("${smart-university.schedule.tasks.batch-status.enabled:true}")
    private boolean enabled;

    /**
     * 扫描到达开始 / 结束时间的批次并推进状态，开关关闭时直接跳过
     */
    @Scheduled(fixedDelayString = "${smart-university.schedule.tasks.batch-status.fixed-delay:60000}")
    public void execute() {
        if (!enabled) {
            return;
        }
        scheduleTaskLogUtil.runWithLog(TaskCodeEnum.BATCH_STATUS_TASK, TriggerTypeEnum.SCHEDULED, this::doExecute);
    }

    /**
     * 执行批次状态流转，单批次失败跳过并记录异常摘要
     *
     * @return 任务执行上下文
     */
    /**
     * 手工触发入口，供教务端手动执行任务复用
     *
     * @return 任务执行上下文
     */
    public TaskContext runManually() {
        return doExecute();
    }

    public TaskContext doExecute() {
        TaskContext context = TaskContext.empty();
        LocalDateTime now = LocalDateTime.now();
        List<SelectionBatchDO> toStartList = selectionBatchMapper.listBatchToStart(now);
        List<SelectionBatchDO> toEndList = selectionBatchMapper.listBatchToEnd(now);
        context.addScanCount(toStartList.size() + toEndList.size());
        for (SelectionBatchDO each : toStartList) {
            try {
                updateStatus(each, SelectionBatchStatusEnum.IN_PROGRESS);
                context.addSuccessCount(1);
            } catch (Exception ex) {
                context.addFailCount(1);
                context.appendError("批次开启失败，batchId：" + each.getId() + "，" + ex.getMessage());
                log.error("批次自动开启失败，batchId：{}", each.getId(), ex);
            }
        }
        for (SelectionBatchDO each : toEndList) {
            try {
                updateStatus(each, SelectionBatchStatusEnum.ENDED);
                context.addSuccessCount(1);
            } catch (Exception ex) {
                context.addFailCount(1);
                context.appendError("批次结束失败，batchId：" + each.getId() + "，" + ex.getMessage());
                log.error("批次自动结束失败，batchId：{}", each.getId(), ex);
            }
        }
        return context;
    }

    /**
     * 推进单个批次状态
     *
     * @param batchDO    选课批次
     * @param targetStatus 目标状态
     */
    private void updateStatus(SelectionBatchDO batchDO, SelectionBatchStatusEnum targetStatus) {
        SelectionBatchStatusUpdateReqDTO requestParam = new SelectionBatchStatusUpdateReqDTO();
        requestParam.setStatus(targetStatus.getCode());
        selectionBatchService.updateSelectionBatchStatus(batchDO.getId(), requestParam);
        log.info("选课批次状态自动流转完成，batchId：{}，目标状态：{}", batchDO.getId(), targetStatus.getCode());
    }
}
