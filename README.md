lettuce - A scalable Java Redis client
======================================

Lettuce is a scalable thread-safe Redis client providing both synchronous and
asynchronous connections. Multiple threads may share one connection provided
they avoid blocking and transactional operations such as BLPOP, and MULTI/EXEC.
Multiple connections are efficiently managed by the excellent netty NIO
framework.

This version of lettuce has been tested against redis 2.8.9. 

Lettuce works with:

* Java 6
* Java 7
* Java 8

Currently following commands are __not yet__ supported:

* Sorted Sets: ZLEXCOUNT, ZRANGEBYLEX, ZREMRANGEBYLEX 
* Server Commands: DEBUG SEGFAULT, MONITOR, PUBSUB


Join the lambdaWorks-OSS Google Group to discuss this project:

    http://groups.google.com/group/lambdaworks-oss
    lambdaworks-oss@googlegroups.com
    
Maven Artifacts
----------------

Releases of lettuce are available in the maven central repository.

```xml
<dependency>
  <groupId>com.lambdaworks</groupId>
  <artifactId>lettuce</artifactId>
  <version>2.3.3</version>
</dependency>
```    

Basic Usage
-----------

```java
  RedisClient client = new RedisClient("localhost")
  RedisStringsConnection<String, String> connection = client.connect()
  String value = connection.get("key")
```

Each redis command is implemented by one or more methods with names identical
to the lowercase redis command name. Complex commands with multiple modifiers
that change the result type include the CamelCased modifier as part of the
command name, e.g. zrangebyscore and zrangebyscoreWithScores.

Redis connections are designed to be long-lived, and if the connection is lost
will reconnect until close() is called. Pending commands that have not timed
out will be (re)sent after successful reconnection.

All connections inherit a default timeout from their RedisClient and
and will throw a RedisException when non-blocking commands fail to return a
result before the timeout expires. The timeout defaults to 60 seconds and
may be changed in the RedisClient or for each individual connection.

Asynchronous Connections
------------------------

```java
RedisStringsConnection<String, String> async = client.connectAsync()
RedisFuture<String> set = async.set("key", "value")
RedisFuture<String> get = async.get("key")

async.awaitAll(set, get) == true

set.get() == "OK"
get.get() == "value"
 ```

Pub/Sub
-------

```java
RedisPubSubConnection<String, String> connection = client.connectPubSub()
connection.addListener(new RedisPubSubListener<String, String>() { ... })
connection.subscribe("channel")
```

Advanced Usage
--------------
  
RedisClient can take a RedisURI object for connecting. RedisURI contains host, authentication, database, timeout and
sentinel details. You can build your own RedisURI or use the RedisUI Builder.
  
```java
RedisURI redisUri = RedisURI.Builder.redis("localhost").withPassword("authentication").withDatabase(2).build();

RedisClient client = new RedisClient(rediUri);    
```
  
Streaming API
-------------
  
Redis can contain a huge set of data. Collections can burst your memory, when the amount of data is too massive for your heap.
Lettuce can return your collection data either as List/Set/Map or can push the data on StreamingChannel interfaces.
StreamingChannels are similar to callback methods. Every method, which can return bulk data (except transactions/multi and some
config methods) specifies beside a regular method with a collection return class also method which accepts a StreamingChannel.

There are 4 StreamingChannels accepting different data types:
     
  * KeyStreamingChannel
  * ValueStreamingChannel
  * KeyValueStreamingChannel
  * ScoredValueStreamingChannel
  
The result of the steaming methods is the count of keys/values/key-value pairs as long value.
  
```java
Long count = redis.hgetall(new KeyValueStreamingChannel<String, String>()
    {
        @Override
        public void onKeyValue(String key, String value)
        {
            ...
        }
    }, key);
```

Streaming happens real-time to the redis responses. The method call (future) completes after the last call to the StreamingChannel.
    
    
Sentinel
--------

  There are two ways, how to use Redis sentinel with Lettuce. 
  
  1. Direct connection to sentinel, for issuing sentinel commands
  2. Redis discovery using sentinel.
  
In both cases you've to supply a RedisURI since the Redis Sentinel integration supports multiple Sentinel hosts to
provide a high availability.
  
Please note: Redis Sentinel integration provides only async connections and no connection pooling until now.
  

### Sentinel connection

```java
RedisURI redisUri = RedisURI.Builder.sentinel("sentinelhost1", "mymaster").withSentinel("sentinelhost2").build();
RedisClient client = new RedisClient(redisUri);

RedisSentinelAsyncConnection<String, String>  connection = client.connectSentinelAsync();

Map<String, String> map = connection.master("mymaster").get();
```
  
### Redis discovery

```java
RedisURI redisUri = RedisURI.Builder.sentinel("sentinelhost1", "mymaster").withSentinel("sentinelhost2").build();
RedisClient client = new RedisClient(redisUri);

RedisConnection<String, String> connection = client.connect();
```
    
Please note: Every time you connect to redis using sentinel, the redis master is discovered using a new connection to a sentinel. This
can be time consuming, especially when multiple sentinels are tried and run perhaps into timeouts.
  
  
Clustering
--------

lettuce supports redis cluster (v3.0) operations. 

```java
RedisURI redisUri = RedisURI.Builder.redis("localhost").withPassword("authentication").build();

RedisClusterClient client = new RedisClusterClient(rediUri);
RedisClusterAsyncConnection<String, String> client = clusterClient.connectClusterAsync()
```

The clustering support covers:
  
  * Support of all CLUSTER commands
  * Cluster node selection (initial) based on key hash-slot
  * MOVED redirection handling
  * Node authentication

The clustering needs one or more initial nodes in order to resolve the cluster topology (partitions). 
The client maintains multiple connections, which are selected based on the topology and hash. In case your requests
run into MOVED errors (because of slot imports/migrations), you can reload the partitions using 
  
    RedisClusterClient.reloadPartitions
    


Connection Interfaces
---------------------

  Redis supports 400+ commands. These commands are grouped within multiple interfaces:
 
  * RedisHashesConnection
  * RedisKeysConnection
  * RedisListsConnection
  * RedisScriptingConnection
  * RedisServerConnection
  * RedisSetsConnection
  * RedisSortedSetsConnection
  * RedisStringsConnection

Every of these interfaces is available though the connect() method. Same applies for the async interfaces:

  * RedisHashesAsyncConnection
  * RedisKeysAsyncConnection
  * RedisListsAsyncConnection
  * RedisScriptingAsyncConnection
  * RedisServerAsyncConnection
  * RedisSetsAsyncConnection
  * RedisSortedSetsAsyncConnection
  * RedisStringsAsyncConnection

These interfaces are implemented by the merged connection classes of RedisConnection for the sync execution and 
RedisAsyncConnection for async execution which are also available using connect() or connectAsync(). The grouping is
derived from the redis command grouping.

Connection Pooling
------------------

The built-in connection pooling provides managed connections. Every pool can allocate sync and async connections.
  
```java  
RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();

RedisConnection<String, String> connection = pool.allocateConnection();

try {
    connection.set("key", "value");
} finally {    
    pool.freeConnection(connection);
}
```
    
  Every pool keeps its connections until you explicitly close the pool.
    
  This client also provides transparent pooling, so yo don't have to bother yourself with allocating/freeing connections. 
  
```java  
RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
RedisConnection<String, String> connection = PoolingProxyFactory.create(pool);

connection.set("a", "b");
connection.set("x", "y");

pool.close();
```  

Codecs
------

Lettuce supports pluggable codecs responsible for encoding and decoding keys
and values. The default codec supports UTF-8 encoded String keys and values.

Each connection may have its own codec passed to the extended
RedisClient.connect methods:

```java
RedisConnection<K, V> connect(RedisCodec<K, V> codec)
RedisAsyncConnection<K, V> connectAsync(RedisCodec<K, V> codec)
RedisPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec)
```

For pub/sub connections channel names and patterns are treated as keys,
messages are treated as values.

  
Spring Support
--------------
  
Lettuce provides a factory for the RedisClient. You need to specify a redisUri or a URI string in order to
create the client.

```xml  
<bean id="redisClient" class="com.lambdaworks.redis.support.RedisClientFactoryBean">
    <property name="password" value="mypassword"/>
    <!-- Redis Uri Format: redis://host[:port]/database -->
    <!-- Redis Uri: Specify Database as Path -->
    <property name="uri" value="redis://localhost/12"/>
    
    <!-- Redis Sentinel Uri Format: redis-sentinel://host[:port][,host[:port][,host[:port]]/database#masterId -->
    <!-- Redis Sentinel Uri: You can specify multiple sentinels. Specify Database as Path, Master Id as Fragment. -->
    <property name="uri" value="redis-sentinel://localhost,localhost2,localhost3/1#myMaster"/>
</bean>
```

Performance
-----------

Lettuce is made for performance. Issuing (and returning) 1000 PING's over the sync API takes on a MacBook with Intel i7 an average of 190ms to complete all.
The async API can issue 1000 commands within 20ms.
    
