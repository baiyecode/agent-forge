package com.baiye.agentforge.ai.tools;


import cn.hutool.json.JSONObject;
import com.baiye.agentforge.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * ClassName: FileModifyTool
 * Package: com.baiye.agentforge.ai.tools
 * Description: 文件修改工具,支持 AI 通过工具调用的方式修改文件内容
 *
 * @Author 白夜
 * @Create 2026/5/22 16:20
 * @Version 1.0
 */
@Slf4j
@Component
public class FileModifyTool extends BaseTool {

    @Tool("修改文件内容，用新内容替换指定的旧内容")
    public String modifyFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要替换的旧内容")
            String oldContent,
            @P("替换后的新内容")
            String newContent,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return "错误：文件不存在或不是文件 - " + relativeFilePath;
            }
            String originalContent = Files.readString(path);//读取文件全部文本（默认使用 UTF-8 字符集）。
            if (!originalContent.contains(oldContent)) {
                return "警告：文件中未找到要替换的内容，文件未修改 - " + relativeFilePath;
            }
            //使用 String.replace(CharSequence, CharSequence)，它替换所有匹配的字面量，不涉及正则表达式。
            //String.replace 会替换所有出现的字面量。如果文件中 oldContent 出现多次，全部都会被替换。
            //这是一个纯字符串替换，所以 oldContent 中的特殊符号（如 $、\）会被当作普通字符处理，不会出现正则转义问题。
            String modifiedContent = originalContent.replace(oldContent, newContent);
            if (originalContent.equals(modifiedContent)) {
                return "信息：替换后文件内容未发生变化 - " + relativeFilePath;
            }
            //使用 writeString 将新内容写入文件（默认UTF-8 编码）。
            //StandardOpenOption.CREATE：如果文件意外不存在则创建（尽管前面已检查存在，这里增加一层容错）。
            //StandardOpenOption.TRUNCATE_EXISTING：如果文件已存在，先清空内容再写入。
            //两者结合就是覆盖写入。
            Files.writeString(path, modifiedContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功修改文件: {}", path.toAbsolutePath());//记录绝对路径方便排查。
            return "文件修改成功: " + relativeFilePath;//返回成功消息（只显示相对路径），避免暴露服务器目录结构。
        } catch (IOException e) {
            String errorMessage = "修改文件失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "modifyFile";
    }

    @Override
    public String getDisplayName() {
        return "修改文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String oldContent = arguments.getStr("oldContent");
        String newContent = arguments.getStr("newContent");
        // 显示对比内容
        return String.format("""
                [工具调用] %s %s
                
                替换前：
                ```
                %s
                ```
                
                替换后：
                ```
                %s
                ```
                """, getDisplayName(), relativeFilePath, oldContent, newContent);
    }
}

