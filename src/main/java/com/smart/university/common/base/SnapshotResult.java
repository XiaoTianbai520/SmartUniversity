package com.smart.university.common.base;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 快照读取结果载体，承载快照数据、是否命中快照与快照生成时间
 *
 * @param <T> 快照数据类型
 */
@Data
public class SnapshotResult<T> {

    /**
     * 快照数据
     */
    private T data;

    /**
     * 是否命中快照，false 表示本次为实时计算
     */
    private boolean fromSnapshot;

    /**
     * 快照生成时间，实时计算时为当前时间
     */
    private LocalDateTime snapshotTime;

    /**
     * 构造命中快照的结果
     *
     * @param data         快照数据
     * @param snapshotTime 快照生成时间
     * @param <T>          快照数据类型
     * @return 快照读取结果
     */
    public static <T> SnapshotResult<T> hit(T data, LocalDateTime snapshotTime) {
        SnapshotResult<T> result = new SnapshotResult<>();
        result.setData(data);
        result.setFromSnapshot(true);
        result.setSnapshotTime(snapshotTime);
        return result;
    }

    /**
     * 构造实时计算的结果
     *
     * @param data         计算结果
     * @param snapshotTime 计算时间
     * @param <T>          快照数据类型
     * @return 快照读取结果
     */
    public static <T> SnapshotResult<T> computed(T data, LocalDateTime snapshotTime) {
        SnapshotResult<T> result = new SnapshotResult<>();
        result.setData(data);
        result.setFromSnapshot(false);
        result.setSnapshotTime(snapshotTime);
        return result;
    }
}
