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

/**
 * ClassName: FileDeleteTool
 * Package: com.baiye.agentforge.ai.tools
 * Description: 文件删除工具，支持 AI 通过工具调用的方式删除文件
 *
 * @Author 白夜
 * @Create 2026/5/21 20:08
 * @Version 1.0
 */
@Slf4j
@Component
public class FileDeleteTool extends BaseTool {

    @Tool("删除指定路径的文件")   //标记该方法为一个可供 AI 调用的工具，括号中的字符串是对这个工具功能的描述。
    public String deleteFile(
            @P("文件的相对路径")  //描述工具方法的参数
            String relativeFilePath,
            @ToolMemoryId Long appId  //标识“会话”或“上下文”相关的 ID,会自动传入当前会话绑定的 appId，用于隔离不同用户/项目的文件空间。
    ) {
        try {
            Path path = Paths.get(relativeFilePath);//将传入的字符串 relativeFilePath 转为 Path 对象。
            if (!path.isAbsolute()) { //如果路径不是绝对路径
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);//将相对路径转换为绝对路径
            }
            if (!Files.exists(path)) {
                return "警告：文件不存在，无需删除 - " + relativeFilePath;
            }
            if (!Files.isRegularFile(path)) {
                return "错误：指定路径不是文件，无法删除 - " + relativeFilePath;
            }
            // 安全检查：避免删除重要文件
            String fileName = path.getFileName().toString();
            if (isImportantFile(fileName)) {
                return "错误：不允许删除重要文件 - " + fileName;
            }
            Files.delete(path);
            log.info("成功删除文件: {}", path.toAbsolutePath());//输出被删除文件的完整路径，无论传入的 path 是相对路径还是绝对路径
            return "文件删除成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "删除文件失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 判断是否是重要文件，不允许删除
     */
    private boolean isImportantFile(String fileName) {
        String[] importantFiles = {
                "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
                "vite.config.js", "vite.config.ts", "vue.config.js",
                "tsconfig.json", "tsconfig.app.json", "tsconfig.node.json",
                "index.html", "main.js", "main.ts", "App.vue", ".gitignore", "README.md"
        };
        for (String important : importantFiles) {
            if (important.equalsIgnoreCase(fileName)) { //忽略大小写（例如 Windows 系统文件名不区分大小写，但此处统一忽略以防万一）。
                return true;
            }
        }
        return false;
    }

    @Override
    public String getToolName() {
        return "deleteFile";
    }

    @Override
    public String getDisplayName() {
        return "删除文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        return String.format("[工具调用] %s %s", getDisplayName(), relativeFilePath);
    }
}

