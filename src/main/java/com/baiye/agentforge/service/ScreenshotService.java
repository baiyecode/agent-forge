package com.baiye.agentforge.service;

/**
 * ClassName: ScreenshotService
 * Package: com.baiye.agentforge.service
 * Description: 截图服务
 *
 * @Author 白夜
 * @Create 2026/5/19 17:01
 * @Version 1.0
 */
public interface ScreenshotService {


    /**
     * 生成并上传截图服务
     * @param webUrl
     * @return
     */
    String generateAndUploadScreenshot(String webUrl);
}
