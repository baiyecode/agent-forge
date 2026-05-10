package com.baiye.agentforge.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: AppDeployRequest
 * Package: com.baiye.agentforge.model.dto.app
 * Description: 部署请求类
 *
 * @Author 白夜
 * @Create 2026/5/9 15:13
 * @Version 1.0
 */
@Data
public class AppDeployRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long appId;

    private static final long serialVersionUID = 1L;
}

