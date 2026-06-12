package com.baiye.agentforge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * ClassName: JsonConfig
 * Package: com.baiye.agentforge.config
 * Description: 数据精度修复 Spring MVC Json 配置
 *
 * @Author 白夜
 * @Create 2026/5/4 16:28
 * @Version 1.0
 */
@JsonComponent // 作用：自动将其注册到 Jackson 的全局配置中，覆盖默认的 ObjectMapper。
public class JsonConfig {

    /**
     * 添加 Long 转 json 精度丢失的配置
     * 为什么 Long 会丢失精度？
     * Java 中 Long 的取值范围：-2⁶³ ~ 2⁶³-1。
     * JavaScript 的 Number 类型基于 IEEE 754 双精度浮点数，能安全表示的最大整数是 2⁵³−1（即 9007199254740991）。
     * 当后端返回的 Long 值超过这个安全范围（如雪花算法生成的长 ID），前端直接当数字处理时会精度失真，导致后面几位变成 0。
     * 常见解决方案：序列化 JSON 时，把 Long 转为 "String" 类型输出。
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        // 创建自定义 ObjectMapper,通过 .createXmlMapper(false) 明确只构建 JSON 映射器（不使用 XML）。
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        /*
         * SimpleModule：Jackson 提供的模块，用于批量注册自定义序列化器、反序列化器等。
         * ToStringSerializer.instance：Jackson 内置的序列化器，能将任意对象序列化为其 toString() 形式的 JSON 字符串。
         * Long.class：代表包装类型 Long（即对象）。
         * Long.TYPE：代表基本类型 long（即 long.class）。
         * 两行 addSerializer 覆盖了包装类型和基本类型两种 Long，确保无论是 Long id 还是 long count，最终在 JSON 中都输出为字符串
         */
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(module);
        return objectMapper;
    }
}

