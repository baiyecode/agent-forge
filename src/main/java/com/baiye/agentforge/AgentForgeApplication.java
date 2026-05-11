package com.baiye.agentforge;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.baiye.agentforge.mapper")
//排除langchain4j的Redis 的 Embedding 向量存储的 Bean 配置
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
public class AgentForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentForgeApplication.class, args);
    }

}
