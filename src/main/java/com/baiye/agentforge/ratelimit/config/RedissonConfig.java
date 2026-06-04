package com.baiye.agentforge.ratelimit.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.SingleServerConfig;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: RedissonConfig
 * Package: com.baiye.agentforge.ratelimit.config
 * Description: Redisson 客户端配置
 *
 * @Author 白夜
 * @Create 2026/6/4 9:49
 * @Version 1.0
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.data.redis.database}")
    private Integer redisDatabase;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        //地址格式为 redis://主机:端口,Redisson 内部会根据这个地址建立 TCP 连接。
        String address = "redis://" + redisHost + ":" + redisPort;
        SingleServerConfig singleServerConfig = config.useSingleServer() //指定使用单机模式（非集群、非哨兵）配置，
                .setAddress(address)
                .setDatabase(redisDatabase)
                .setConnectionMinimumIdleSize(1) //连接池中最小空闲连接数。即便没有请求，池中也保持至少这么多连接，避免冷启动。
                .setConnectionPoolSize(10) //连接池最大大小，最多同时有 10 个连接。超过该数目会等待或报错。
                .setIdleConnectionTimeout(30000)// 30 秒，空闲连接超过此时间会被关闭（节省资源）。
                .setConnectTimeout(5000) //与 Redis 建立连接的超时时间。若超过 5 秒没连上，则判定连接失败。
                .setTimeout(3000) // 3 秒,命令响应超时时间。即客户端等待 Redis 返回结果的最长时间，超过则抛出超时异常。
                .setRetryAttempts(3) //命令执行失败后的最大重试次数（部分场景下如网络闪断会重试）。
                .setRetryInterval(1500); // 1.5 秒,每次重试之间的间隔时间。
        // 如果有密码则设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            singleServerConfig.setPassword(redisPassword);
        }
        return Redisson.create(config);
    }
}

