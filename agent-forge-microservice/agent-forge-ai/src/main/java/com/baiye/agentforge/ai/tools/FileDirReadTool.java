package com.baiye.agentforge.ai.tools;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baiye.agentforge.constant.AppConstant;
import com.baiye.agentforge.utils.DirectoryTreePrinterUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * ClassName: FileDirReadTool
 * Package: com.baiye.agentforge.ai.tools
 * Description: 文件目录读取工具,使用 Hutool 简化文件操作
 *
 * @Author 白夜
 * @Create 2026/5/22 15:15
 * @Version 1.0
 */
@Slf4j
@Component
public class FileDirReadTool extends BaseTool {

    /**
     * 需要忽略的文件和目录
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store",
            ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );

    /**
     * 需要忽略的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log", ".tmp", ".cache", ".lock"
    );

    @Tool("读取目录结构，获取指定目录下的所有文件和子目录信息")
    public String readDir(
            @P("目录的相对路径，为空则读取整个项目结构")
            String relativeDirPath,
            @ToolMemoryId Long appId
    ) {
        try {
            //relativeDirPath 可能为 null，此时视为空字符串（代表根目录）。
            Path path = Paths.get(relativeDirPath == null ? "" : relativeDirPath);
            if (!path.isAbsolute()) { // 如果路径不是绝对路径
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeDirPath == null ? "" : relativeDirPath);
            }
            File targetDir = path.toFile();//将路径转换为文件对象
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                return "错误：目录不存在或不是目录 - " + relativeDirPath;
            }
            StringBuilder structure = new StringBuilder();
            structure.append("项目目录结构:\n");
            //传入目标目录路径和一个文件过滤器
            String directoryTree = DirectoryTreePrinterUtils.directoryTree(targetDir.getPath(), file -> !shouldIgnore(file.getName()));
            structure.append(directoryTree);
            if (!directoryTree.endsWith("\n")) { //判断最后一个字符是不是换行。
                structure.append("\n");
            }

            return structure.toString();
            // FileUtil.loopFiles 是 Hutool 的文件工具方法，递归遍历目标目录及其子目录。
            //第二个参数是一个 FileFilter（lambda 形式）：决定是否将当前遍历到的文件/目录加入结果列表。
            //如果 shouldIgnore 返回 true → !true = false → 过滤器返回 false → 该文件/目录被排除。
            //如果 shouldIgnore 返回 false → !false = true → 过滤器返回 true → 该文件/目录被保留。
            // 也就是说，只收集不需要忽略的文件。
            //List<File> allFiles = FileUtil.loopFiles(targetDir, file -> !shouldIgnore(file.getName()));
            // 按路径深度和名称排序显示,按文件相对于根目录的深度升序排列（浅层的在前），深度相同时按完整路径的字典序排序。
            //allFiles.stream()
            //        .sorted((f1, f2) -> {  //对流中的元素进行排序
            //            int depth1 = getRelativeDepth(targetDir, f1);
            //            int depth2 = getRelativeDepth(targetDir, f2);
            //            if (depth1 != depth2) { // 深度不同，按深度升序排列
            //                //Integer.compare(a, b) 返回：
            //                //负数 如果 a < b → depth1 小于 depth2 时，f1 排在 f2 前面。
            //                //0 如果相等。
            //                //正数 如果 a > b。
            //                //这意味着浅层文件排在前，深层文件排在后。
            //                return Integer.compare(depth1, depth2);
            //            }
            //            //当深度相同时，调用 File.getPath() 获取文件的完整路径字符串，然后使用 String.compareTo 进行字典序比较。
            //            //即字母顺序（A 在 B 前，小写 a 在大写 Z 后等）,这样可以保证同一目录下的文件按名称排序。
            //            return f1.getPath().compareTo(f2.getPath());
            //        })
            //        .forEach(file -> { //终止操作,遍历所有文件
            //            int depth = getRelativeDepth(targetDir, file);
            //            //" " 是两个空格字符的字符串。String.repeat(int count) 会将字符串重复指定次数。
            //            //depth = 0 → indent = ""（空字符串），无缩进。
            //            //depth = 1 → indent = " "，两个空格。
            //            //depth = 2 → indent = " "，四个空格。
            //            String indent = "  ".repeat(depth);
            //            //先追加缩进，再追加文件名（通过 file.getName() 获取，不包含路径部分）。
            //            //例如一个深度为 1 的文件 b.txt，拼接结果为:  b.txt（前面两个空格）。
            //            structure.append(indent).append(file.getName());
            //        });
        } catch (Exception e) {
            String errorMessage = "读取目录结构失败: " + relativeDirPath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 计算文件相对于根目录的深度
     * 根目录下的文件深度为 0，子目录中的文件深度 >= 1。
     */
    private int getRelativeDepth(File root, File file) {
        Path rootPath = root.toPath();
        Path filePath = file.toPath();
        //rootPath.relativize(filePath) 获取从根目录到文件的相对路径。
        //getNameCount() 返回相对路径中名称元素的数量（即路径深度，根目录深度为 0 时返回 0 或 1，但这里 -1 确保根目录文件深度为 0）。
        //例：根目录下的 a.txt 相对路径是 a.txt，getNameCount=1，减1后深度为0；sub/b.txt 深度为 1。
        return rootPath.relativize(filePath).getNameCount() - 1;
    }

    /**
     * 判断是否应该忽略该文件或目录
     * 只要满足任一条件就返回 true（忽略该文件）。
     */
    private boolean shouldIgnore(String fileName) {
        // 检查是否在忽略名称列表中
        if (IGNORED_NAMES.contains(fileName)) {
            return true;
        }

        // 检查文件扩展名,检查文件名是否以忽略扩展名结尾（如 .log），符合则忽略。
        return IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    @Override
    public String getToolName() {
        return "readDir";
    }

    @Override
    public String getDisplayName() {
        return "读取目录";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeDirPath = arguments.getStr("relativeDirPath");
        //如果该字段为空，就将目录名默认显示为 "根目录"，防止生成一条不完整的回执。
        if (StrUtil.isEmpty(relativeDirPath)) {
            relativeDirPath = "根目录";
        }
        return String.format("[工具调用] %s %s", getDisplayName(), relativeDirPath);
    }
}

