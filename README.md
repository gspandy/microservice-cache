# microservice-cache

基于ehcache（1级）、redis（2级）的二级缓存组件


## 组件目标
* 解决以redis主备作为中心节点提供缓存时，随着访问量增大，大量的缓存数据访问使得应用服务器和redis缓存服务器之间的网络I/O消耗过大的问题。


## 设计思路来源
* 在以redis为中心节点的方案上优化，在缓存到远程redis的同时，缓存一份到本地缓存ehcache（此处的ehcache不用做集群，避免组播带来的开销）。取缓存的时候会先取本地ehcache，没有取到则会向redis请求，这样会减少应用服务器<–>缓存服务器redis之间的网络开销。每个应用服务订阅redis的keyspace时间，当发生set、del事件时，更新本地ehcache缓存，确保一二级缓存数据一致性。

## 项目信息
java:1.8   
SpringBoot:1.5.8    
Spring:4.0+ 


## 使用方法
#### pom文件

``` 
<dependency>
    <groupId>com.yonyou.cloud</groupId>
    <artifactId>cache-client</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

#### 配置文件
redis相关配置，与spring-boot-starter-redis配置一致。
```
#host
spring.redis.host=localhost
#Port  
spring.redis.port=6379
#password
spring.redis.password=*****
# database name
spring.redis.database=0
# pool settings ...
spring.redis.pool.max-idle=8
spring.redis.pool.min-idle=0
spring.redis.pool.max-active=8
spring.redis.pool.max-wait=-1
```
ehRedisCache相关配置，设置二级缓存相关的配置。
```
# ehcache元素过期的访问间隔(秒为单位). 0表示可以永远空闲,默认为0
ehRedisCache.ehcache.timeToLiveSeconds 

# ehcache元素在缓存里存在的时间(秒为单位). 0 表示永远存在不过期,默认为0
ehRedisCache.ehcache.timeToLiveSeconds

# 当达到maxElementsInMemory限制时，Ehcache将会根据指定的策略去清理内存。可选策略有：LRU（最近最少使用，默认策略）、FIFO（先进先出）、LFU（最少访问次数）。
ehRedisCache.ehcache.memoryStoreEvictionPolicy

# ehRedisCache缓存名称
ehRedisCache.cacheName

# 一级缓存击穿值，当一级缓存命中数大于该值则强制访问二级缓存 ，并刷新一级缓存。默认值：10
ehRedisCache.activeCount

# 二级缓存redis的过期时间，单位为秒。默认为 0，永不过期。
ehRedisCache.redisCache.liveTime

# 订阅redis的key，列入user*：订阅user开头的key的所有事件
ehRedisCache.redis.subscribe.keyPattern
```
#### 实际使用
其中Cacheable注解的value就是ehRedisCache.cacheName定义的缓存名，key是缓存的key值。
```
    @Cacheable(value="users", key="'user'+#p0")
	public User findUser(int id) {
		System.err.println("没有使用缓存！！！！");
		User user = selectById(id);
		return user;
	}
```
