package com.baiye.agentforge.langgraph4j.model;


import com.baiye.agentforge.langgraph4j.model.enums.ImageCategoryEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * ClassName: ImageResource
 * Package: com.baiye.agentforge.langgraph4j.model
 * Description: 图片资源对象
 *
 * @Author 白夜
 * @Create 2026/5/24 16:11
 * @Version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResource implements Serializable {

    /**
     * 图片类别
     */
    private ImageCategoryEnum category;

    /**
     * 图片描述
     */
    private String description;

    /**
     * 图片地址
     */
    private String url;

    @Serial
    private static final long serialVersionUID = 1L;
}

