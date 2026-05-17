package com.baiye.agentforge.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: ReasoningStreamingChatModelConfig
 * Package: com.baiye.agentforge.config
 * Description:
 *
 * @Author 白夜
 * @Create 2026/5/17 12:31
 * @Version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;//DeepSeek 兼容 OpenAI 的 API 地址，例如 https://api.deepseek.com。

    private String apiKey;

    /**
     * 推理流式模型（用于 Vue 项目生成，带工具调用）
     */
    @Bean
    public StreamingChatModel reasoningStreamingChatModel() {
        // 为了测试方便临时修改
        final String modelName = "deepseek-v4-flash";
        final int maxTokens = 32768;
        // 生产环境使用：
        // final String modelName = "deepseek-v4-pro";
        // final int maxTokens = 32768;
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}

