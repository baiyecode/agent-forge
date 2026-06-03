package com.baiye.agentforge.langgraph4j.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * ClassName: ImageCollectionService
 * Package: com.baiye.agentforge.ai
 * Description: 图片收集 AI 服务
 *
 * @Author 白夜
 * @Create 2026/5/25 20:03
 * @Version 1.0
 */
public interface ImageCollectionService {

    /**
     * 根据用户提示词收集所需的图片资源
     * AI 会根据需求自主选择调用相应的工具
     */
    @SystemMessage(fromResource = "prompt/image-collection-system-prompt.txt")
    String collectImages(@UserMessage String userPrompt);
}

