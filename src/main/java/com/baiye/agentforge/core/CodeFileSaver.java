package com.baiye.agentforge.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baiye.agentforge.ai.model.HtmlCodeResult;
import com.baiye.agentforge.ai.model.MultiFileCodeResult;
import com.baiye.agentforge.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * ClassName: CodeFileSaver
 * Package: com.baiye.agentforge.core
 * Description: 代码文件保存器
 *
 * @Author 白夜
 * @Create 2026/5/6 14:46
 * @Version 1.0
 */
public class CodeFileSaver {

    // 文件保存根目录,System.getProperty("user.dir")：获取 JVM 启动时的工作目录（通常是项目根目录）。
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 保存 HtmlCodeResult
     */
    public static File saveHtmlCodeResult(HtmlCodeResult result) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        return new File(baseDirPath);
    }

    /**
     * 保存 MultiFileCodeResult
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult result) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
        return new File(baseDirPath);
    }

    /**
     * 构建唯一目录路径：tmp/code_output/bizType_雪花ID
     */
    private static String buildUniqueDir(String bizType) {
        //IdUtil.getSnowflakeNextIdStr()
        //获取一个雪花算法（Snowflake） 生成的全局唯一 ID 的字符串形式。
        String uniqueDirName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        //File.separator：跨平台路径分隔符（Windows 是 \，Linux/macOS 是 /），保证可移植性。
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 写入单个文件
     */
    private static void writeToFile(String dirPath, String filename, String content) {
        String filePath = dirPath + File.separator + filename;
        //FileUtil.writeString 来自 Hutool 工具库，会覆盖已存在的文件，并自动创建父目录（此处父目录已提前创建）。
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }
}

