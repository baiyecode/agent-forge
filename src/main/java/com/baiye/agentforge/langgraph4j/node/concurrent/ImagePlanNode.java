package com.baiye.agentforge.langgraph4j.node.concurrent;

import com.baiye.agentforge.ai.ImageCollectionPlanService;
import com.baiye.agentforge.langgraph4j.model.ImageCollectionPlan;
import com.baiye.agentforge.langgraph4j.state.WorkflowContext;
import com.baiye.agentforge.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * ClassName: ImagePlanNode
 * Package: com.baiye.agentforge.langgraph4j.node.concurrent
 * Description: 图片收集计划生成节点
 *
 * @Author 白夜
 * @Create 2026/5/27 19:10
 * @Version 1.0
 */
@Slf4j
public class ImagePlanNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            try {
                // 获取图片收集计划服务
                ImageCollectionPlanService planService = SpringContextUtil.getBean(ImageCollectionPlanService.class);
                ImageCollectionPlan plan = planService.planImageCollection(originalPrompt);
                log.info("生成图片收集计划，准备启动并发分支");
                // 将计划存储到上下文中
                context.setImageCollectionPlan(plan);
                context.setCurrentStep("图片计划");
            } catch (Exception e) {
                log.error("图片计划生成失败: {}", e.getMessage(), e);
            }
            return WorkflowContext.saveContext(context);
        });
    }
}

