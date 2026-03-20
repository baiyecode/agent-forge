package com.baiye.agentforge.common;

import lombok.Data;

/**
 * ClassName: PageRequest
 * Package: com.baiye.agentforge.common
 * Description: 分页请求包装类
 *
 * @Author 白夜
 * @Create 2026/3/20 14:24
 * @Version 1.0
 */
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int pageNum = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";
}

