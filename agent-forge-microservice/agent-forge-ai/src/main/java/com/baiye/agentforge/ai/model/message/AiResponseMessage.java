package com.baiye.agentforge.ai.model.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * ClassName: AiResponseMessage
 * Package: com.baiye.agentforge.ai.model.message
 * Description: AI 响应消息
 *
 * @Author 白夜
 * @Create 2026/5/17 15:54
 * @Version 1.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class AiResponseMessage extends StreamMessage {

    private String data;

    public AiResponseMessage(String data) {
        super(StreamMessageTypeEnum.AI_RESPONSE.getValue());
        this.data = data;
    }
}

