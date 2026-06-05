package com.baiye.agentforge.ai;

import com.baiye.agentforge.ai.guardrail.PromptSafetyInputGuardrail;
import com.baiye.agentforge.ai.guardrail.RetryOutputGuardrail;
import com.baiye.agentforge.ai.tools.*;
import com.baiye.agentforge.exception.BusinessException;
import com.baiye.agentforge.exception.ErrorCode;
import com.baiye.agentforge.model.enums.CodeGenTypeEnum;
import com.baiye.agentforge.service.ChatHistoryOriginalService;
import com.baiye.agentforge.service.ChatHistoryService;
import com.baiye.agentforge.utils.SpringContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * ClassName: AiCodeGeneratorServiceFactory
 * Package: com.baiye.agentforge.ai
 * Description: 工厂类初始化AI服务
 *
 * @Author 白夜
 * @Create 2026/5/4 20:46
 * @Version 1.0
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ChatHistoryOriginalService chatHistoryOriginalService;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ToolManager toolManager;


    //private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
    //        .maximumSize(1000)
    //        .expireAfterWrite(Duration.ofMinutes(30))
    //        .expireAfterAccess(Duration.ofMinutes(10))
    //        .removalListener((key, value, cause) -> {
    //            log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
    //        })
    //        .build();

    /**
     * 因为 AI 服务实例的构建依赖 appId（用于记忆隔离）和 codeGenType（用于模型和工具组合），
     * 仅用 Long 类型的 appId 无法区分同一个项目使用不同生成类型时的服务实例。
     * 第二个 String 缓存则可以将多个因素组合成一个字符串键，解决这个问题。
     */

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（带缓存）这个方法是为了兼容历史逻辑
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 和代码生成类型获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }


    /**
     * 构建缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

    /**
     * 默认提供一个 Bean
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }

    /**
     * 创建新的 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        AiCodeGeneratorService aiCodeGeneratorService;
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(60) // 一次工具调用也算一次记忆，maxMessages得设置得大一点，不然模型会失忆一直循环调用工具
                .build();
        // 根据代码生成类型选择不同的模型配置
        switch (codeGenType) {
            // Vue 项目生成使用推理模型
            case VUE_PROJECT -> {
                // 从数据库加载历史对话到缓存中，由于多了工具调用相关信息，加载的最大数量稍微多一些
                chatHistoryOriginalService.loadOriginalChatHistoryToMemory(appId, chatMemory, 50);
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                aiCodeGeneratorService = AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory) //绑定对话记忆。
                        .tools(toolManager.getAllTools())
                        // 默认行为：当框架收到一个未知的工具名称时，通常会直接抛出异常（如 IllegalStateException 或自定义异常），
                        // 导致整个对话流程中断，用户可能会看到错误，且无法自动恢复。
                        // hallucinatedToolNameStrategy 定义一种“柔性处理”：
                        // 不中断流程，而是返回一条错误消息给模型，让它知道这个工具不存在，并有机会修正自己的行为。
                        // 当框架检测到模型请求的工具名称未被注册时，会调用这个函数，将原始的 ToolExecutionRequest 传递进去。
                        // 你需要返回一个 ToolExecutionResultMessage，作为“虚拟”的工具执行结果返回给模型，告诉模型发生了什么（通常是错误信息）。
                        .hallucinatedToolNameStrategy(toolExecutionRequest ->
                                ToolExecutionResultMessage.from(toolExecutionRequest,
                                        "Error: there is no tool called " + toolExecutionRequest.name())
                        )
                        .maxSequentialToolsInvocations(20)  // 最多连续调用 20 次工具
                        .inputGuardrails(new PromptSafetyInputGuardrail())  // 添加输入护轨
                        //.outputGuardrails(new RetryOutputGuardrail()) // 添加输出护轨，为了流式输出，这里不使用
                        .build();
            }
            // HTML 和多文件生成使用默认模型
            case HTML, MULTI_FILE -> {
                // 从数据库加载历史对话到缓存中
                chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                aiCodeGeneratorService = AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .inputGuardrails(new PromptSafetyInputGuardrail())  // 添加输入护轨
                        //.outputGuardrails(new RetryOutputGuardrail()) // 添加输出护轨，为了流式输出，这里不使用
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        }
        return aiCodeGeneratorService;
    }
}


