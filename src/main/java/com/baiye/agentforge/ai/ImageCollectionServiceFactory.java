package com.baiye.agentforge.ai;

import com.baiye.agentforge.langgraph4j.tools.ImageSearchTool;
import com.baiye.agentforge.langgraph4j.tools.LogoGeneratorTool;
import com.baiye.agentforge.langgraph4j.tools.MermaidDiagramTool;
import com.baiye.agentforge.langgraph4j.tools.UndrawIllustrationTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: ImageCollectionServiceFactory
 * Package: com.baiye.agentforge.ai
 * Description: 图片收集 AI 服务工厂
 *
 * @Author 白夜
 * @Create 2026/5/25 20:08
 * @Version 1.0
 */
@Slf4j
@Configuration
public class ImageCollectionServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private ImageSearchTool imageSearchTool;

    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    /**
     * 创建图片收集 AI 服务
     */
    @Bean
    public ImageCollectionService createImageCollectionService() {
        return AiServices.builder(ImageCollectionService.class)
                .chatModel(chatModel)
                .tools(
                        imageSearchTool,
                        undrawIllustrationTool,
                        mermaidDiagramTool,
                        logoGeneratorTool
                )
                .build();
    }
}

