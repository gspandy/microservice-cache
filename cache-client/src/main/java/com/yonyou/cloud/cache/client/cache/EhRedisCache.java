package com.yonyou.cloud.cache.client.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**
 * 两级缓存，一级:ehcache,二级为redisCache
 *
 */
public class EhRedisCache implements Cache {

	private static final Logger log = LoggerFactory.getLogger(EhRedisCache.class);

	private String name;

	private net.sf.ehcache.Ehcache ehCache;

	private RedisTemplate<?, ?> redisTemplate;

	private long liveTime = 60; // 默认redis过期时间60秒

	private int activeCount = 10; //默认访问10次EhCache 强制访问一次redis 使得数据不失效

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Object getNativeCache() {
		return this;
	}

	@Override
	public ValueWrapper get(Object key) {
		Element value = ehCache.get(key);

		log.info("Cache L1 (ehcache) :{}={}", key.toString(), value);

		if (value != null) {
			if (value.getHitCount() < activeCount) {
				return new SimpleValueWrapper(value.getObjectValue());
			} else {
                //统计次数重置为0
				value.resetAccessStatistics();
			}
		}
		final String keyStr = key.toString();
		Object objectValue = redisTemplate.execute(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				byte[] key = keyStr.getBytes();
				byte[] value = connection.get(key);
				if (value == null) {
					return null;
				}
				// 每次获得，重置缓存过期时间
//				if (liveTime > 0) {
//					connection.expire(key, liveTime);
//				}
				return toObject(value);
			}
		}, true);
		
		ehCache.put(new Element(key, objectValue));// 取出来之后缓存到本地
		log.info("Cache L2 (redis) :{}={}", keyStr, objectValue);
		return (objectValue != null ? new SimpleValueWrapper(objectValue) : null);

	}

	@Override
	public void put(Object key, Object value) {
//		value为空则清除缓存
//		if(null == value){
//			evict(key);
//			return;
//		}
		
		log.info("Cache put :{}={}", key, value);
		ehCache.put(new Element(key, value));
		final String keyStr = key.toString();
		final Object valueStr = value;
		redisTemplate.execute(new RedisCallback<Long>() {
			@Override
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				byte[] keyb = keyStr.getBytes();
				byte[] valueb = toByteArray(valueStr);
				connection.set(keyb, valueb);
				if (liveTime > 0) {
					connection.expire(keyb, liveTime);
				}
				return 1L;
			}
		}, true);

	}

	@Override
	public void evict(Object key) {
		ehCache.remove(key);
		final String keyStr = key.toString();
		redisTemplate.execute(new RedisCallback<Long>() {
			@Override
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.del(keyStr.getBytes());
			}
		}, true);
	}

	@Override
	public void clear() {
		ehCache.removeAll();
		redisTemplate.execute(new RedisCallback<String>() {
			@Override
			public String doInRedis(RedisConnection connection) throws DataAccessException {
				connection.flushDb();
				return "clear done.";
			}
		}, true);
	}

	public net.sf.ehcache.Ehcache getEhCache() {
		return ehCache;
	}

	public void setEhCache(Ehcache ehcache) {
		this.ehCache = ehcache;
	}

	public RedisTemplate<?, ?> getRedisTemplate() {
		return redisTemplate;
	}

	public void setRedisTemplate(RedisTemplate<?, ?> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public long getLiveTime() {
		return liveTime;
	}

	public void setLiveTime(long liveTime) {
		this.liveTime = liveTime;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getActiveCount() {
		return activeCount;
	}

	public void setActiveCount(int activeCount) {
		this.activeCount = activeCount;
	}

	/**
	 * 描述 : Object转byte[]. <br>
	 * 
	 * @param obj
	 * @return
	 */
	private byte[] toByteArray(Object obj) {
		byte[] bytes = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(obj);
			oos.flush();
			bytes = bos.toByteArray();
			oos.close();
			bos.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return bytes;
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

	@Override
	public <T> T get(Object arg0, Class<T> arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T get(Object arg0, Callable<T> arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ValueWrapper putIfAbsent(Object arg0, Object arg1) {
		// TODO Auto-generated method stub
		return null;
	}
}
