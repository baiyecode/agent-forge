package com.baiye.agentforge.ai;

import com.baiye.agentforge.langgraph4j.model.QualityResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

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
    @SystemMessage(fromResource = "prompt/code-quality-check-system-prompt.txt")
    QualityResult checkCodeQuality(@UserMessage String codeContent);
}

