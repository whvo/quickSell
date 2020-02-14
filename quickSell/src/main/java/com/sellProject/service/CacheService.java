package com.sellProject.service;

/**
 * @author whvo
 * @date 2019/11/7 0007 -15:35
 * 用来做本地热点缓存
 */
public interface CacheService {
    // 存
    void setCommonCache(String key, Object value);

    //取
    Object getCommonCache(String key);
}
