package com.baiye.agentforgeuser;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * ClassName: AgentForgeUserApplication
 * Package: com.baiye.agentforgeuser
 * Description:
 *
 * @Author 白夜
 * @Create 2026/6/13 10:20
 * @Version 1.0
 */
@SpringBootApplication
@MapperScan("com.baiye.agentforgeuser.mapper")
@ComponentScan("com.baiye")
public class AgentForgeUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentForgeUserApplication.class, args);
    }
}

