package com.baiye.agentforge.controller;

/**
 * ClassName: WorkflowSseController
 * Package: com.baiye.agentforge.controller
 * Description:
 *
 * @Author 白夜
 * @Create 2026/6/2 9:49
 * @Version 1.0
 */

import com.baiye.agentforge.langgraph4j.CodeGenWorkflow;
import com.baiye.agentforge.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * 工作流 SSE 控制器
 * 演示 LangGraph4j 工作流的流式输出功能
 */
@RestController
@RequestMapping("/workflow")
@Slf4j
public class WorkflowSseController {

    /**
     * 同步执行工作流
     */
    @PostMapping("/execute")
    public WorkflowContext executeWorkflow(@RequestParam String prompt) {
        log.info("收到同步工作流执行请求: {}", prompt);
        return new CodeGenWorkflow().executeWorkflow(prompt);
    }

    /**
     * Flux 流式执行工作流
     *
     * @GetMapping("/execute-flux") 映射 HTTP GET 请求到 /workflow/execute-flux（注意这里是 GET，因为 SSE 通常用 GET 长连接）。
     * produces = MediaType.TEXT_EVENT_STREAM_VALUE
     * 声明响应的 Content-Type 为 text/event-stream，这是 SSE 协议要求的 MIME 类型，告诉浏览器或客户端这是一个事件流。
     * 返回值 Flux<String>
     * 返回一个 Reactor 的 Flux 对象，它代表一个异步的、可以随时间多次发送数据的流。
     * Spring WebFlux 会自动将 Flux 中的每个字符串元素作为 SSE 消息逐条发送给客户端，保持连接不断开，直到流完成或出错。
     */
    @GetMapping(value = "/execute-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> executeWorkflowWithFlux(@RequestParam String prompt) {
        log.info("收到 Flux 工作流执行请求: {}", prompt);
        return new CodeGenWorkflow().executeWorkflowWithFlux(prompt);
    }

    /**
     * SSE 流式执行工作流
     */
    @GetMapping(value = "/execute-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeWorkflowWithSse(@RequestParam String prompt) {
        log.info("收到 SSE 工作流执行请求: {}", prompt);
        return new CodeGenWorkflow().executeWorkflowWithSse(prompt);
    }

}

