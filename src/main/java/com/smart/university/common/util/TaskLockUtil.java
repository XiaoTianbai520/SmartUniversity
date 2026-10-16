package com.smart.university.common.util;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.smart.university.common.enums.TaskCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定时任务分布式锁工具，基于 Redis 的 SET NX PX 实现。
 * 释放锁一律走 Lua 脚本校验 value 后删除，禁止裸 DEL，避免误删其他实例持有的锁
 */
@Slf4j
@Component
public class TaskLockUtil {

    /**
     * 默认锁租约时长（秒），未显式指定租约时使用
     */
    private static final long DEFAULT_LEASE_SECONDS = 300L;

    /**
     * 释放锁脚本路径
     */
    private static final String UNLOCK_SCRIPT_PATH = "lua/unlock.lua";

    /**
     * Redis 字符串模板
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 校验 value 后删除的释放锁脚本
     */
    private final RedisScript<Long> unlockScript;

    /**
     * 实例标识，格式为「主机名:端口」，写入锁 value 用于定位持锁实例
     */
    private final String instanceId;

    /**
     * 已抢到的锁令牌，Key 为任务编码，供无参 unlock 使用
     */
    private final Map<String, String> lockTokenMap = new ConcurrentHashMap<>();

    public TaskLockUtil(StringRedisTemplate stringRedisTemplate,
                        @Value("${server.port:8080}") String serverPort) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.instanceId = NetUtil.getLocalHostName() + ":" + serverPort;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(UNLOCK_SCRIPT_PATH)));
        script.setResultType(Long.class);
        this.unlockScript = script;
    }

    /**
     * 获取本实例标识
     *
     * @return 实例标识，格式为「主机名:端口」
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 尝试抢占任务锁，使用默认租约时长
     *
     * @param taskCode 任务编码
     * @return 抢到返回 true，未抢到或 Redis 不可用返回 false
     */
    public boolean tryLock(TaskCodeEnum taskCode) {
        return tryLock(taskCode, DEFAULT_LEASE_SECONDS);
    }

    /**
     * 尝试抢占任务锁
     *
     * @param taskCode     任务编码
     * @param leaseSeconds 锁租约时长（秒），超过该时长锁自动失效
     * @return 抢到返回 true，未抢到或 Redis 不可用返回 false
     */
    public boolean tryLock(TaskCodeEnum taskCode, long leaseSeconds) {
        if (taskCode == null) {
            return false;
        }
        return tryLock(taskCode.getCode(), leaseSeconds);
    }

    /**
     * 尝试抢占任务锁，任务编码以字符串传入
     *
     * @param taskCode     任务编码
     * @param leaseSeconds 锁租约时长（秒）
     * @return 抢到返回 true，未抢到或 Redis 不可用返回 false
     */
    public boolean tryLock(String taskCode, long leaseSeconds) {
        if (StrUtil.isBlank(taskCode)) {
            return false;
        }
        long lease = leaseSeconds > 0 ? leaseSeconds : DEFAULT_LEASE_SECONDS;
        String lockKey = RedisKeyUtil.buildTaskLockKey(taskCode);
        String token = buildLockToken(taskCode);
        try {
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, token, Duration.ofSeconds(lease));
            if (Boolean.TRUE.equals(locked)) {
                lockTokenMap.put(taskCode, token);
                return true;
            }
            return false;
        } catch (Exception ex) {
            log.warn("抢占任务锁失败，任务编码：{}，异常摘要：{}", taskCode, ex.getMessage());
            return false;
        }
    }

    /**
     * 释放任务锁，使用本线程抢锁时生成的令牌
     *
     * @param taskCode 任务编码
     */
    public void unlock(TaskCodeEnum taskCode) {
        if (taskCode == null) {
            return;
        }
        unlock(taskCode.getCode());
    }

    /**
     * 释放任务锁，使用本线程抢锁时生成的令牌
     *
     * @param taskCode 任务编码
     */
    public void unlock(String taskCode) {
        if (StrUtil.isBlank(taskCode)) {
            return;
        }
        String token = lockTokenMap.remove(taskCode);
        if (StrUtil.isBlank(token)) {
            return;
        }
        unlock(taskCode, token);
    }

    /**
     * 释放任务锁，走 Lua 脚本校验 value 后再删除
     *
     * @param taskCode 任务编码
     * @param token    加锁时写入的 value
     */
    public void unlock(String taskCode, String token) {
        if (StrUtil.isBlank(taskCode) || StrUtil.isBlank(token)) {
            return;
        }
        try {
            Long deleted = stringRedisTemplate.execute(unlockScript,
                    List.of(RedisKeyUtil.buildTaskLockKey(taskCode)), token);
            if (deleted == null || deleted == 0L) {
                log.debug("任务锁已过期或被其他实例持有，跳过释放，任务编码：{}", taskCode);
            }
        } catch (Exception ex) {
            log.warn("释放任务锁失败，任务编码：{}，异常摘要：{}", taskCode, ex.getMessage());
        }
    }

    /**
     * 构建锁 value，格式为「实例标识-UUID」，保证不同实例与不同轮次互不冲突
     *
     * @param taskCode 任务编码
     * @return 锁 value
     */
    private String buildLockToken(String taskCode) {
        return instanceId + "-" + taskCode + "-" + UUID.randomUUID();
    }
}
