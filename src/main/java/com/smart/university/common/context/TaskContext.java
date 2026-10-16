package com.smart.university.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 定时任务执行上下文，由业务代码累加扫描 / 成功 / 失败条数与异常摘要，
 * 任务结束后统一写入 schedule_task_log
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 异常摘要最大长度，与 schedule_task_log.error_summary 的 VARCHAR(1000) 对齐
     */
    public static final int MAX_ERROR_SUMMARY_LENGTH = 1000;

    /**
     * 扫描条数
     */
    private Integer scanCount;

    /**
     * 成功条数
     */
    private Integer successCount;

    /**
     * 失败条数
     */
    private Integer failCount;

    /**
     * 异常摘要
     */
    private String errorSummary;

    /**
     * 构建空上下文，供无统计口径的任务使用
     *
     * @return 计数全部为 0 的任务上下文
     */
    public static TaskContext empty() {
        TaskContext result = new TaskContext();
        result.setScanCount(0);
        result.setSuccessCount(0);
        result.setFailCount(0);
        result.setErrorSummary(null);
        return result;
    }

    /**
     * 累加扫描条数
     *
     * @param delta 增量
     */
    public void addScanCount(int delta) {
        this.scanCount = nullSafe(this.scanCount) + delta;
    }

    /**
     * 累加成功条数
     *
     * @param delta 增量
     */
    public void addSuccessCount(int delta) {
        this.successCount = nullSafe(this.successCount) + delta;
    }

    /**
     * 累加失败条数
     *
     * @param delta 增量
     */
    public void addFailCount(int delta) {
        this.failCount = nullSafe(this.failCount) + delta;
    }

    /**
     * 追加异常摘要，超过最大长度时截断，避免超出数据库列宽
     *
     * @param error 异常摘要
     */
    public void appendError(String error) {
        if (error == null || error.isEmpty()) {
            return;
        }
        String merged = this.errorSummary == null || this.errorSummary.isEmpty()
                ? error
                : this.errorSummary + " | " + error;
        this.errorSummary = merged.length() > MAX_ERROR_SUMMARY_LENGTH
                ? merged.substring(0, MAX_ERROR_SUMMARY_LENGTH)
                : merged;
    }

    /**
     * 获取扫描条数，为空时返回 0
     *
     * @return 扫描条数
     */
    public int getScanCountOrDefault() {
        return nullSafe(scanCount);
    }

    /**
     * 获取成功条数，为空时返回 0
     *
     * @return 成功条数
     */
    public int getSuccessCountOrDefault() {
        return nullSafe(successCount);
    }

    /**
     * 获取失败条数，为空时返回 0
     *
     * @return 失败条数
     */
    public int getFailCountOrDefault() {
        return nullSafe(failCount);
    }

    /**
     * 空值兜底为 0
     *
     * @param value 计数值
     * @return 非空计数
     */
    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}
