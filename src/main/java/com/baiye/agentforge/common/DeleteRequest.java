package com.baiye.agentforge.common;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: DeleteRequest
 * Package: com.baiye.agentforge.common
 * Description: 删除请求包装类
 *
 * @Author 白夜
 * @Create 2026/3/20 14:24
 * @Version 1.0
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}

