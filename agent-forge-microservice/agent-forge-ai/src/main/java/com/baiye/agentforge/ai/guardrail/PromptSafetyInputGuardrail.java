package com.baiye.agentforge.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ClassName: PromptSafetyInputGuardrail
 * Package: com.baiye.agentforge.ai.guardrail
 * Description: 在用户输入传递给 AI 模型之前进行安全审查
 *
 * @Author 白夜
 * @Create 2026/6/5 10:34
 * @Version 1.0
 */
public class PromptSafetyInputGuardrail implements InputGuardrail {

    // 敏感词列表
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "忽略之前的指令", "ignore previous instructions", "ignore above",
            "破解", "hack", "绕过", "bypass", "越狱", "jailbreak"
    );

    //这些是预编译的正则表达式，用来捕获更复杂的提示注入句式，防止简单关键词匹配的遗漏。
    //忽略大小写 (?i)。
    //\s+ 允许任意数量的空白字符。
    //(?:...) 是非捕获组，只做分组不存储结果，提高性能。
    //\s* 允许冒号前后有空格
    // 注入攻击模式
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)ignore\\s+(?:previous|above|all)\\s+(?:instructions?|commands?|prompts?)"),
            Pattern.compile("(?i)(?:forget|disregard)\\s+(?:everything|all)\\s+(?:above|before)"),
            Pattern.compile("(?i)(?:pretend|act|behave)\\s+(?:as|like)\\s+(?:if|you\\s+are)"),
            Pattern.compile("(?i)system\\s*:\\s*you\\s+are"),
            Pattern.compile("(?i)new\\s+(?:instructions?|commands?|prompts?)\\s*:")
    );

    /**
     * 返回 InputGuardrailResult：有两种状态：
     * fatal(...)：表示验证失败，请求被拦截，同时附带失败原因。
     * success()：表示验证通过，允许继续处理。
     * @param userMessage 用户消息
     * @return
     */
    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        //singleText() 提取出纯文本字符串。
        String input = userMessage.singleText();
        // 检查输入长度
        if (input.length() > 1000) {
            return fatal("输入内容过长，不要超过 1000 字");
        }
        //经过 trim() 去除首尾空白后，如果字符串为空，说明用户没有提供有效内容，直接拒绝。
        if (input.trim().isEmpty()) {
            return fatal("输入内容不能为空");
        }
        // 检查敏感词
        //将输入全部转为小写，进行不区分大小写的包含匹配。
        String lowerInput = input.toLowerCase();
        //遍历敏感词列表，只要输入中包含任意一个敏感词，马上拦截。
        for (String sensitiveWord : SENSITIVE_WORDS) {
            if (lowerInput.contains(sensitiveWord.toLowerCase())) {
                return fatal("输入包含不当内容，请修改后重试");
            }
        }
        // 检查注入攻击模式
        //遍历预编译的正则表达式列表，用 matcher(input).find() 判断输入字符串中是否存在匹配的子串。
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return fatal("检测到恶意输入，请求被拒绝");
            }
        }
        return success();
    }
}

