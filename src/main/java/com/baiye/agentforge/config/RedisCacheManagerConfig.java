package com.baiye.agentforge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Resource;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * ClassName: RedisCacheManagerConfig
 * Package: com.baiye.agentforge.config
 * Description: 缓存管理器
 *
 * @Author 白夜
 * @Create 2026/6/2 20:24
 * @Version 1.0
 */
@Configuration
public class RedisCacheManagerConfig {

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    public CacheManager cacheManager() {
        // 配置 ObjectMapper 支持 Java8 时间类型
        ObjectMapper objectMapper = new ObjectMapper();//负责 Java 对象与 JSON 之间的序列化/反序列化。
        //JavaTimeModule：用于处理 Java 8 引入的时间 API（LocalDate、LocalDateTime、Instant 等）。
        //如果没有注册该模块，Jackson 无法正确序列化这些新时间类型，会抛出异常或序列化成不直观的结构。
        objectMapper.registerModule(new JavaTimeModule());

        // 默认配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // 默认 30 分钟过期
                //有时我们希望缓存层不存储 null，而是每次方法返回 null 时都直接执行原方法。
                //此配置禁用 null 缓存，即遇到 null 返回值不放入 Redis，下次查询会直接执行原方法。
                .disableCachingNullValues()
                // key 使用 String 序列化器,使 Key 直接转换为字符串（UTF-8）存入 Redis，人类可读、便于调试。
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()));
                // value 使用 JSON 序列化器（支持复杂对象）
                //该序列化器会将 Java 对象序列化为 JSON 格式存储，
                //同时会在 JSON 中添加 @class 属性记录原始类的全限定名，
                //反序列化时可以还原为正确的 Java 对象（支持泛型、多态等）。
                //.serializeValuesWith(RedisSerializationContext.SerializationPair
                //        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                // 针对 good_app_page 配置5分钟过期
                .withCacheConfiguration("good_app_page",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .build();
    }
}

