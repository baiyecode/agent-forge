package com.baiye.agentforge.langgraph4j.ai;

import com.baiye.agentforge.langgraph4j.model.ImageCollectionPlan;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * ClassName: ImageCollectionPlanService
 * Package: com.baiye.agentforge.ai
 * Description: 图片收集 AI 服务
 *
 * @Author 白夜
 * @Create 2026/5/27 15:39
 * @Version 1.0
 */
public interface ImageCollectionPlanService {

    /**
     * 根据用户提示词分析需要收集的图片类型和参数
     */
    @SystemMessage(fromResource = "prompt/image-collection-plan-system-prompt.txt")
    ImageCollectionPlan planImageCollection(@UserMessage String userPrompt);
}

