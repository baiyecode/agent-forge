package com.baiye.agentforge.ai.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

/**
 * ClassName: RetryOutputGuardrail
 * Package: com.baiye.agentforge.ai.guardrail
 * Description: 检查 AI 响应质量
 *
 * @Author 白夜
 * @Create 2026/6/5 11:20
 * @Version 1.0
 */
public class RetryOutputGuardrail implements OutputGuardrail {

    /**
     * 方法内对响应内容进行 三层检查：空值/过短、敏感内容。
     * 根据检查结果，返回两种状态：
     * reprompt(...)：表示验证失败，但会触发重试机制，将错误信息和修正提示传回模型，要求重新生成。
     * success()：表示验证通过，响应可返回给用户。
     * @param responseFromLLM AI 响应内容
     * @return
     */
    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        //.text() 提取出纯文本响应。
        String response = responseFromLLM.text();
        // 检查响应是否为空或过短
        if (response == null || response.trim().isEmpty()) {
            return reprompt("响应内容为空", "请重新生成完整的内容");
        }
        if (response.trim().length() < 10) {
            return reprompt("响应内容过短", "请提供更详细的内容");
        }
        // 检查是否包含敏感信息或不当内容
        if (containsSensitiveContent(response)) {
            return reprompt("包含敏感信息", "请重新生成内容，避免包含敏感信息");
        }
        return success();
    }

    /**
     * 检查是否包含敏感内容
     */
    private boolean containsSensitiveContent(String response) {
        //先将响应全转为小写，实现不区分大小写的匹配。
        String lowerResponse = response.toLowerCase();
        String[] sensitiveWords = {
                "密码", "password", "secret", "token",
                "api key", "私钥", "证书", "credential"
        };
        for (String word : sensitiveWords) {
            if (lowerResponse.contains(word)) {
                return true;
            }
        }
        return false;
    }
}

