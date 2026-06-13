package com.baiye.agentforge.core.parser;

/**
 * ClassName: CodeParser
 * Package: com.baiye.agentforge.parser
 * Description: 代码解析器策略接口
 *
 * @Author 白夜
 * @Create 2026/5/6 15:19
 * @Version 1.0
 */
public interface CodeParser<T> {

    /**
     * 解析代码内容
     *
     * @param codeContent 原始代码内容
     * @return 解析后的结果对象
     */
    T parseCode(String codeContent);
}

