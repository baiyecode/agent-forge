package com.baiye.agentforge.utils;


import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

/**
 * ClassName: CacheKeyUtils
 * Package: com.baiye.agentforge.utils
 * Description: 缓存 key 生成工具类
 *
 * @Author 白夜
 * @Create 2026/6/2 19:54
 * @Version 1.0
 */
public class CacheKeyUtils {

    /**
     * 根据对象生成缓存key (JSON + MD5)
     * 缓存键的生成思路是将复杂的对象转换为固定长度的哈希值，这样既保证了不同查询请求的key唯一，又避免了key过长的问题
     *
     * @param obj 要生成key的对象
     * @return MD5哈希后的缓存key
     */
    public static String generateKey(Object obj) {
        if (obj == null) {
            return DigestUtil.md5Hex("null");
        }
        // 先转JSON，再MD5
        String jsonStr = JSONUtil.toJsonStr(obj);
        return DigestUtil.md5Hex(jsonStr);
    }
}

