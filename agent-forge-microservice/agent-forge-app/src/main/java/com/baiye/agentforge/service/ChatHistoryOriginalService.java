package com.baiye.agentforge.service;

import com.baiye.agentforge.model.entity.ChatHistoryOriginal;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

/**
 * 原始对话历史 服务层。
 * 为 vue 工程模式恢复对话记忆(包含工具调用信息)
 *
 * @author <a href="https://github.com/baiyecode">白夜</a>
 */
public interface ChatHistoryOriginalService extends IService<ChatHistoryOriginal> {

    /**
     * 添加对话历史
     *
     * @param appId
     * @param message
     * @param messageType
     * @param userId
     * @return
     */
    boolean addOriginalChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 批量添加对话历史
     *
     * @param chatHistoryOriginalList
     * @return
     */
    boolean addOriginalChatMessageBatch(List<ChatHistoryOriginal> chatHistoryOriginalList);

    /**
     * 根据 appId 关联删除对话历史记录
     *
     * @param appId
     * @return
     */
    boolean deleteByAppId(Long appId);

    /**
     * 将 APP 的对话历史加载到缓存中
     *
     * @param appId
     * @param chatMemory
     * @param maxCount
     * @return
     */
    int loadOriginalChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}

