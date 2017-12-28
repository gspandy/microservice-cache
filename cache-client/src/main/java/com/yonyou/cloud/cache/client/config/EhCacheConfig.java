package com.yonyou.cloud.cache.client.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Nicky_chin  --Created on 2017/11/14
 */
@Configuration
@EnableCaching
public class EhCacheConfig {
	@Value("${ehRedisCache.ehcache.timeToLiveSeconds:0}")
	private Long timeToLiveSeconds;//ehcache元素过期的访问间隔(秒为单位). 0表示可以永远空闲,默认为0
	
	@Value("${ehRedisCache.ehcache.timeToLiveSeconds:0}")
	private Long timeToIdleSeconds; //ehcache元素在缓存里存在的时间(秒为单位). 0 表示永远存在不过期,默认为0
	
	@Value("${ehRedisCache.ehcache.memoryStoreEvictionPolicy:LRU}")
	private String memoryStoreEvictionPolicy;//当达到maxElementsInMemory限制时，Ehcache将会根据指定的策略去清理内存。可选策略有：LRU（最近最少使用，默认策略）、FIFO（先进先出）、LFU（最少访问次数）。
	
	@Bean(name = "ehcacheManager")
	public EhCacheManagerFactoryBean getEhCacheBean() {
		EhCacheManagerFactoryBean cacheManagerFactoryBean = new EhCacheManagerFactoryBean();
//		cacheManagerFactoryBean.setConfigLocation(new ClassPathResource("ehcache.xml"));
		//<!--true:单例，一个cacheManager对象共享；false：多个对象独立  -->
		cacheManagerFactoryBean.setShared(true);
		cacheManagerFactoryBean.setCacheManagerName("ehcacheManager");
		return  cacheManagerFactoryBean;
	}
	
	/*
	 * ehcache 主要的管理器
	 */
	//@Primary
	@Bean(name = "appEhCacheCacheManager")
	public EhCacheCacheManager ehCacheCacheManager(@Qualifier(value = "ehcacheManager") EhCacheManagerFactoryBean bean){
		return new EhCacheCacheManager (bean.getObject());
	}

    /*
    * ehCache 操作对象
    */
    @Bean(name = "ehCache")
    public EhCacheFactoryBean ehCacheCacheManagerBean(
            @Qualifier(value = "appEhCacheCacheManager") EhCacheCacheManager ehCacheCacheManager){
        EhCacheFactoryBean ehCacheFactoryBean = new EhCacheFactoryBean();
//        ehCacheFactoryBean.setCacheName("ehCache");
		ehCacheFactoryBean.setTimeToLiveSeconds(timeToLiveSeconds);
		ehCacheFactoryBean.setTimeToIdleSeconds(timeToIdleSeconds);
		ehCacheFactoryBean.setMemoryStoreEvictionPolicy(memoryStoreEvictionPolicy);
        ehCacheFactoryBean.setCacheManager(ehCacheCacheManager.getCacheManager());
        return ehCacheFactoryBean;
    }

}
