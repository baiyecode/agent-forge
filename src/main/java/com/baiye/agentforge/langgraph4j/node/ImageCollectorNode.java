package com.baiye.agentforge.langgraph4j.node;

import cn.hutool.core.date.StopWatch;
import com.baiye.agentforge.ai.ImageCollectionPlanService;
import com.baiye.agentforge.ai.ImageCollectionService;
import com.baiye.agentforge.langgraph4j.model.ImageCollectionPlan;
import com.baiye.agentforge.langgraph4j.model.ImageResource;
import com.baiye.agentforge.langgraph4j.state.WorkflowContext;
import com.baiye.agentforge.langgraph4j.tools.ImageSearchTool;
import com.baiye.agentforge.langgraph4j.tools.LogoGeneratorTool;
import com.baiye.agentforge.langgraph4j.tools.MermaidDiagramTool;
import com.baiye.agentforge.langgraph4j.tools.UndrawIllustrationTool;
import com.baiye.agentforge.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * ClassName: ImageCollectorNode
 * Package: com.baiye.agentforge.langgraph4j.node
 * Description: 图片收集节点,使用 AI 进行工具调用，收集不同类型的图片
 *
 * @Author 白夜
 * @Create 2026/5/25 11:17
 * @Version 1.0
 */
@Slf4j
public class ImageCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            List<ImageResource> collectedImages = new ArrayList<>();

            // 1. 创建并启动 StopWatch（紧贴在 try 之前）
            //StopWatch stopWatch = new StopWatch();
            //stopWatch.start();
            try {
                // 第一步：获取图片收集计划
                ImageCollectionPlanService planService = SpringContextUtil.getBean(ImageCollectionPlanService.class);
                ImageCollectionPlan plan = planService.planImageCollection(originalPrompt);
                log.info("获取到图片收集计划，开始并发执行");

                // 第二步：并发执行各种图片收集任务，每个任务返回一个 List<ImageResource>。
                List<CompletableFuture<List<ImageResource>>> futures = new ArrayList<>();
                // 并发执行内容图片搜索
                if (plan.getContentImageTasks() != null) {
                    ImageSearchTool imageSearchTool = SpringContextUtil.getBean(ImageSearchTool.class);
                    for (ImageCollectionPlan.ImageSearchTask task : plan.getContentImageTasks()) {
                        //CompletableFuture.supplyAsync 的意义是：
                        //立刻将括号里的任务提交给线程池，并立即返回一个代表这个异步操作的 CompletableFuture。
                        futures.add(CompletableFuture.supplyAsync(() ->
                                imageSearchTool.searchContentImages(task.query())));
                    }
                }
                // 并发执行插画图片搜索
                if (plan.getIllustrationTasks() != null) {
                    UndrawIllustrationTool illustrationTool = SpringContextUtil.getBean(UndrawIllustrationTool.class);
                    for (ImageCollectionPlan.IllustrationTask task : plan.getIllustrationTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                illustrationTool.searchIllustrations(task.query())));
                    }
                }
                // 并发执行架构图生成
                if (plan.getDiagramTasks() != null) {
                    MermaidDiagramTool diagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);
                    for (ImageCollectionPlan.DiagramTask task : plan.getDiagramTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                diagramTool.generateMermaidDiagram(task.mermaidCode(), task.description())));
                    }
                }
                // 并发执行Logo生成
                if (plan.getLogoTasks() != null) {
                    LogoGeneratorTool logoTool = SpringContextUtil.getBean(LogoGeneratorTool.class);
                    for (ImageCollectionPlan.LogoTask task : plan.getLogoTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                logoTool.generateLogos(task.description())));
                    }
                }

                // 等待所有任务完成并收集结果
                //CompletableFuture<Void>，它的完成并不携带任何计算结果（所有子任务的结果仍在各自的 future 中），仅作为一个信号——“全部完成”。
                //CompletableFuture.allOf(...)：返回一个新的 CompletableFuture，
                //当 所有传入的 future 都完成时（无论成功还是异常），它自身才算完成。
                //futures.toArray(new CompletableFuture[0])
                //将 List<CompletableFuture<List<ImageResource>>> 转换为 CompletableFuture[]。
                CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0]));
                //阻塞当前线程，直到所有异步任务执行完毕。
                //如果 join() 抛出了异常（因为某个子任务异常），控制流直接跳到外层 catch，不会执行后续的 for 循环.
                allTasks.join();
                // 收集所有结果
                for (CompletableFuture<List<ImageResource>> future : futures) {
                    List<ImageResource> images = future.get();
                    if (images != null) {
                        collectedImages.addAll(images);
                    }
                }
                log.info("并发图片收集完成，共收集到 {} 张图片", collectedImages.size());
            } catch (Exception e) {
                log.error("图片收集失败: {}", e.getMessage(), e);
            }
                // 2. 在 finally 中停止并记录，确保异常时也能输出耗时
                //stopWatch.stop();
                //log.info("图片收集总耗时: {} ms", stopWatch.getTotalTimeMillis());
            // 更新状态
            context.setCurrentStep("图片收集");
            context.setImageList(collectedImages);
            return WorkflowContext.saveContext(context);
        });
    }
}



