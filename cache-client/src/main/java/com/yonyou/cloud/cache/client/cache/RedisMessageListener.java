package com.yonyou.cloud.cache.client.cache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import net.sf.ehcache.Element;

public class RedisMessageListener implements MessageListener {
	
	private static final Logger log = LoggerFactory.getLogger(RedisMessageListener.class);
	
	private net.sf.ehcache.Ehcache ehCache;
	
	private RedisTemplate<?, ?> redisTemplate;
	
	public net.sf.ehcache.Ehcache getEhCache() {
		return ehCache;
	}

	public void setEhCache(net.sf.ehcache.Ehcache ehCache) {
		this.ehCache = ehCache;
	}

	public RedisTemplate<?, ?> getRedisTemplate() {
		return redisTemplate;
	}

	public void setRedisTemplate(RedisTemplate<?, ?> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
	@Override
	public void onMessage(Message message, byte[] pattern) {
		
		String event = new String(message.getBody());
		String channel = new String(message.getChannel());
		String patternStr = new String(pattern);
		String key = channel.substring(channel.lastIndexOf(":") + 1);
		log.info("subscribe redis=======>>>>> key:{}, event:{}, channel:{}, pattern:{}", key, event, channel, patternStr);

		if ("set".equals(event)) {
			Object value = redisTemplate.execute(new RedisCallback<Object>() {
				@Override
				public Object doInRedis(RedisConnection connection) throws DataAccessException {
					byte[] keyByte = key.getBytes();
					byte[] value = connection.get(keyByte);
					if (value == null) {
						return null;
					}
					return toObject(value);
				}
			}, true);
			log.info("received redis set event, update ehCache. key:{},value{}", key, value);
			ehCache.put(new Element(key, value));
		} else if ("del".equals(event) || "expired".equals(event)) {
			log.info("received redis " + event + " event, del ehCache. key:{}", key);
			ehCache.remove(key);
		}

	}
	
	/**
	 * 描述 : byte[]转Object . <br>
	 * 
	 * @param bytes
	 * @return
	 */
	private Object toObject(byte[] bytes) {
		Object obj = null;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bis);
			obj = ois.readObject();
			ois.close();
			bis.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		return obj;
	}

}
