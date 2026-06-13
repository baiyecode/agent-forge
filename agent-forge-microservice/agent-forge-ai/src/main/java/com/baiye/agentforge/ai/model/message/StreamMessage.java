package com.baiye.agentforge.ai.model.message;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ClassName: StreamMessage
 * Package: com.baiye.agentforge.ai.model.message
 * Description: 流式消息响应基类
 *
 * @Author 白夜
 * @Create 2026/5/17 15:47
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamMessage {
    private String type;
}

