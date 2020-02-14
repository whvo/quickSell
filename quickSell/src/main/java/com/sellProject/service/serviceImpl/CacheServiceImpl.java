package com.sellProject.service.serviceImpl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sellProject.service.CacheService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @author whvo
 * @date 2019/11/7 0007 -15:38
 *
 * 本地缓存， 原理类似hashMap，但是不同的是guava可以支持初始化并且可以设置失效时间以及过期策略
 */
@Service
public class CacheServiceImpl implements CacheService {
    private Cache<String ,Object> commonCache = null ;

    @PostConstruct // 该方法优先执行
    public  void init() {
        commonCache = CacheBuilder.newBuilder()
                // 设置初始缓存容量
                .initialCapacity(20)
                // 设置最大缓存容量
                .maximumSize(100)
                // 设置写之后的有效时间  缓存项在创建后，在给定时间内没有被写访问（创建或覆盖），则清除
                .expireAfterWrite(60, TimeUnit.SECONDS).build();
    }

    @Override
    public void setCommonCache(String key, Object value) {
        commonCache.put(key, value);
    }

    @Override
    public Object getCommonCache(String key) {
        return commonCache.getIfPresent(key);
    }
}
