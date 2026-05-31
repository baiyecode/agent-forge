package com.baiye.agentforge.langgraph4j.node.concurrent;

import com.baiye.agentforge.langgraph4j.model.ImageResource;
import com.baiye.agentforge.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * ClassName: ImageAggregatorNode
 * Package: com.baiye.agentforge.langgraph4j.node.concurrent
 * Description: 图片聚合节点
 *
 * @Author 白夜
 * @Create 2026/5/27 19:26
 * @Version 1.0
 */
@Slf4j
public class ImageAggregatorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> allImages = new ArrayList<>();
            log.info("开始聚合并发收集的图片");
            // 从合并后的状态通道安全地读取
            appendImages(state, "contentImages", allImages);
            appendImages(state, "illustrations", allImages);
            appendImages(state, "diagrams", allImages);
            appendImages(state, "logos", allImages);
            context.setImageList(allImages);
            context.setCurrentStep("图片聚合");
            return WorkflowContext.saveContext(context);
        });
    }

    private static void appendImages(MessagesState<String> state, String key, List<ImageResource> target) {
        Object obj = state.data().get(key);

        if (obj instanceof List<?> images) {
            for (Object image : images) {
                if (image instanceof ImageResource imageResource) {
                    target.add(imageResource);
                }
            }
        }
    }
}
