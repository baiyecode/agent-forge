package com.baiye.agentforge.core;

import cn.hutool.json.JSONUtil;
import com.baiye.agentforge.ai.AiCodeGeneratorService;
import com.baiye.agentforge.ai.AiCodeGeneratorServiceFactory;
import com.baiye.agentforge.ai.model.HtmlCodeResult;
import com.baiye.agentforge.ai.model.MultiFileCodeResult;
import com.baiye.agentforge.ai.model.message.AiResponseMessage;
import com.baiye.agentforge.ai.model.message.ToolExecutedMessage;
import com.baiye.agentforge.ai.model.message.ToolRequestMessage;
import com.baiye.agentforge.exception.BusinessException;
import com.baiye.agentforge.exception.ErrorCode;
import com.baiye.agentforge.model.enums.CodeGenTypeEnum;
import com.baiye.agentforge.parser.CodeParserExecutor;
import com.baiye.agentforge.saver.CodeFileSaverExecutor;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * ClassName: AiCodeGeneratorFacade
 * Package: com.baiye.agentforge.core
 * Description: AI 代码生成外观类，组合生成和保存功能(门面模式）
 *
 * @Author 白夜
 * @Create 2026/5/6 14:56
 * @Version 1.0
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;


    /**
     * 统一入口：根据类型生成并保存代码(使用appId)
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 通用流式代码处理方法
     * AI 生成服务返回一个 Flux<String>，这只是一种 构建好的流水线声明，尚未开始执行。
     * 这个 Flux<String> 表示：当有人订阅它时，会以 流式方式 逐块（chunk）输出代码片段，每块是一个 String。
     * 例如 AI 依次返回 "<html>"、"<body>"、"<p>hello</p>"、"</body>"、"</html>" 五个字符串片段。
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();// 用于收集代码片段
        return codeStream.doOnNext(chunk -> {
            // 每次收到一个 chunk(AI 每次返回的代码片段字符串)，就把它追加到 codeBuilder 中
            codeBuilder.append(chunk);
        }).doOnComplete(() -> { //流结束后统一处理
            // 流式返回完成后保存代码
            try {
                String completeCode = codeBuilder.toString();
                //策略分发执行器，根据 codeGenType 选择并调用对应的 CodeParser 解析方法
                // 使用执行器解析代码
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                // 使用执行器保存代码
                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式,使用appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * yield 的含义
     * 在传统 switch 语句中，break 用于跳出；但 switch 表达式必须产生一个值。
     * 在箭头后面如果是一个代码块 { }，就需要用 yield 来指定该分支的返回值。
     * yield processCodeStream(...) 表示：将 processCodeStream 返回的 Flux<String> 作为这个 case 的结果。
     * 如果箭头后面只有一个表达式（没有大括号），可以省略 yield，例如：case SOME -> someMethod();，
     * 但这里因为要先声明局部变量 codeStream，所以必须用块 + yield。
     */


    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * 将 LLM 流式回调事件转化为响应式 Flux 流，并以统一 JSON 格式向外传递，
     * 使得基于 WebFlux 的应用可以原生地、高效地将 AI 生成内容实时推送至前端。
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
                        //部分响应（Token 流文本）
            tokenStream.onPartialResponse((String partialResponse) -> { //每次回调拿到的不是“刚生的词”，而是从对话开始到现在已经生成的全部字符的集合。
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        //sink.next() 将 JSON 字符串推入 Flux，下游消费者（如 HTTP 客户端）将接收到一条 SSE 格式的数据。
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    //工具调用请求（模型决定调用工具）
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));//立即推送 JSON 给前端，前端可以显示“正在执行工具...”。
                    })
                    //工具执行完成,ToolExecution：包含请求信息 + 执行结果（成功或错误消息）。
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    //整个对话完成
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    //流过程中发生错误
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    // 启动流式处理
                    .start();
        });
    }



}

