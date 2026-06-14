package com.baiye.agentforge.service.impl;

import com.baiye.agentforge.innerservice.InnerScreenshotService;
import com.baiye.agentforge.service.ScreenshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * ClassName: InnerScreenshotServiceImpl
 * Package: com.baiye.agentforge.service.impl
 * Description:
 *
 * @Author 白夜
 * @Create 2026/6/14 15:54
 * @Version 1.0
 */
@DubboService
@Slf4j
public class InnerScreenshotServiceImpl implements InnerScreenshotService {

    @Resource
    private ScreenshotService screenshotService;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        return screenshotService.generateAndUploadScreenshot(webUrl);
    }
}

