package com.smart.university.service;

import com.smart.university.domain.dto.resp.CacheStatusRespDTO;
import com.smart.university.domain.dto.resp.CacheWarmupRespDTO;
import com.smart.university.domain.dto.resp.CapacityLuaResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 选课容量服务（V1.2 第 1 / 4 道防线）
 *
 * <p>C1 红线：Redis 计数 Key 只作并发互斥器，`teaching_class.selected_count` 同样只作互斥器，
 * 任何对外展示与统计的「已选人数」一律以 DB `COUNT(course_selection WHERE status='SELECTED')` 为准。
 */
public interface SelectionCapacityService {

    /**
     * 预扣教学班名额，名额已满时抛 40910，Redis 不可用时走 DB 降级判定
     *
     * @param teachingClassId 教学班 ID
     * @param capacity        教学班容量
     * @return Lua 执行结果
     */
    CapacityLuaResult tryDeduct(Long teachingClassId, Integer capacity);

    /**
     * 回补教学班名额，退课与递补成功后调用
     *
     * @param teachingClassId 教学班 ID
     */
    void restore(Long teachingClassId);

    /**
     * 补偿回补教学班名额，事务回滚后调用
     *
     * @param teachingClassId 教学班 ID
     */
    void compensateRestore(Long teachingClassId);

    /**
     * 取教学班的 DB 权威已选人数
     *
     * @param teachingClassId 教学班 ID
     * @return 已选人数
     */
    long getAuthoritativeCount(Long teachingClassId);

    /**
     * 预热指定教学班的容量计数，以 DB 权威值为准覆盖写
     *
     * @param teachingClassId 教学班 ID
     * @return 是否与预热前的值不一致
     */
    boolean warmup(Long teachingClassId);

    /**
     * 批量预热指定批次下的教学班容量计数
     *
     * @param batchId 选课批次 ID
     * @return 预热结果
     */
    CacheWarmupRespDTO warmupByBatchId(Long batchId);

    /**
     * 查询容量缓存状态，含降级标记与已缓存 Key 数
     *
     * @param semesterId 学期 ID
     * @return 缓存状态
     */
    CacheStatusRespDTO getCacheStatus(Long semesterId);

    /**
     * 失效指定教学班的容量缓存
     *
     * @param teachingClassId 教学班 ID
     */
    void invalidate(Long teachingClassId);

    /**
     * 失效当前学期缓存
     */
    void invalidateCurrentSemester();

    /**
     * 失效指定学期的当前批次缓存
     *
     * @param semesterId 学期 ID
     */
    void invalidateCurrentBatch(Long semesterId);

    /**
     * 判断是否处于降级状态
     *
     * @return 降级返回 true
     */
    boolean isDegraded();

    /**
     * 取最近一次降级时间
     *
     * @return 降级时间，未降级返回 null
     */
    LocalDateTime getDegradedAt();

    /**
     * 取最近一次降级原因
     *
     * @return 降级原因，未降级返回空串
     */
    String getDegradeReason();

    /**
     * 统计已缓存容量 Key 的教学班数量
     *
     * @param teachingClassIds 教学班 ID 集合
     * @return 已缓存 Key 数
     */
    long countCachedKey(List<Long> teachingClassIds);

    /**
     * 清除降级标记，Redis 恢复后由 Lua 调用成功路径自动触发
     */
    void clearDegraded();
}
