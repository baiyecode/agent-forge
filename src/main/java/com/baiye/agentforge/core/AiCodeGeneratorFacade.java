package com.baiye.agentforge.core;

import com.baiye.agentforge.ai.AiCodeGeneratorService;
import com.baiye.agentforge.ai.model.HtmlCodeResult;
import com.baiye.agentforge.ai.model.MultiFileCodeResult;
import com.baiye.agentforge.exception.BusinessException;
import com.baiye.agentforge.exception.ErrorCode;
import com.baiye.agentforge.model.enums.CodeGenTypeEnum;
import com.baiye.agentforge.parser.CodeParserExecutor;
import com.baiye.agentforge.saver.CodeFileSaverExecutor;
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



    /**
     * 生成 HTML 模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveHtmlCode(String userMessage) {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
        return CodeFileSaver.saveHtmlCodeResult(result);
    }

    /**
     * 生成多文件模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveMultiFileCode(String userMessage) {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
        return CodeFileSaver.saveMultiFileCodeResult(result);
    }

    /**
     * 统一入口：根据类型生成并保存代码(使用appId)
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum,Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML,appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE,appId);
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
     * @param appId  应用 ID
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType,Long appId) {
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
                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType,appId);
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
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML,appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE,appId);
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


}

