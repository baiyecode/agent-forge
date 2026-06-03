package com.baiye.agentforge.langgraph4j.ai;

import com.baiye.agentforge.langgraph4j.model.QualityResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * ClassName: CodeQuailtyCheckService
 * Package: com.baiye.agentforge.ai
 * Description: 质量检查 AI 服务
 *
 * @Author 白夜
 * @Create 2026/5/26 17:16
 * @Version 1.0
 */
public interface CodeQualityCheckService {

    /**
     * 检查代码质量
     * AI 会分析代码并返回质量检查结果
     */
    @UserMessage("{{userMessage}}")
    @SystemMessage(fromResource = "prompt/code-quality-check-system-prompt.txt")
    //QualityResult checkCodeQuality(@UserMessage String codeContent);
    //langchain4j会把代码里的{{msg}}块当成模板，导致报错,利用@V注解解决
    QualityResult checkCodeQuality(@V("userMessage") String userMessage);
}

