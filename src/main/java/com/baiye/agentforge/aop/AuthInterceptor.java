package com.baiye.agentforge.aop;

import com.baiye.agentforge.annotation.AuthCheck;
import com.baiye.agentforge.exception.BusinessException;
import com.baiye.agentforge.exception.ErrorCode;
import com.baiye.agentforge.model.entity.User;
import com.baiye.agentforge.model.enums.UserRoleEnum;
import com.baiye.agentforge.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * ClassName: AuthInterceptor
 * Package: com.baiye.agentforge.aop
 * Description: 权限校验拦截器
 *
 * @Author 白夜
 * @Create 2026/5/3 20:39
 * @Version 1.0
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)") // @annotation(authCheck) 表示拦截所有带有AuthCheck注解的方法
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();//取出注解中指定的角色值
        /* RequestContextHolder：Spring 提供给非 Web 组件（如切面、服务层）访问当前请求上下文的工具。
         * currentRequestAttributes()：获取当前线程绑定的请求属性，强转为 ServletRequestAttributes 后即可拿到 HttpServletRequest 对象。
         * 这样切面就能访问请求中的 Session、Header 等，用于获取登录用户信息。
         */
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 当前登录用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 不需要权限，放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();//通过所有校验，执行原方法，并返回原方法的返回值。
        }
        // 以下为：必须有该权限才通过
        // 获取当前用户具有的权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 没有权限，拒绝
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 要求必须有管理员权限，但用户没有管理员权限，拒绝
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}

