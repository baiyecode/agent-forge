package com.baiye.agentforge.ai;

import com.baiye.agentforge.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;

/**
 * ClassName: AiCodeGenTypeRoutingService
 * Package: com.baiye.agentforge.ai
 * Description: AI代码生成类型智能路由服务,使用结构化输出直接返回枚举类型
 *
 * @Author 白夜
 * @Create 2026/5/20 16:46
 * @Version 1.0
 */
public interface AiCodeGenTypeRoutingService {

    /**
     * 根据用户需求智能选择代码生成类型
     *
     * @param userPrompt 用户输入的需求描述
     * @return 推荐的代码生成类型
     */
    @SystemMessage(fromResource = "prompt/codegen-routing-system-prompt.txt")
    CodeGenTypeEnum routeCodeGenType(String userPrompt);
}

