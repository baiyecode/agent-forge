package com.baiye.agentforge.langgraph4j.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * ClassName: QualityResult
 * Package: com.baiye.agentforge.langgraph4j.model
 * Description: 质检结果
 *
 * @Author 白夜
 * @Create 2026/5/26 16:25
 * @Version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否通过质检
     */
    private Boolean isValid;

    /**
     * 错误列表
     */
    private List<String> errors;

    /**
     * 改进建议
     */
    private List<String> suggestions;
}

