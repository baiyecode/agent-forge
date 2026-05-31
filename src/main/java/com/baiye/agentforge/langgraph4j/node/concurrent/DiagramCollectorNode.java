package com.baiye.agentforge.langgraph4j.node.concurrent;

import com.baiye.agentforge.langgraph4j.model.ImageCollectionPlan;
import com.baiye.agentforge.langgraph4j.model.ImageResource;
import com.baiye.agentforge.langgraph4j.state.WorkflowContext;
import com.baiye.agentforge.langgraph4j.tools.MermaidDiagramTool;
import com.baiye.agentforge.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * ClassName: DiagramCollectorNode
 * Package: com.baiye.agentforge.langgraph4j.node.concurrent
 * Description: 架构图生成节点
 *
 * @Author 白夜
 * @Create 2026/5/27 19:22
 * @Version 1.0
 */
@Slf4j
public class DiagramCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> diagrams = new ArrayList<>();
            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getDiagramTasks() != null) {
                    MermaidDiagramTool diagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);
                    log.info("开始并发生成架构图，任务数: {}", plan.getDiagramTasks().size());
                    for (ImageCollectionPlan.DiagramTask task : plan.getDiagramTasks()) {
                        List<ImageResource> images = diagramTool.generateMermaidDiagram(
                                task.mermaidCode(), task.description());
                        if (images != null) {
                            diagrams.addAll(images);
                        }
                    }
                    log.info("架构图生成完成，共生成 {} 张图片", diagrams.size());
                }
            } catch (Exception e) {
                log.error("架构图生成失败: {}", e.getMessage(), e);
            }
            return Map.of("diagrams", diagrams);
        });
    }
}

