package com.baiye.agentforge.langgraph4j;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.baiye.agentforge.exception.BusinessException;
import com.baiye.agentforge.exception.ErrorCode;
import com.baiye.agentforge.langgraph4j.model.QualityResult;
import com.baiye.agentforge.langgraph4j.node.*;
import com.baiye.agentforge.langgraph4j.node.concurrent.*;
import com.baiye.agentforge.langgraph4j.state.WorkflowContext;
import com.baiye.agentforge.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.*;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * ClassName: CodeGenConcurrentWorkflow
 * Package: com.baiye.agentforge.langgraph4j
 * Description: 代码生成并发工作流
 *
 * @Author 白夜
 * @Create 2026/5/27 19:29
 * @Version 1.0
 */
@Slf4j
public class CodeGenConcurrentWorkflow {

    /**
     * 创建并发工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            // 构造 channels，声明四个图片通道使用 appender 合并
            Map<String, Channel<?>> channels = Map.of(
                    "contentImages", (Channel<?>) Channels.appender(ArrayList::new),
                    "illustrations", (Channel<?>) Channels.appender(ArrayList::new),
                    "diagrams",      (Channel<?>) Channels.appender(ArrayList::new),
                    "logos",         (Channel<?>) Channels.appender(ArrayList::new)
            );

            return new StateGraph<MessagesState<String>>(
                    channels,
                    data -> new MessagesState<>(data)
            )
                    // 添加节点
                    .addNode("image_plan", ImagePlanNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())

                    // 添加并发图片收集节点
                    .addNode("content_image_collector", ContentImageCollectorNode.create())
                    .addNode("illustration_collector", IllustrationCollectorNode.create())
                    .addNode("diagram_collector", DiagramCollectorNode.create())
                    .addNode("logo_collector", LogoCollectorNode.create())
                    .addNode("image_aggregator", ImageAggregatorNode.create())

                    // 添加边
                    .addEdge(START, "image_plan")

                    // 并发分支：从计划节点分发到各个收集节点
                    .addEdge("image_plan", "content_image_collector")
                    .addEdge("image_plan", "illustration_collector")
                    .addEdge("image_plan", "diagram_collector")
                    .addEdge("image_plan", "logo_collector")

                    // 汇聚：所有收集节点都汇聚到聚合器
                    .addEdge("content_image_collector", "image_aggregator")
                    .addEdge("illustration_collector", "image_aggregator")
                    .addEdge("diagram_collector", "image_aggregator")
                    .addEdge("logo_collector", "image_aggregator")

                    // 继续串行流程
                    .addEdge("image_aggregator", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "code_quality_check")

                    // 质检条件边
                    .addConditionalEdges("code_quality_check",
                            edge_async(this::routeAfterQualityCheck),
                            Map.of(
                                    "build", "project_builder",
                                    "skip_build", END,
                                    "fail", "code_generator"
                            ))
                    .addEdge("project_builder", END)
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "并发工作流创建失败");
        }
    }

    /**
     * 执行并发工作流
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("初始化")
                .build();
        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("并发工作流图:\n{}", graph.content());
        log.info("开始执行并发代码生成工作流");
        WorkflowContext finalContext = null;
        int stepCounter = 1;
        // 配置并发执行
        ExecutorService pool = ExecutorBuilder.create()
                .setCorePoolSize(10) // 核心线程数
                .setMaxPoolSize(20) // 最大线程数
                .setWorkQueue(new LinkedBlockingQueue<>(100)) // 使用容量为 100 的有界阻塞队列。
                .setThreadFactory(ThreadFactoryBuilder.create().setNamePrefix("Parallel-Image-Collect").build())
                .build();
        try {
            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .addParallelNodeExecutor("image_plan", pool) //将刚才创建的线程池 绑定到名为 "image_plan" 的节点。
                    .build();
            for (NodeOutput<MessagesState<String>> step : workflow.stream(
                    Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext),
                    runnableConfig
            )) {
                log.info("--- 第 {} 步完成 ---", stepCounter);
                WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                if (currentContext != null) {
                    finalContext = currentContext;
                    log.info("当前步骤上下文: {}", currentContext);
                }
                stepCounter++;
            }
            log.info("并发代码生成工作流执行完成！");
            return finalContext;
        } finally {
            pool.shutdown();// 关闭线程池
        }
    }

    /**
     * 路由函数：根据质检结果决定下一步
     */
    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();

        if (qualityResult == null || !qualityResult.getIsValid()) {
            log.error("代码质检失败，需要重新生成代码");
            return "fail";
        }
        log.info("代码质检通过，继续后续流程");
        CodeGenTypeEnum generationType = context.getGenerationType();
        if (generationType == CodeGenTypeEnum.VUE_PROJECT) {
            return "build";
        } else {
            return "skip_build";
        }
    }
}

