package com.baiye.agentforge.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * ClassName: MultiFileCodeResult
 * Package: com.baiye.agentforge.ai.model
 * Description: 多文件模式结果类
 *
 * @Author 白夜
 * @Create 2026/5/6 14:30
 * @Version 1.0
 */
@Description("生成多个代码文件的结果")
@Data
public class MultiFileCodeResult {

    @Description("HTML代码")
    private String htmlCode;

    @Description("CSS代码")
    private String cssCode;

    @Description("JS代码")
    private String jsCode;

    @Description("生成代码的描述")
    private String description;
}


