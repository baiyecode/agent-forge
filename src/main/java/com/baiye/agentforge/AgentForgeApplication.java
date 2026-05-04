package com.baiye.agentforge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.baiye.agentforge.mapper")
@SpringBootApplication
public class AgentForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentForgeApplication.class, args);
    }

}
