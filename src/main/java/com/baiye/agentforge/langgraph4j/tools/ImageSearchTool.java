package com.baiye.agentforge.langgraph4j.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baiye.agentforge.langgraph4j.model.ImageResource;
import com.baiye.agentforge.langgraph4j.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: ImageSearchTool
 * Package: com.baiye.agentforge.langgraph4j.tools
 * Description: 内容图片收集工具
 *
 * @Author 白夜
 * @Create 2026/5/25 14:49
 * @Version 1.0
 */
@Slf4j
@Component
public class ImageSearchTool {

    private static final String PEXELS_API_URL = "https://api.pexels.com/v1/search";

    @Value("${pexels.api-key}")
    private String pexelsApiKey;

    @Tool("搜索内容相关的图片，用于网站内容展示")
    public List<ImageResource> searchContentImages(@P("搜索关键词") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        int searchCount = 12;
        // 调用 API，注意释放资源
        try (HttpResponse response = HttpRequest.get(PEXELS_API_URL)
                .header("Authorization", pexelsApiKey)
                .form("query", query) // 搜索关键词
                .form("per_page", searchCount) // 每页返回数量
                .form("page", 1) // 第几页
                .execute()) {
            if (response.isOk()) {
                JSONObject result = JSONUtil.parseObj(response.body());//将响应体字符串解析为 JSONObject。
                JSONArray photos = result.getJSONArray("photos");//遍历数组，获取每个 photo 对象。
                for (int i = 0; i < photos.size(); i++) {
                    JSONObject photo = photos.getJSONObject(i);
                    JSONObject src = photo.getJSONObject("src");//从 photo 中拿到 src 对象
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.CONTENT)
                            //获取 alt 字段（图片替代文本），如果该字段不存在或为空，则用搜索关键词 query 作为默认值。
                            .description(photo.getStr("alt", query))
                            .url(src.getStr("medium")) //取出 medium 字段作为图片 URL（中等尺寸，适合网页展示）
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Pexels API 调用失败: {}", e.getMessage(), e);
        }
        return imageList;
    }
}

