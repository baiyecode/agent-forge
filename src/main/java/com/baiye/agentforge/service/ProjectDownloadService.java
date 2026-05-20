package com.baiye.agentforge.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * ClassName: ProjectDownloadService
 * Package: com.baiye.agentforge.service
 * Description: 项目下载服务
 *
 * @Author 白夜
 * @Create 2026/5/20 11:22
 * @Version 1.0
 */
public interface ProjectDownloadService {


    /**
     * 下载项目为zip文件
     * @param projectPath
     * @param downloadFileName
     * @param response
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
