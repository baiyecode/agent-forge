package com.baiye.agentforge.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;


/**
 * ClassName: StaticResourceController
 * Package: com.baiye.agentforge.controller
 * Description: 静态资源服务接口
 *
 * @Author 白夜
 * @Create 2026/5/9 15:12
 * @Version 1.0
 */
@RestController
@RequestMapping("/static")
public class StaticResourceController {

    // 应用生成根目录（用于浏览）
    private static final String PREVIEW_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 提供静态资源访问，支持目录重定向
     * 访问格式：http://localhost:8123/api/static/{deployKey}[/{fileName}]
     */
    @GetMapping("/{deployKey}/**")
    public ResponseEntity<Resource> serveStaticResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        try {
            // 获取资源路径，
            // HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE 是 Spring MVC 在内部存储的“实际匹配的路径”。
            // 举例：如果请求是 /api/static/abc123/css/style.css，那么 resourcePath 就是 /abc123/css/style.css。
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            //截掉前缀，只保留文件部分。假设 deployKey 是 abc123，会被截成 /css/style.css。
            resourcePath = resourcePath.substring(("/static/" + deployKey).length());
            // 如果是目录访问（不带斜杠），重定向到带斜杠的URL
            /*  if (resourcePath.isEmpty())触发条件：
                resourcePath 是从 /static/{deployKey}/** 中提取的 除 deployKey 以外的剩余路径部分。
                当请求为 /api/static/abc123 时，resourcePath 会是 ""（空字符串）。
                当请求为 /api/static/abc123/ 时，resourcePath 会是 "/"。
                当请求为 /api/static/abc123/css/style.css 时，resourcePath 会是 "/css/style.css"。
                所以 resourcePath.isEmpty() 只有在“用户输入了部署键但不带任何斜杠和文件”时成立，也就是“想访问目录却忘了敲最后的斜杠”。
             */
            if (resourcePath.isEmpty()) {
                /*  request.getRequestURI() 会返回不包含域名和端口、但包含整个上下文路径和控制器映射路径的 URI。
                    例如对于 /api/static/abc123，它可能返回 /api/static/abc123。
                    在后面拼接 "/"，形成新 Location：/api/static/abc123/。
                    然后创建一个 ResponseEntity，将 headers 和 HTTP 状态码 301（MOVED_PERMANENTLY）返回。
                    浏览器收到 301 后会自动跟随重定向，立即发送第二个请求给 Location 里的新 URL（即带 / 的版本）。
                 */
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }
            /* 当用户访问 /api/static/abc123 时，先把他们重定向到 /api/static/abc123/，
                然后这个新请求会命中接口的第二段逻辑，最终返回 abc123/index.html 的内容。
             */
            // 默认返回 index.html
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }
            // 构建文件路径
            String filePath = PREVIEW_ROOT_DIR + "/" + deployKey + resourcePath;
            File file = new File(filePath);
            // 检查文件是否存在
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            // 返回文件资源
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(filePath))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     */
    private String getContentTypeWithCharset(String filePath) {
        if (filePath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filePath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg")) return "image/jpeg";
        return "application/octet-stream";// 对于所有你没手动处理的文件类型，统一当成未知二进制数据。
    }
}

