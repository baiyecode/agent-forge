package com.baiye.agentforge;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * ClassName: AgentForgeApplication
 * Package: com.baiye.agentforge
 * Description:
 *
 * @Author 白夜
 * @Create 2026/6/13 20:12
 * @Version 1.0
 */
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.baiye.agentforge.mapper")
@EnableCaching
public class AgentForgeAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentForgeAppApplication.class, args);
    }
}

