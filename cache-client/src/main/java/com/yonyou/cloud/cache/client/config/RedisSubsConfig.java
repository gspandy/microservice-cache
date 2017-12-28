package com.yonyou.cloud.cache.client.config;

import java.util.Collection;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import com.google.common.collect.Lists;
import com.yonyou.cloud.cache.client.cache.RedisMessageListener;

@Configuration
public class RedisSubsConfig {
	
	@Autowired
    private RedisTemplate<?, ?> redisTemplate;
	
	@Value("${ehRedisCache.redis.subscribe.keyPattern:*}")
	private String keyPattern;
	
	@Bean
	public MessageListenerAdapter messageListenerAdapter(@Qualifier(value = "ehCache") EhCacheFactoryBean ehCacheFactoryBean){
		RedisMessageListener redisMessageListener = new RedisMessageListener();
		redisMessageListener.setEhCache(ehCacheFactoryBean.getObject());
		redisMessageListener.setRedisTemplate(redisTemplate);
		MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(redisMessageListener);
		return messageListenerAdapter;
	}
	
	@Bean
	public RedisMessageListenerContainer redisContainer(RedisConnectionFactory factory, MessageListenerAdapter messageListenerAdapter){
		RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
		redisMessageListenerContainer.setConnectionFactory(factory);
		PatternTopic patternTopic = new PatternTopic("__keyspace@*__:" + keyPattern);//例：__keyspace@*__:user* 订阅所有db中user开头的key 注：__[keyspace|keyevent]@<db>__:[key|event]]
		HashMap<MessageListener, Collection<? extends Topic>> listeners = new HashMap<MessageListener, Collection<? extends Topic>>();
		listeners.put(messageListenerAdapter, Lists.newArrayList(patternTopic));
		redisMessageListenerContainer.setMessageListeners(listeners);
		
		return redisMessageListenerContainer;
	}
	
}
