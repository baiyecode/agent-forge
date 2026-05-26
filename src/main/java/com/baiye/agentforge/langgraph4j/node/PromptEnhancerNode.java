package com.baiye.agentforge.langgraph4j.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baiye.agentforge.langgraph4j.model.ImageResource;
import com.baiye.agentforge.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * ClassName: PromptEnhancerNode
 * Package: com.baiye.agentforge.langgraph4j.node
 * Description: 提示词增强节点
 * 作用：
 * 1、从工作流上下文中取出用户原始提示词和图片素材（结构化的 imageList 或纯文本 imageListStr）。
 * 2、将图片素材信息以清晰的 Markdown 格式追加到原始提示词末尾，明确告知 AI 要在生成的网站中嵌入这些图片资源。
 * 3、更新上下文中的增强提示词字段，并保存状态，供下游节点（如网站生成节点）使用。
 *
 * @Author 白夜
 * @Create 2026/5/25 14:19
 * @Version 1.0
 */
@Slf4j
public class PromptEnhancerNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 提示词增强");
            // 获取原始提示词和图片列表
            String originalPrompt = context.getOriginalPrompt();
            String imageListStr = context.getImageListStr();// 图片资源字符串，接收 AI 输出的图片信息
            List<ImageResource> imageList = context.getImageList();// 图片资源列表
            // 构建增强后的提示词
            StringBuilder enhancedPromptBuilder = new StringBuilder();
            enhancedPromptBuilder.append(originalPrompt);
            // 如果有图片资源，则添加图片信息，判断集合非空、字符串非空白。
            if (CollUtil.isNotEmpty(imageList) || StrUtil.isNotBlank(imageListStr)) {
                enhancedPromptBuilder.append("\n\n## 可用素材资源\n");// Markdown 二级标题
                enhancedPromptBuilder.append("请在生成网站使用以下图片资源，将这些图片合理地嵌入到网站的相应位置中。\n");
                if (CollUtil.isNotEmpty(imageList)) {
                    for (ImageResource image : imageList) {
                        //按 Markdown 无序列表格式输出：- 类别：描述（URL）。
                        enhancedPromptBuilder.append("- ")
                                .append(image.getCategory().getText())
                                .append("：")
                                .append(image.getDescription())
                                .append("（")
                                .append(image.getUrl())
                                .append("）\n");
                    }
                } else {
                    //如果 imageList 为空但 imageListStr 有内容，则直接将 imageListStr 追加进去（可能是用户手动输入的图片描述文本）。
                    enhancedPromptBuilder.append(imageListStr);
                }
            }
            //得到完整的增强提示词。
            String enhancedPrompt = enhancedPromptBuilder.toString();
            // 更新状态
            context.setCurrentStep("提示词增强");
            context.setEnhancedPrompt(enhancedPrompt);
            log.info("提示词增强完成，增强后长度: {} 字符", enhancedPrompt.length());
            return WorkflowContext.saveContext(context);
        });
    }
}


