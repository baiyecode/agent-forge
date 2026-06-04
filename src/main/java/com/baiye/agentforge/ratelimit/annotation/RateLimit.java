package com.baiye.agentforge.ratelimit.annotation;

import com.baiye.agentforge.ratelimit.enums.RateLimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ClassName: RateLimit
 * Package: com.baiye.agentforge.ratelimit.annotation
 * Description:
 *
 * @Author 白夜
 * @Create 2026/6/4 10:39
 * @Version 1.0
 */
@Target({ElementType.METHOD}) // 注解在方法上
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时有效
public @interface RateLimit {

    /**
     * 限流key前缀
     */
    String key() default "";

    /**
     * 每个时间窗口允许的请求数
     */
    int rate() default 10;

    /**
     * 时间窗口（秒）
     */
    int rateInterval() default 1;

    /**
     * 限流类型
     */
    RateLimitType limitType() default RateLimitType.USER;

    /**
     * 限流提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}

