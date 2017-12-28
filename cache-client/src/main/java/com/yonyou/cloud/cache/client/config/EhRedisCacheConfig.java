package com.yonyou.cloud.cache.client.config;

import java.lang.reflect.Method;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

import com.yonyou.cloud.cache.client.cache.EhRedisCache;

@Configuration
public class EhRedisCacheConfig extends CachingConfigurerSupport{
	
	@Autowired
    private RedisTemplate<?, ?> redisTemplate;
	
	@Value("${ehRedisCache.cacheName:ehRedisCache}")
	private String ehRedisCacheName; //缓存名称
	
	@Value("${ehRedisCache.activeCount:10}")
	private int activeCount;//一级缓存击穿值，当一级缓存命中数大于该值则强制访问二级缓存 ，并刷新一级缓存
	
	@Value("${ehRedisCache.redisCache.liveTime:0}")
	private Long liveTime;//二级缓存redis的过期时间，单位为秒。默认为 0，永不过期。
	
    @Bean(name = "ehRedisCache")
    public EhRedisCache getEhRedisCacheObj(@Qualifier(value = "ehCache") EhCacheFactoryBean ehCacheFactoryBean) {

        EhRedisCache cache = new EhRedisCache();
        cache.setEhCache(ehCacheFactoryBean.getObject());
        cache.setName(ehRedisCacheName);
        cache.setRedisTemplate(redisTemplate);
        cache.setActiveCount(activeCount);
        cache.setLiveTime(liveTime);
        return cache;

    }

    /**
     *指定ehcache的主管理器为ehRedisCache
     */
    @Primary
    @Bean(name = "ehRedisCacheManager")
    public SimpleCacheManager getSimpleCacheManagerObj(@Qualifier(value = "ehRedisCache") EhRedisCache cache) {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Collections.singletonList(cache));
        return manager;
    }
    
    /**
     * 生成key的策略
     *
     * @return
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                StringBuilder sb = new StringBuilder();
                sb.append(target.getClass().getName());
                sb.append(method.getName());
                for (Object obj : params) {
                    sb.append(obj.toString());
                }
                return sb.toString();
            }
        };
    }
}
