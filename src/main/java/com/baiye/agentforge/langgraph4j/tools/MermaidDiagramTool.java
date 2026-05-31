package com.baiye.agentforge.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.baiye.agentforge.exception.BusinessException;
import com.baiye.agentforge.exception.ErrorCode;
import com.baiye.agentforge.langgraph4j.model.ImageResource;
import com.baiye.agentforge.langgraph4j.model.enums.ImageCategoryEnum;
import com.baiye.agentforge.manager.CosManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ClassName: MermaidDiagramTool
 * Package: com.baiye.agentforge.langgraph4j.tools
 * Description: 架构图绘制工具，mermaid-cli + COS
 * 把 Mermaid 文本代码转换成 SVG 架构图，上传到云存储（COS），并返回一个图片资源列表。
 *
 * @Author 白夜
 * @Create 2026/5/25 16:53
 * @Version 1.0
 */
@Slf4j
@Component
public class MermaidDiagramTool {

    @Resource
    private CosManager cosManager;

    @Tool("将 Mermaid 代码转换为架构图图片，用于展示系统结构和技术关系")
    public List<ImageResource> generateMermaidDiagram(@P("Mermaid 图表代码") String mermaidCode,
                                                      @P("架构图描述") String description) {
        if (StrUtil.isBlank(mermaidCode)) {
            return new ArrayList<>();
        }
        try {
            // 转换为SVG图片
            File diagramFile = convertMermaidToSvgWithSpecifiedChromePath(mermaidCode);
            // 上传到COS
            String keyName = String.format("/mermaid/%s/%s",
                    RandomUtil.randomString(5), diagramFile.getName());
            String cosUrl = cosManager.uploadFile(keyName, diagramFile);
            // 清理临时文件
            //删除生成的 SVG 临时文件。注意这里只删除了 diagramFile（输出文件），而输入临时文件在 convertMermaidToSvg 内部已清理。
            FileUtil.del(diagramFile);
            if (StrUtil.isNotBlank(cosUrl)) {
                return Collections.singletonList(ImageResource.builder()
                        .category(ImageCategoryEnum.ARCHITECTURE)
                        .description(description)
                        .url(cosUrl)
                        .build());
            }
        } catch (Exception e) {
            log.error("生成架构图失败: {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * 将Mermaid代码转换为SVG图片
     * 该方法没有显式指定 Chrome 浏览器的路径，依赖系统环境变量中找到 Chrome。
     * 如果服务器上 Chrome 未安装或路径未在 PATH 中，mmdc 会因找不到 Puppeteer 所需的浏览器而失败。
     */
    private File convertMermaidToSvg(String mermaidCode) {
        // 创建临时输入文件
        File tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
        FileUtil.writeUtf8String(mermaidCode, tempInputFile);//将 mermaidCode 以 UTF-8 编码写入，
        // 创建临时输出文件
        File tempOutputFile = FileUtil.createTempFile("mermaid_output_", ".svg", true);
        // 根据操作系统选择命令
        String command = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : "mmdc";
        // 构建命令
        //-i：指定输入文件路径。
        //-o：指定输出文件路径。
        //-b transparent：设置背景透明（适合架构图叠加展示）。
        String cmdLine = String.format("%s -i %s -o %s -b transparent",
                command,
                tempInputFile.getAbsolutePath(),//即之前写入的 .mmd 临时文件的绝对路径
                tempOutputFile.getAbsolutePath()//即之前创建的 .svg 临时文件的绝对路径
        );
        // 执行命令,Hutool 对 Runtime.exec 的封装，同步执行并返回输出字符串（本方法忽略返回值）。
        RuntimeUtil.execForStr(cmdLine);
        // 检查输出文件
        if (!tempOutputFile.exists() || tempOutputFile.length() == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI 执行失败");
        }
        // 清理输入文件，保留输出文件供上传使用
        FileUtil.del(tempInputFile);
        return tempOutputFile;
    }


    /**
     * 将Mermaid代码转换为SVG图片, 指定 chromePath 路径
     * 针对错误：
     * 2025-09-20 14:06:18.230 INFO 3672 --- [ main] c.a.i.l.tools.MermaidDiagramTool : Mermaid CLI 输出:
     * Error: Could not find expected browser (chrome) locally. Run `npm install` to download the correct Chromium revision (1045629).
     * at ChromeLauncher.launch (file:///G:/jetbrains_tools/frontend/Nvm/node_global/node_modules/@mermaid-js/mermaid-cli/node_modules/puppeteer-core/lib/esm/puppeteer/node/ChromeLauncher.js:64:23)
     * at async run (file:///G:/jetbrains_tools/frontend/Nvm/node_global/node_modules/@mermaid-js/mermaid-cli/src/index.js:343:19)
     * at async cli (file:///G:/jetbrains_tools/frontend/Nvm/node_global/node_modules/@mermaid-js/mermaid-cli/src/index.js:138:3)
     * 2025-09-20 14:06:18.235 ERROR 3672 --- [ main] c.a.i.l.tools.MermaidDiagramTool : 生成架构图失败: Mermaid CLI 执行失败
     * @param mermaidCode mermaid代码
     * @return SVG图片文件
     */
    private File convertMermaidToSvgWithSpecifiedChromePath(String mermaidCode) {
        File tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
        FileUtil.writeUtf8String(mermaidCode, tempInputFile);

        File tempOutputFile = FileUtil.createTempFile("mermaid_output_", ".svg", true);
        String command = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : "mmdc";
        String chromePath = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";

        try {
            // 用 ProcessBuilder 设置环境变量
            ProcessBuilder pb = new ProcessBuilder(
                    command,
                    "-i", tempInputFile.getAbsolutePath(),
                    "-o", tempOutputFile.getAbsolutePath(),
                    "-b", "transparent"
            );

            // 加环境变量 Puppeteer 执行路径
            pb.environment().put("PUPPETEER_EXECUTABLE_PATH", chromePath);

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出日志
            String result = IoUtil.read(process.getInputStream(), Charset.defaultCharset());
            log.info("Mermaid CLI 输出: {}", result);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI 执行失败，exit=" + exitCode);
            }

            if (!tempOutputFile.exists() || tempOutputFile.length() == 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI 没有生成输出文件");
            }
            return tempOutputFile;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成架构图失败: " + e.getMessage());
        } finally {
            FileUtil.del(tempInputFile);
        }

    }
}

