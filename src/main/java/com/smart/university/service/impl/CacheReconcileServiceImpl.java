package com.smart.university.service.impl;

import com.smart.university.common.util.RedisKeyUtil;
import com.smart.university.domain.dto.resp.CacheReconcileDetailDTO;
import com.smart.university.domain.dto.resp.CacheReconcileRespDTO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.service.CacheReconcileService;
import com.smart.university.service.SelectionCapacityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 缓存一致性对账服务实现（V1.2 W2）
 *
 * <p>三方对账：DB COUNT / Redis 计数 Key / teaching_class.selected_count。
 * 以 DB 为准回写：Redis 侧调用已存在的 {@code selectionCapacityService.warmup(id)}（幂等，把 Redis 写为 DB 值），
 * DB 列侧调用已冻结的 {@code TeachingClassMapper#resetSelectedCount(id, targetCount)} 回写 selected_count。
 *
 * <p>C1 红线：本实现只校正互斥器，不读取计数用于统计；对外的权威已选人数恒为 DB COUNT。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheReconcileServiceImpl implements CacheReconcileService {

    /**
     * 默认单次扫描上限
     */
    private static final int DEFAULT_LIMIT = 500;

    private final CourseSelectionMapper courseSelectionMapper;

    private final TeachingClassMapper teachingClassMapper;

    private final SelectionCapacityService selectionCapacityService;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public CacheReconcileRespDTO reconcile(Long semesterId, Integer limit) {
        long startTimeMillis = System.currentTimeMillis();
        int scanLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : limit;

        CacheReconcileRespDTO result = new CacheReconcileRespDTO();
        result.setSemesterId(semesterId);

        int scanCount = 0;
        int mismatchCount = 0;
        int fixedCount = 0;
        int failCount = 0;
        List<CacheReconcileDetailDTO> details = new ArrayList<>();

        // 复用 TeachingClassMapper 已有方法获取目标学期下教学班，禁止给冻结 Mapper 新增 default 方法
        List<TeachingClassDO> teachingClassDOList = teachingClassMapper.listTeachingClassBySemesterId(semesterId);
        List<Long> teachingClassIds = teachingClassDOList.stream()
                .map(TeachingClassDO::getId)
                .filter(Objects::nonNull)
                .limit(scanLimit)
                .toList();

        for (Long teachingClassId : teachingClassIds) {
            scanCount++;
            try {
                CacheReconcileDetailDTO detail = reconcileOne(teachingClassId);
                if (Boolean.TRUE.equals(detail.getFixed())) {
                    mismatchCount++;
                    fixedCount++;
                }
                details.add(detail);
            } catch (Exception ex) {
                failCount++;
                log.error("缓存一致性对账失败，teachingClassId={}，异常摘要：{}", teachingClassId, ex.getMessage());
            }
        }

        result.setScanCount(scanCount);
        result.setMismatchCount(mismatchCount);
        result.setFixedCount(fixedCount);
        result.setFailCount(failCount);
        result.setDurationMs(System.currentTimeMillis() - startTimeMillis);
        result.setDetails(details);
        log.info("缓存一致性对账完成，学期={}，扫描={}，不一致={}，已修复={}，失败={}，耗时={}ms",
                semesterId, scanCount, mismatchCount, fixedCount, failCount, result.getDurationMs());
        return result;
    }

    /**
     * 单个教学班三方对账：取 DB 权威值，比对 Redis 计数与 selected_count 列，
     * 任一不一致即以 DB 为准回写两侧。
     *
     * @param teachingClassId 教学班 ID
     * @return 对账明细
     */
    private CacheReconcileDetailDTO reconcileOne(Long teachingClassId) {
        // 1) DB 权威已选人数：COUNT(course_selection WHERE status='SELECTED')
        long dbCount = courseSelectionMapper.countSelectedByTeachingClassId(teachingClassId);

        // 2) Redis 计数 Key：缺失或解析失败均视为不一致
        String redisKey = RedisKeyUtil.buildTeachingClassSelectedCountKey(teachingClassId);
        String redisValue = null;
        Long redisCount = null;
        boolean redisConsistent;
        try {
            redisValue = stringRedisTemplate.opsForValue().get(redisKey);
        } catch (Exception ex) {
            log.warn("读取容量计数 Key 失败，teachingClassId={}，异常摘要：{}", teachingClassId, ex.getMessage());
        }
        if (redisValue == null) {
            redisConsistent = false;
        } else {
            try {
                redisCount = Long.parseLong(redisValue);
                redisConsistent = redisCount == dbCount;
            } catch (NumberFormatException ex) {
                redisConsistent = false;
                redisCount = null;
            }
        }

        // 3) teaching_class.selected_count 列
        TeachingClassDO teachingClassDO = teachingClassMapper.getTeachingClassById(teachingClassId);
        Integer columnCount = teachingClassDO == null ? null : teachingClassDO.getSelectedCount();
        boolean columnConsistent = columnCount != null && columnCount.longValue() == dbCount;

        boolean mismatch = !redisConsistent || !columnConsistent;
        if (mismatch) {
            // 以 DB 为准回写：Redis 侧（幂等覆盖写）
            selectionCapacityService.warmup(teachingClassId);
            // 以 DB 为准回写：DB 列侧
            teachingClassMapper.resetSelectedCount(teachingClassId, (int) dbCount);
        }

        CacheReconcileDetailDTO detail = new CacheReconcileDetailDTO();
        detail.setTeachingClassId(teachingClassId);
        detail.setDbCount(dbCount);
        detail.setRedisCount(redisCount);
        detail.setFixed(mismatch);
        return detail;
    }
}
