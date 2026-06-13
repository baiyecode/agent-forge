package com.baiye.agentforge.ratelimit.enums;

/**
 * ClassName: RateLimitType
 * Package: com.baiye.agentforge.ratelimit.enums
 * Description: 限流类型枚举
 *
 * @Author 白夜
 * @Create 2026/6/4 10:38
 * @Version 1.0
 */
public enum RateLimitType {

    /**
     * 接口级别限流
     */
    API,

    /**
     * 用户级别限流
     */
    USER,

    /**
     * IP级别限流
     */
    IP
}

