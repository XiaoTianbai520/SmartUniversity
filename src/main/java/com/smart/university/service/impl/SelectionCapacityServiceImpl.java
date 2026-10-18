package com.smart.university.service.impl;

import com.smart.university.common.constant.RedisCommonConstant;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.common.util.RedisKeyUtil;
import com.smart.university.domain.dto.resp.CacheStatusRespDTO;
import com.smart.university.domain.dto.resp.CacheWarmupRespDTO;
import com.smart.university.domain.dto.resp.CapacityLuaResult;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.SelectionBatchClassMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.service.SelectionCapacityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 选课容量服务实现（V1.2 第 1 / 4 道防线）
 *
 * <p>预扣走 Lua 原子脚本；Redis 不可用时写降级标记并回落 DB 判定（不抛 5xx）；
 * 计数漂移由脚本用 DB 权威值校正；`teaching_class.selected_count` 仅作互斥器，
 * 每次预扣前按 DB 权威值自愈对齐，保证第 2 道防线的条件更新始终有效。
 *
 * <p>C1 红线：本服务的计数 Key 与 selected_count 都只作并发互斥器，
 * 权威读数一律走 {@link #getAuthoritativeCount}（DB COUNT）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelectionCapacityServiceImpl implements SelectionCapacityService {

    /**
     * 降级原因默认文案
     */
    private static final String DEFAULT_DEGRADE_REASON = "Redis 不可用，已回落 DB 条件更新";

    private final StringRedisTemplate stringRedisTemplate;

    private final RedisScript<List<Long>> capacityDeductScript;

    private final RedisScript<List<Long>> capacityRestoreScript;

    private final CourseSelectionMapper courseSelectionMapper;

    private final TeachingClassMapper teachingClassMapper;

    private final SelectionBatchClassMapper selectionBatchClassMapper;

    /**
     * 容量计数 Key 过期时间（秒）
     */
    @Value("${smart-university.selection.capacity-key-ttl-seconds:86400}")
    private long capacityKeyTtlSeconds;

    /**
     * 降级标记过期时间（秒）
     */
    @Value("${smart-university.selection.degraded-flag-ttl-seconds:60}")
    private long degradedFlagTtlSeconds;

    /**
     * 单次扫描上限
     */
    @Value("${smart-university.snapshot.scan-limit:500}")
    private int scanLimit;

    /**
     * 最近一次降级原因，进程内保留，不额外占用 Redis Key
     */
    private volatile String degradeReason = "";

    /**
     * 最近一次降级时间
     */
    private volatile LocalDateTime degradedAt;

    @Override
    public CapacityLuaResult tryDeduct(Long teachingClassId, Integer capacity) {
        long dbCount = getAuthoritativeCount(teachingClassId);
        int safeCapacity = capacity == null ? 0 : capacity;
        alignSelectedCount(teachingClassId, dbCount);
        try {
            List<Long> scriptResult = stringRedisTemplate.execute(capacityDeductScript,
                    List.of(RedisKeyUtil.buildTeachingClassSelectedCountKey(teachingClassId)),
                    String.valueOf(safeCapacity), String.valueOf(dbCount),
                    String.valueOf(capacityKeyTtlSeconds));
            CapacityLuaResult result = CapacityLuaResult.parse(scriptResult);
            clearDegraded();
            if (result.isFull()) {
                throw new BizException(ResultCodeEnum.CLASS_CAPACITY_FULL);
            }
            if (result.isDrift()) {
                // 计数已被脚本校正为 DB 权威值，本次按 DB 判定是否还有名额
                if (dbCount >= safeCapacity) {
                    throw new BizException(ResultCodeEnum.CLASS_CAPACITY_FULL);
                }
                return result;
            }
            return result;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            return degradeDeduct(teachingClassId, safeCapacity, dbCount, ex);
        }
    }

    @Override
    public void restore(Long teachingClassId) {
        executeRestore(teachingClassId);
    }

    @Override
    public void compensateRestore(Long teachingClassId) {
        executeRestore(teachingClassId);
    }

    @Override
    public long getAuthoritativeCount(Long teachingClassId) {
        return courseSelectionMapper.countSelectedByTeachingClassId(teachingClassId);
    }

    @Override
    public boolean warmup(Long teachingClassId) {
        long dbCount = getAuthoritativeCount(teachingClassId);
        String key = RedisKeyUtil.buildTeachingClassSelectedCountKey(teachingClassId);
        String existValue = stringRedisTemplate.opsForValue().get(key);
        boolean mismatch = existValue != null && parseLong(existValue) != dbCount;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(dbCount),
                Duration.ofSeconds(capacityKeyTtlSeconds));
        alignSelectedCount(teachingClassId, dbCount);
        return mismatch;
    }

    @Override
    public CacheWarmupRespDTO warmupByBatchId(Long batchId) {
        long startTimeMillis = System.currentTimeMillis();
        List<Long> teachingClassIds = selectionBatchClassMapper.listTeachingClassIdByBatchId(batchId)
                .stream().limit(scanLimit).toList();
        int warmupCount = 0;
        int failCount = 0;
        int mismatchCount = 0;
        for (Long each : teachingClassIds) {
            try {
                if (warmup(each)) {
                    mismatchCount++;
                }
                warmupCount++;
            } catch (Exception ex) {
                failCount++;
                log.error("容量缓存预热失败，teachingClassId：{}，异常摘要：{}", each, ex.getMessage());
            }
        }
        if (warmupCount == 0 && failCount > 0) {
            throw new BizException(ResultCodeEnum.CACHE_WARMUP_FAILED);
        }
        CacheWarmupRespDTO result = new CacheWarmupRespDTO();
        result.setBatchId(batchId);
        result.setTeachingClassCount(teachingClassIds.size());
        result.setWarmupCount(warmupCount);
        result.setFailCount(failCount);
        result.setMismatchCount(mismatchCount);
        result.setDurationMs(System.currentTimeMillis() - startTimeMillis);
        return result;
    }

    @Override
    public CacheStatusRespDTO getCacheStatus(Long semesterId) {
        List<TeachingClassDO> teachingClassDOList = teachingClassMapper.listTeachingClassBySemesterId(semesterId);
        List<Long> teachingClassIds = teachingClassDOList.stream().map(TeachingClassDO::getId).toList();
        CacheStatusRespDTO result = new CacheStatusRespDTO();
        result.setSemesterId(semesterId);
        result.setDegraded(isDegraded());
        result.setDegradedAt(getDegradedAt());
        result.setDegradeReason(getDegradeReason());
        result.setTeachingClassCount(teachingClassDOList.size());
        result.setCachedKeyCount((int) countCachedKey(teachingClassIds));
        // 最近一次对账时间由 W2 对账任务负责回填，此处保持为空
        result.setLastReconcileTime(null);
        result.setSelectedCountSample(teachingClassIds.isEmpty()
                ? 0
                : (int) getAuthoritativeCount(teachingClassIds.get(0)));
        return result;
    }

    @Override
    public void invalidate(Long teachingClassId) {
        try {
            stringRedisTemplate.delete(RedisKeyUtil.buildTeachingClassSelectedCountKey(teachingClassId));
        } catch (Exception ex) {
            log.warn("容量缓存失效失败，teachingClassId={}", teachingClassId, ex);
        }
    }

    @Override
    public void invalidateCurrentSemester() {
        try {
            stringRedisTemplate.delete(RedisCommonConstant.CURRENT_SEMESTER_KEY);
        } catch (Exception ex) {
            log.warn("当前学期缓存失效失败", ex);
        }
    }

    @Override
    public void invalidateCurrentBatch(Long semesterId) {
        if (semesterId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(RedisKeyUtil.buildCurrentSelectionBatchKey(semesterId));
        } catch (Exception ex) {
            log.warn("当前批次缓存失效失败，semesterId={}", semesterId, ex);
        }
    }

    @Override
    public boolean isDegraded() {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisCommonConstant.SELECTION_DEGRADED_KEY));
        } catch (Exception ex) {
            return true;
        }
    }

    @Override
    public LocalDateTime getDegradedAt() {
        try {
            String value = stringRedisTemplate.opsForValue().get(RedisCommonConstant.SELECTION_DEGRADED_KEY);
            if (value == null) {
                return degradedAt;
            }
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(parseLong(value)), ZoneId.systemDefault());
        } catch (Exception ex) {
            return degradedAt;
        }
    }

    @Override
    public String getDegradeReason() {
        return degradeReason == null ? "" : degradeReason;
    }

    @Override
    public long countCachedKey(List<Long> teachingClassIds) {
        if (teachingClassIds == null || teachingClassIds.isEmpty()) {
            return 0L;
        }
        List<String> keyList = new ArrayList<>();
        for (Long each : teachingClassIds) {
            keyList.add(RedisKeyUtil.buildTeachingClassSelectedCountKey(each));
        }
        try {
            List<String> valueList = stringRedisTemplate.opsForValue().multiGet(keyList);
            if (valueList == null) {
                return 0L;
            }
            return valueList.stream().filter(each -> each != null).count();
        } catch (Exception ex) {
            return 0L;
        }
    }

    @Override
    public void clearDegraded() {
        try {
            stringRedisTemplate.delete(RedisCommonConstant.SELECTION_DEGRADED_KEY);
        } catch (Exception ex) {
            log.debug("清除降级标记失败", ex);
        }
    }

    /**
     * 执行回补：Redis 计数回补 + 教学班互斥器回补，任一失败只记录日志不影响业务结果
     *
     * @param teachingClassId 教学班 ID
     */
    private void executeRestore(Long teachingClassId) {
        long dbCount = getAuthoritativeCount(teachingClassId);
        try {
            stringRedisTemplate.execute(capacityRestoreScript,
                    List.of(RedisKeyUtil.buildTeachingClassSelectedCountKey(teachingClassId)),
                    String.valueOf(dbCount), String.valueOf(capacityKeyTtlSeconds));
        } catch (Exception ex) {
            log.warn("容量回补失败，teachingClassId={}，异常摘要：{}", teachingClassId, ex.getMessage());
        }
        try {
            teachingClassMapper.restoreSelectedCount(teachingClassId);
        } catch (Exception ex) {
            log.warn("教学班已选人数回补失败，teachingClassId={}，异常摘要：{}", teachingClassId, ex.getMessage());
        }
    }

    /**
     * 降级判定：写降级标记后回落 DB 判定，只产生业务结果不抛 5xx
     *
     * @param teachingClassId 教学班 ID
     * @param capacity        教学班容量
     * @param dbCount         DB 权威已选人数
     * @param ex              Redis 异常
     * @return 降级后的预扣结果
     */
    private CapacityLuaResult degradeDeduct(Long teachingClassId, int capacity, long dbCount, Exception ex) {
        log.warn("选课容量缓存降级，teachingClassId={}，异常摘要：{}", teachingClassId, ex.getMessage());
        markDegraded(ex);
        if (dbCount >= capacity) {
            throw new BizException(ResultCodeEnum.CLASS_CAPACITY_FULL);
        }
        CapacityLuaResult result = new CapacityLuaResult();
        result.setCode(CapacityLuaResult.CODE_SUCCESS);
        result.setCount(dbCount);
        result.setCapacity(capacity);
        return result;
    }

    /**
     * 写降级标记，值为降级发生的时间戳
     *
     * @param ex Redis 异常
     */
    private void markDegraded(Exception ex) {
        degradeReason = DEFAULT_DEGRADE_REASON + "：" + ex.getClass().getSimpleName();
        degradedAt = LocalDateTime.now();
        try {
            stringRedisTemplate.opsForValue().set(RedisCommonConstant.SELECTION_DEGRADED_KEY,
                    String.valueOf(System.currentTimeMillis()),
                    Duration.ofSeconds(degradedFlagTtlSeconds));
        } catch (Exception innerEx) {
            log.warn("降级标记写入失败，异常摘要：{}", innerEx.getMessage());
        }
    }

    /**
     * 冷启动与漂移自愈：把互斥器 selected_count 对齐到 DB 权威值
     *
     * @param teachingClassId 教学班 ID
     * @param dbCount         DB 权威已选人数
     */
    private void alignSelectedCount(Long teachingClassId, long dbCount) {
        try {
            TeachingClassDO teachingClassDO = teachingClassMapper.getTeachingClassById(teachingClassId);
            if (teachingClassDO == null) {
                return;
            }
            Integer selectedCount = teachingClassDO.getSelectedCount();
            if (selectedCount == null || selectedCount.longValue() != dbCount) {
                teachingClassMapper.resetSelectedCount(teachingClassId, (int) dbCount);
            }
        } catch (Exception ex) {
            log.warn("教学班已选人数对齐失败，teachingClassId={}，异常摘要：{}", teachingClassId, ex.getMessage());
        }
    }

    /**
     * 解析整数，解析失败返回 0
     *
     * @param value 字符串数值
     * @return 整数值
     */
    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
