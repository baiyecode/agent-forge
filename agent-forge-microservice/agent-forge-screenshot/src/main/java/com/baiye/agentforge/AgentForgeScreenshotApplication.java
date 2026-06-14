package com.baiye.agentforge;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ClassName: AgentForgeScreenshotApplication
 * Package: com.baiye.agentforge
 * Description:
 *
 * @Author 白夜
 * @Create 2026/6/14 10:30
 * @Version 1.0
 */
@SpringBootApplication
@EnableDubbo
public class AgentForgeScreenshotApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentForgeScreenshotApplication.class, args);
    }
}

