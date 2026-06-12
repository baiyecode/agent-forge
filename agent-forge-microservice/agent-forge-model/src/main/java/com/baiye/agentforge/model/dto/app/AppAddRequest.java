package com.baiye.agentforge.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建应用请求
 *
 * @author <a href="https://github.com/baiyecode">白夜</a>
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt（必填）
     */
    private String initPrompt;

    private static final long serialVersionUID = 1L;
}
