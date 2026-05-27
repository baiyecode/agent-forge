package com.baiye.agentforge.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: ImageCollectionPlanServiceFactory
 * Package: com.baiye.agentforge.ai
 * Description: 图片收集 AI 服务工厂
 *
 * @Author 白夜
 * @Create 2026/5/27 15:43
 * @Version 1.0
 */
@Configuration
public class ImageCollectionPlanServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Bean
    public ImageCollectionPlanService createImageCollectionPlanService() {
        return AiServices.builder(ImageCollectionPlanService.class)
                .chatModel(chatModel)
                .build();
    }
}

