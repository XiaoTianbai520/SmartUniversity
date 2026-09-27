package com.smart.university.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置，统一 Key / Value 序列化方式，避免存入不可读的二进制
 */
@Configuration
public class RedisConfiguration {

    /**
     * 构建对象型 RedisTemplate
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> result = new RedisTemplate<>();
        result.setConnectionFactory(redisConnectionFactory);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        result.setKeySerializer(stringRedisSerializer);
        result.setHashKeySerializer(stringRedisSerializer);
        result.setValueSerializer(jsonRedisSerializer);
        result.setHashValueSerializer(jsonRedisSerializer);
        result.afterPropertiesSet();
        return result;
    }
}
