package com.baiye.agentforge.ai.tools;

/**
 * ClassName: FileWriteTool
 * Package: com.baiye.agentforge.ai.tools
 * Description:
 *
 * @Author 白夜
 * @Create 2026/5/17 13:16
 * @Version 1.0
 */

import com.baiye.agentforge.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 */
@Slf4j
public class FileWriteTool {

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeFilePath);//将路径字符串（或字符串片段）转换为 Path 对象。
            //path.isAbsolute() 返回 true 表示该路径是绝对路径
            if (!path.isAbsolute()) { //判断是否为绝对路径
                // 相对路径处理，创建基于 appId 的项目目录
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                //resolve(String other)：将当前路径与另一个路径字符串进行“解析合并”。
                //如果 other 是绝对路径，resolve 会直接返回 other。
                //如果 other 是相对路径，则返回 projectRoot 加上 other 后的路径。
                //例子：
                //projectRoot = tmp/code_output/vue_project_123
                //relativeFilePath = "src/components/Header.vue"
                //resolve 结果：tmp/code_output/vue_project_123/src/components/Header.vue
                path = projectRoot.resolve(relativeFilePath);
            }
            // 创建父目录（如果不存在）
            Path parentDir = path.getParent();//获取要写入文件的父目录路径。
            if (parentDir != null) {
                Files.createDirectories(parentDir);//自动创建所有缺失的父目录
            }
            // 写入文件内容
            //StandardOpenOption.CREATE：若文件不存在则创建。
            //StandardOpenOption.TRUNCATE_EXISTING：若文件已存在，则清空其内容再写入（相当于覆盖）。
            Files.write(path, content.getBytes(),//将 content 字符串转换为字节数组，使用 UTF-8 编码
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            //Path toAbsolutePath(),将相对路径转换为绝对路径
            //如果原路径已经是绝对路径，则直接返回等价路径；如果是相对路径，则基于当前工作目录解析出绝对路径。
            log.info("成功写入文件: {}", path.toAbsolutePath());
            // 注意要返回相对路径，不能让 AI 把文件绝对路径返回给用户
            return "文件写入成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
}

