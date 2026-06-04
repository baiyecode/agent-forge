package com.baiye.agentforge.ratelimit.aspect;

import com.baiye.agentforge.exception.BusinessException;
import com.baiye.agentforge.exception.ErrorCode;
import com.baiye.agentforge.model.entity.User;
import com.baiye.agentforge.ratelimit.annotation.RateLimit;
import com.baiye.agentforge.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * ClassName: RateLimitAspect
 * Package: com.baiye.agentforge.ratelimit.aspect
 * Description: 限流切面
 *
 * @Author 白夜
 * @Create 2026/6/4 10:45
 * @Version 1.0
 */
@Aspect //一个 Spring AOP 切面类，用于定义切入点和通知（Advice）。
@Component
@Slf4j
public class RateLimitAspect {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserService userService;


    //前置通知。拦截所有被 @RateLimit 自定义注解标记的方法。
    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint point, RateLimit rateLimit) {
        //根据限流类型和注解参数生成唯一的 Redis Key，用于标识一个限流器。
        String key = generateRateLimitKey(point, rateLimit);
        // 通过 Key 获取 Redisson 的分布式限流器对象。
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        //给这个限流器在 Redis 中的 Key 设置 1 小时过期时间。这是为了防止 Redis 中堆积大量废弃的限流 Key（例如某个 API 下线后，Key 依然存在）。
        rateLimiter.expire(Duration.ofHours(1));
        // trySetRate：初始化限流规则。
        //RateType.OVERALL：表示分布式全局共享配额（所有微服务实例共享同一个令牌桶）。如果是 PER_CLIENT 则是单机/单实例独享。
        //rateLimit.rate()：时间窗口内允许的最大请求数（令牌数）。
        //rateLimit.rateInterval()：时间窗口的大小。
        //RateIntervalUnit.SECONDS：时间窗口的单位（秒）。
        //注：trySetRate 具有幂等性，如果 Redis 中已经存在该限流规则，它不会重复初始化。
        rateLimiter.trySetRate(RateType.OVERALL, rateLimit.rate(), rateLimit.rateInterval(), RateIntervalUnit.SECONDS);
        // 非阻塞尝试获取 1 个令牌。
        //如果获取成功，返回 true，切面放行，目标方法继续执行。
        //如果获取失败（说明达到限流阈值），返回 false，抛出 BusinessException，直接阻断目标方法的执行。
        if (!rateLimiter.tryAcquire(1)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, rateLimit.message());
        }
    }


    /**
     * 该方法根据不同的限流策略（API、USER、IP）生成不同格式的 Redis Key。
     * @param point
     * @param rateLimit
     * @return
     */
    private String generateRateLimitKey(JoinPoint point, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder();
        //统一使用 rate_limit: 作为 Redis Key 的前缀，方便在 Redis 中统一管理。
        keyBuilder.append("rate_limit:");
        // 如果 @RateLimit(key = "xxx") 指定了 key 前缀，则追加到 Key 中，可用来区分不同业务场景。
        if (!rateLimit.key().isEmpty()) {
            keyBuilder.append(rateLimit.key()).append(":");
        }
        // 根据限流类型生成不同的key
        switch (rateLimit.limitType()) {
            case API: // API – 接口级别限流
                //从切点获得方法签名
                MethodSignature signature = (MethodSignature) point.getSignature();
                //获取方法对象,通过方法对象获取类名和方法名
                Method method = signature.getMethod();
                //method.getDeclaringClass() 返回的是声明该方法的类，即方法代码实际所在的类（可能是父类或接口）。
                //比如，如果 UserController 继承自 BaseController，
                //并且 getUser() 方法定义在 BaseController 中，
                //那么 method.getDeclaringClass() 会返回 BaseController.class，
                //而不是 UserController.class。
                //为什么不用 method.getClass() 或者目标对象的实际类？
                //因为限流的 Key 需要 按方法定义来区分，而不是按当前运行时的具体实例类型。一个方法无论被哪个子类继承调用，
                //都应该是同一个限流桶，否则就会出现同一个方法在不同子类中被独立限流的不符合预期的行为。
                //使用 getDeclaringClass() 保证了 Key 的稳定性。
                //
                // .getSimpleName(),返回不包含包名的短类名.
                //
                // method.getName()
                //返回方法名，例如 getUser。
                //对于重载（同一个类中有多个同名方法，但参数不同），这里 仅使用了方法名，没有包含参数类型，因此所有重载方法会共用同一个限流 Key。
                keyBuilder.append("api:").append(method.getDeclaringClass().getSimpleName())
                        .append(".").append(method.getName());
                break;
            case USER:
                // 用户级别：用户ID
                try {
                    //获取 Spring 管理的请求属性
                    //RequestContextHolder 是 Spring 提供的持有请求上下文的工具类，它内部使用 ThreadLocal 存储当前线程关联的请求属性。
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    //如果 attributes 不为 null，说明当前线程是 Web 请求处理线程，可以尝试获取 HttpServletRequest。
                    if (attributes != null) {
                        HttpServletRequest request = attributes.getRequest();
                        User loginUser = userService.getLoginUser(request);
                        keyBuilder.append("user:").append(loginUser.getId());
                    } else {
                        // 无法获取请求上下文，使用IP限流
                        keyBuilder.append("ip:").append(getClientIP());
                    }
                } catch (BusinessException e) {
                    // 未登录用户使用IP限流
                    keyBuilder.append("ip:").append(getClientIP());
                }
                break;
            case IP:
                // IP级别：客户端IP
                keyBuilder.append("ip:").append(getClientIP());
                break;
            default:
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的限流类型");
        }
        return keyBuilder.toString();
    }


    /**
     * 按优先级获取真实客户端 IP，考虑了反向代理的情况
     * @return
     */
    private String getClientIP() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        //标准代理头，格式为 client, proxy1, proxy2，取第一个 IP。
        String ip = request.getHeader("X-Forwarded-For");

        //null：头不存在。
        //isEmpty()：空字符串。
        //"unknown"：部分代理软件当无法获取真实 IP 时会填 unknown。
        //equalsIgnoreCase：String 类的方法，比较两个字符串在忽略大小写的情况下是否相等。
        //若满足任一条件，降级到下一级。

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            //来源：一般由 Nginx 等反向代理单独设置，只包含一个 IP，即实际发起请求的客户端 IP。
            //适用场景：代理层配置了 X-Real-IP 且信任该头，比 X-Forwarded-For 更简洁，不会出现多 IP 列表。
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            //来源：直接从 TCP 连接的对端 IP 获取（java.net.Socket 的对方地址）。
            //适用：没有代理或上述头均未设置时，这是最后的兜底。
            //注意：如果经过反向代理，getRemoteAddr() 返回的是代理服务器的 IP，不是真实客户端 IP，所以优先级最低。
            ip = request.getRemoteAddr();
        }

        // 处理多级代理的情况
        //X-Forwarded-For 可能包含多个 IP，格式如：203.0.113.5, 10.0.0.2, 172.16.0.1。
        //此时需要提取最左边的 IP（原始客户端），使用逗号分割后取第一个元素并去除首尾空格。
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }



}

