package com.baiye.agentforge.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * ClassName: HtmlCodeResult
 * Package: com.baiye.agentforge.ai.model
 * Description: html代码结果类
 *
 * @Author 白夜
 * @Create 2026/5/6 14:29
 * @Version 1.0
 */
@Description("生成 HTML 代码文件的结果")
@Data
public class HtmlCodeResult {

    @Description("HTML代码")
    private String htmlCode;

    @Description("生成代码的描述")
    private String description;
}


