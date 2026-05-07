package com.baiye.agentforge.parser;

import com.baiye.agentforge.exception.BusinessException;
import com.baiye.agentforge.exception.ErrorCode;
import com.baiye.agentforge.model.enums.CodeGenTypeEnum;

/**
 * ClassName: CodeParserExecutor
 * Package: com.baiye.agentforge.parser
 * Description: 代码解析执行器,根据代码生成类型执行相应的解析逻辑（策略模式）
 *
 * @Author 白夜
 * @Create 2026/5/6 15:35
 * @Version 1.0
 */
public class CodeParserExecutor {

    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();

    private static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();

    /**
     * 执行代码解析
     *
     * @param codeContent 代码内容
     * @param codeGenType 代码生成类型
     * @return 解析结果（HtmlCodeResult 或 MultiFileCodeResult）
     */
    public static Object executeParser(String codeContent, CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case HTML -> htmlCodeParser.parseCode(codeContent);
            case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType);
        };
    }
}

