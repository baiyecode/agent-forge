package com.baiye.agentforge.ai.model.message;

import dev.langchain4j.service.tool.ToolExecution;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * ClassName: ToolExecutedMessage
 * Package: com.baiye.agentforge.ai.model.message
 * Description: 工具执行结果消息
 *
 * @Author 白夜
 * @Create 2026/5/17 16:01
 * @Version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolExecutedMessage extends StreamMessage {

    private String id;

    private String name;

    private String arguments;//工具方法被调用时传入的参数键值对（即 @Tool 方法的参数名和值）。

    private String result;

    public ToolExecutedMessage(ToolExecution toolExecution) {
        super(StreamMessageTypeEnum.TOOL_EXECUTED.getValue());
        this.id = toolExecution.request().id();
        this.name = toolExecution.request().name();
        this.arguments = toolExecution.request().arguments();
        this.result = toolExecution.result();
    }
}

