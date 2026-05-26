package com.baiye.agentforge.utils;


import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * ClassName: SpringContextUtil
 * Package: com.baiye.agentforge.utils
 * Description: Spring上下文工具类,用于在静态方法中获取Spring Bean
 * 在非 Spring 管理的普通类中获取 Spring 容器中的 Bean。
 *
 * 1、Spring 启动时，扫描 SpringContextUtil 并将其作为一个 Bean 实例化。
 * 2、由于它实现了 ApplicationContextAware，Spring 会检测到，并调用 setApplicationContext，
 *    把全局唯一的 ApplicationContext 传进去。
 * 3、该工具类把上下文保存在静态变量中。
 * 4、之后任何地方的代码都可以通过 SpringContextUtil.getBean(...) 来获取 Spring 容器中的 Bean，
 *    全程与容器解耦（虽然这是一种与 Spring 耦合的获取方式，但提供了在静态上下文中访问 Bean 的能力）。
 *
 * @Author 白夜
 * @Create 2026/5/25 20:30
 * @Version 1.0
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * Spring 容器会在该 Bean 实例化后、初始化时，自动调用该方法，把当前的 ApplicationContext 传进来。
     * 我们将传入的上下文赋值给静态变量 applicationContext，这样后面的静态方法就可以通过它来获取 Bean。
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.applicationContext = applicationContext;
    }

    /**
     * 获取Spring Bean
     * 按类型获取 Bean
     * <T> T：泛型方法，返回值类型与传入的 Class 对象一致，无需强制转换。
     */
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    /**
     * 获取Spring Bean
     * 按名称（ID）获取 Bean
     * 通过 Bean 的名称（默认是类名首字母小写，或在 @Component("xxx") 中指定的名称）获取。
     * 返回类型为 Object，调用者需要手动做类型转换。
     *
     */
    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    /**
     * 根据名称和类型获取Spring Bean
     * 同时指定名称和类型，这样更安全，既能保证唯一性，又避免了类型转换。
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return applicationContext.getBean(name, clazz);
    }
}

