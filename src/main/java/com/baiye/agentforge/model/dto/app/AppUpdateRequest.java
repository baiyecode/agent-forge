package com.baiye.agentforge.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户修改应用请求（仅支持修改应用名称）
 *
 * @author <a href="https://github.com/baiyecode">白夜</a>
 */
@Data
public class AppUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    private static final long serialVersionUID = 1L;
}
