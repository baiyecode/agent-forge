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

    /**
     * 如果 state 中 key 对应的值是一个 List，
     * 就把其中所有 ImageResource 类型的元素加入 target 列表；否则什么也不做。
     * @param state
     * @param key
     * @param target
     */
    private static void appendImages(MessagesState<String> state, String key, List<ImageResource> target) {
        Object obj = state.data().get(key);
        //这是 Java 16+ 引入的 instanceof 模式匹配。
        //先判断 obj 是否为 List 类型（泛型擦除后为 List<?>，即任意元素类型的 List）。
        //如果为 true，直接把 obj 转换为 List<?> 并绑定到变量 images，无需手动强转。
        //如果为 false，则跳过整个 if 块，什么都不做。
        //List<?> 表示“元素是某种未知类型的列表”，后续遍历时元素只能当作 Object 处理。
        if (obj instanceof List<?> images) {
            for (Object image : images) {
                //第二次模式匹配：判断 image 是否是 ImageResource 类型。
                //如果是，则自动转换为 ImageResource 并绑定到 imageResource。
                //然后将这个 imageResource 添加到目标列表 target 中。
                //不是 ImageResource 的元素会被静默忽略，这起到了过滤和类型安全的作用。
                //这里没有 else，因此不匹配的元素不会引起异常，也不会被添加。
                if (image instanceof ImageResource imageResource) {
                    target.add(imageResource);
                }
            }
        }
    }
}
