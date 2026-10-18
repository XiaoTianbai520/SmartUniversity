package com.smart.university.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

/**
 * 选课容量 Lua 脚本配置，独立于既有的 {@link RedisConfiguration}，不修改其序列化方式
 */
@Configuration
public class SelectionRedisConfiguration {

    /**
     * 容量预扣减脚本路径
     */
    private static final String CAPACITY_DEDUCT_SCRIPT_PATH = "lua/capacity_deduct.lua";

    /**
     * 容量回补脚本路径
     */
    private static final String CAPACITY_RESTORE_SCRIPT_PATH = "lua/capacity_restore.lua";

    /**
     * 注册容量预扣减脚本
     *
     * @return 预扣减脚本
     */
    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    public RedisScript<List<Long>> capacityDeductScript() {
        return buildLongListScript(CAPACITY_DEDUCT_SCRIPT_PATH);
    }

    /**
     * 注册容量回补脚本
     *
     * @return 回补脚本
     */
    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    public RedisScript<List<Long>> capacityRestoreScript() {
        return buildLongListScript(CAPACITY_RESTORE_SCRIPT_PATH);
    }

    /**
     * 构建返回 Long 列表的脚本 Bean，Lua 返回数组由 Spring 转为 List
     *
     * @param scriptPath 脚本 classpath 路径
     * @return 脚本对象
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private RedisScript<List<Long>> buildLongListScript(String scriptPath) {
        DefaultRedisScript script = new DefaultRedisScript();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(scriptPath)));
        script.setResultType(List.class);
        return script;
    }
}
