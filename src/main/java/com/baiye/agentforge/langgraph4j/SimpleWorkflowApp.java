package com.baiye.agentforge.langgraph4j;


import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.StateGraph.END;

/**
 * ClassName: SimpleWorkflowApp
 * Package: com.baiye.agentforge.langgraph4j.demo
 * Description: 简化版网站生成工作流应用 - 使用 MessagesState
 *
 * @Author 白夜
 * @Create 2026/5/24 15:34
 * @Version 1.0
 */
@Slf4j
public class SimpleWorkflowApp {

    /**
     * 创建工作节点的通用方法
     */
    static AsyncNodeAction<MessagesState<String>> makeNode(String message) {
        return node_async(state -> {
            log.info("执行节点: {}", message);
            return Map.of("messages", message);
        });
    }

    /**
     * 工作流图的构建
     * MessagesStateGraph<String> 是状态图的构建器，泛型参数 String 表示消息内容的类型。
     * 链式调用逐步定义节点和边，最后调用 .compile() 生成不可变的 CompiledGraph。
     * @param args
     * @throws GraphStateException
     */
    public static void main(String[] args) throws GraphStateException {
        // 创建工作流图
        CompiledGraph<MessagesState<String>> workflow = new MessagesStateGraph<String>()
                // 添加节点
                //每个 .addNode(节点ID, 动作) 注册一个节点。节点 ID 是字符串，动作由 makeNode 创建，
                // 因此每个节点被触发时只会执行日志打印并更新状态中的消息。
                .addNode("image_collector", makeNode("获取图片素材"))
                .addNode("prompt_enhancer", makeNode("增强提示词"))
                .addNode("router", makeNode("智能路由选择"))
                .addNode("code_generator", makeNode("网站代码生成"))
                .addNode("project_builder", makeNode("项目构建"))

                // 添加边
                //START 和 END 是框架预定义的虚拟节点，表示图的入口和出口。
                //这里全部使用普通边（固定跳转），因此工作流严格按照顺序执行：
                .addEdge(START, "image_collector")                // 开始 -> 图片收集
                .addEdge("image_collector", "prompt_enhancer")    // 图片收集 -> 提示词增强
                .addEdge("prompt_enhancer", "router")             // 提示词增强 -> 智能路由
                .addEdge("router", "code_generator")              // 智能路由 -> 代码生成
                .addEdge("code_generator", "project_builder")     // 代码生成 -> 项目构建
                .addEdge("project_builder", END)                  // 项目构建 -> 结束

                // 编译工作流
                .compile();

        log.info("开始执行工作流");

        //getGraph 方法可以以不同格式导出图结构，这里选择了 Mermaid 格式（常用于 Markdown 绘图）。
        //打印出来的图可以直观看到整个工作流的节点和边。
        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图: \n{}", graph.content());

        // 执行工作流
        int stepCounter = 1;
        //workflow.stream(initialState) 启动工作流并返回一个 可迭代的流，每执行完一个节点就产生一个 NodeOutput 对象。
        //Map.of() 作为初始状态传入（空 Map），表示初始的 MessagesState 没有消息。
        //循环遍历每个步骤的输出：
        //stepCounter 记录当前是第几步。
        //step 包含这一步的节点 ID、执行后的状态等信息。
        //因为图中有 5 个实际节点，所以循环会执行 5 次，依次打印每个节点完成后的输出。
        for (NodeOutput<MessagesState<String>> step : workflow.stream(Map.of())) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
            log.info("步骤输出: {}", step);
            stepCounter++;
        }

        log.info("工作流执行完成！");
    }
}

