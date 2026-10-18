package com.smart.university.service;

import com.smart.university.domain.dto.resp.CacheReconcileRespDTO;

/**
 * 缓存一致性对账服务（V1.2 W2）
 *
 * <p>三方对账：DB COUNT(course_selection WHERE status='SELECTED') /
 * Redis 计数 Key（teaching-class:selected-count:{id}） / teaching_class.selected_count；
 * 不一致时以 DB 为准回写（Redis 侧走 {@code selectionCapacityService.warmup} 幂等覆盖，
 * DB 列侧走 {@code TeachingClassMapper#resetSelectedCount}）。
 *
 * <p>C1 红线：本服务**只校正互斥器**，不读取计数用于统计；权威读数恒为 DB COUNT。
 */
public interface CacheReconcileService {

    /**
     * 对指定学期的教学班做缓存一致性对账
     *
     * @param semesterId 学期 ID，由调用方传入（缺省当前学期已由 Controller / 定时任务解析）
     * @param limit      单次扫描上限，为空或 <=0 时取默认 500
     * @return 对账结果，含扫描数、不一致数、已修复数、失败数与逐项明细
     */
    CacheReconcileRespDTO reconcile(Long semesterId, Integer limit);
}
