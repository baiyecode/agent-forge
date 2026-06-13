package com.baiye.agentforge.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.baiye.agentforge.exception.BusinessException;
import com.baiye.agentforge.exception.ErrorCode;
import com.baiye.agentforge.exception.ThrowUtils;
import com.baiye.agentforge.service.ProjectDownloadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

/**
 * ClassName: ProjectDownloadServiceImpl
 * Package: com.baiye.agentforge.service.impl
 * Description: 项目下载服务实现类
 *
 * @Author 白夜
 * @Create 2026/5/20 11:24
 * @Version 1.0
 */
@Service
@Slf4j
public class ProjectDownloadServiceImpl implements ProjectDownloadService {

    /**
     * 需要过滤的文件和目录名称
     * Set.of(...)
     * Java 9+ 提供的不可变集合工厂方法，生成一个包含指定元素的 Set。
     * 这里列出的都是项目开发中常见的、不需要下载的目录或文件名，
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules",//Node.js 依赖目录，体积大且可重新安装。
            ".git",// Git 版本控制目录，包含版本历史记录，不需要下载。
            "dist",// 构建输出目录，包含编译后的代码，不需要下载。
            "build",// 构建输出目录，包含编译后的代码，不需要下载。
            ".DS_Store",//macOS 系统生成的文件夹属性文件。
            ".env",// 环境变量文件，包含敏感信息，不需要下载。
            "target",// Maven 构建输出目录，包含编译后的代码，不需要下载。
            ".mvn",// Maven 配置目录，包含 Maven 的配置文件，不需要下载。
            ".idea",// IntelliJ IDEA 配置目录，包含 IDE 的配置文件，不需要下载。
            ".vscode"// Visual Studio Code 配置目录，包含 IDE 的配置文件，不需要下载。
    );

    /**
     * 需要过滤的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log",
            ".tmp",
            ".cache"
    );

    /**
     * 检查路径是否允许包含在压缩包中
     *
     * @param projectRoot 项目根目录
     * @param fullPath    完整路径
     * @return 是否允许
     */
    private boolean isPathAllowed(Path projectRoot, Path fullPath) {
        // 获取相对路径
        Path relativePath = projectRoot.relativize(fullPath);
        // 检查路径中的每一部分
        for (Path part : relativePath) {
            String partName = part.toString();
            // 检查是否在忽略名称列表中
            if (IGNORED_NAMES.contains(partName)) {
                return false;
            }
            // 检查文件扩展名,使用 anyMatch 判断当前路径部分的字符串是否以任何一个忽略扩展名结尾。
            if (IGNORED_EXTENSIONS.stream().anyMatch(partName::endsWith)) {
                return false;
            }
        }
        return true;//该文件/目录是“安全”的，允许包含在下载包中。
    }


    /**
     * 下载项目为 zip 压缩包
     * void：不返回数据，直接通过 response 输出二进制流。
     *
     * @param projectPath     项目路径
     * @param downloadFileName 下载文件名
     * @param response        响应对象
     */
    @Override
    public void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response) {
        // 基础校验
        ThrowUtils.throwIf(StrUtil.isBlank(projectPath), ErrorCode.PARAMS_ERROR, "项目路径不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(downloadFileName), ErrorCode.PARAMS_ERROR, "下载文件名不能为空");
        File projectDir = new File(projectPath);
        ThrowUtils.throwIf(!projectDir.exists(), ErrorCode.NOT_FOUND_ERROR, "项目目录不存在");
        ThrowUtils.throwIf(!projectDir.isDirectory(), ErrorCode.PARAMS_ERROR, "指定路径不是目录");
        log.info("开始打包下载项目: {} -> {}.zip", projectPath, downloadFileName);
        // 设置 HTTP 响应头,告诉浏览器“这是一个需要下载的 ZIP 文件”，而非尝试在页面中直接渲染。
        response.setStatus(HttpServletResponse.SC_OK);// 设置 HTTP 状态码为 200 (OK)
        response.setContentType("application/zip");// 设置内容类型为 "application/zip"，告诉浏览器这是一个 ZIP 文件。
        //Content-Disposition 是一个 HTTP 响应头，用于指示浏览器如何处理响应中的内容。
        //值格式为 attachment; filename="xxx.zip"，其中 attachment 告诉浏览器以附件形式下载。
        //filename 参数中拼接了 ${downloadFileName}.zip，并加了双引号，防止中文或特殊字符引起的问题。
        //使用 addHeader 而非 setHeader 可以避免覆盖其他同名头（不过这里通常只设置一次）。
        response.addHeader("Content-Disposition",
                String.format("attachment; filename=\"%s.zip\"", downloadFileName));
        // 定义文件过滤器,创建一个过滤器，用于控制 ZIP 中包含哪些文件或目录。
        //file → 压缩时遍历到的每个项目文件/子目录。
        FileFilter filter = file -> isPathAllowed(projectDir.toPath(), file.toPath());
        try {
            // 使用 Hutool 的 ZipUtil 直接将过滤后的目录压缩到响应输出流
            // response.getOutputStream()： 直接获取 Servlet 的输出流。
            // StandardCharsets.UTF_8：指定字符集为 UTF-8,避免乱码。
            //false（withDir = false）：表示在 ZIP 根目录下直接存放文件，不会额外创建一层以 projectDir 名称命名的文件夹。
            //假设 projectDir 是 /home/app/projectA，打包后 ZIP 内直接就是 src/、pom.xml 等，而不是 projectA/src/……。
            //filter：文件过滤器，决定哪些文件会被包含在 ZIP 中。
            //projectDir：要压缩的目录。
            ZipUtil.zip(response.getOutputStream(), StandardCharsets.UTF_8, false, filter, projectDir);
            log.info("项目打包下载完成: {}", downloadFileName);
        } catch (Exception e) {
            log.error("项目打包下载异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "项目打包下载失败");
        }
    }

}

