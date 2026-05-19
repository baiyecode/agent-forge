package com.baiye.agentforge.core.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baiye.agentforge.ai.model.message.*;
import com.baiye.agentforge.constant.AppConstant;
import com.baiye.agentforge.core.builder.VueProjectBuilder;
import com.baiye.agentforge.model.enums.ChatHistoryMessageTypeEnum;
import com.baiye.agentforge.service.ChatHistoryService;
import com.baiye.agentforge.model.entity.User;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * ClassName: JsonMessageStreamHandler
 * Package: com.baiye.agentforge.core.handler
 * Description: JSON 消息流处理器,处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息
 *
 * @Author 白夜
 * @Create 2026/5/17 19:28
 * @Version 1.0
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {


    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 处理 TokenStream（VUE_PROJECT）
     * 解析 JSON 消息并重组为完整的响应格式
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();//只向前端展示一次工具调用提示。
        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds);
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字串
                .doOnComplete(() -> {
                    // 流式响应完成后，添加 AI 消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    // 异步构造 Vue 项目
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                    vueProjectBuilder.buildProjectAsync(projectPath);
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    /**
     * 解析并收集 TokenStream 数据
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        // 解析 JSON,将 JSON 反序列化为 StreamMessage 基类（含 type 字段）,通过 type 判断具体子类并分别处理。
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (typeEnum) {
            //文本片段
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();//累积全文本
                // 直接拼接响应
                chatHistoryStringBuilder.append(data);
                return data;
            }
            //工具调用请求
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                // 检查是否是第一次看到这个工具 ID    .contains()包含
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 第一次调用这个工具，记录 ID 并完整返回工具信息
                    seenToolIds.add(toolId);
                    return "\n\n[选择工具] 写入文件\n\n";
                } else {
                    // 不是第一次调用这个工具，直接返回空
                    return "";//返回空字符串，经 filter 后丢弃，避免前端重复显示。
                }
            }
            //工具执行完成
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                //getArguments() 返回的是一个 JSON 字符串，内容为工具方法被调用时传入的参数键值对（即 @Tool 方法的参数名和值）。
                //JSONUtil.parseObj 将该字符串解析为 JSONObject，便于按字段名取值。
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                //relativeFilePath：写入文件的相对路径
                String relativeFilePath = jsonObject.getStr("relativeFilePath");
                //suffix：文件后缀名
                String suffix = FileUtil.getSuffix(relativeFilePath);
                //content：AI 生成并写入文件的完整文本内容，将直接嵌入到展示消息中。
                String content = jsonObject.getStr("content");
                //格式化字符串
                String result = String.format("""   
                        [工具调用] 写入文件 %s
                        ```%s
                        %s
                        ```
                        """, relativeFilePath, suffix, content);
                // 输出前端和要持久化的内容
                //在 result 前后各加两个换行符 \n\n，让它在整个对话流中与前后文本（如 AI 的思考或生成的文字）有视觉间隔，增强可读性。
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }
}

