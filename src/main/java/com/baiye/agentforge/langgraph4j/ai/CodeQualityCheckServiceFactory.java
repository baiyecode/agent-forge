package com.baiye.agentforge.langgraph4j.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: CodeQualityCheckServiceFactory
 * Package: com.baiye.agentforge.ai
 * Description: 代码质量检查服务工厂
 *
 * @Author 白夜
 * @Create 2026/5/26 17:18
 * @Version 1.0
 */
@Slf4j
@Configuration
public class CodeQualityCheckServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    /**
     * 创建代码质量检查 AI 服务
     */
    @Bean
    public CodeQualityCheckService createCodeQualityCheckService() {
        return AiServices.builder(CodeQualityCheckService.class)
                .chatModel(chatModel)
                .build();
    }
}

