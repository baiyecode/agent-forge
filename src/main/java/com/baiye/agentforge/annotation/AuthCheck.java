package com.baiye.agentforge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ClassName: AuthCheck
 * Package: com.baiye.agentforge.annotation
 * Description: 权限校验注解
 *
 * @Author 白夜
 * @Create 2026/5/3 20:37
 * @Version 1.0
 */
@Target(ElementType.METHOD) //注解作用于方法
@Retention(RetentionPolicy.RUNTIME) //注解在运行时有效
public @interface AuthCheck {

    /**
     * 必须有某个角色
     */
    String mustRole() default "";
}

