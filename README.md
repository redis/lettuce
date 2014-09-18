lettuce - A scalable Java Redis client
======================================

[![Build Status](https://travis-ci.org/mp911de/lettuce.svg)](https://travis-ci.org/mp911de/lettuce) [![Coverage Status](https://img.shields.io/coveralls/mp911de/lettuce.svg)](https://coveralls.io/r/mp911de/lettuce) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/biz.paluch.redis/lettuce/badge.svg)](https://maven-badges.herokuapp.com/maven-central/biz.paluch.redis/lettuce)

Lettuce is a scalable thread-safe Redis client providing both synchronous and
asynchronous connections. Multiple threads may share one connection provided
they avoid blocking and transactional operations such as BLPOP, and MULTI/EXEC.
Multiple connections are efficiently managed by the excellent netty NIO
framework. Support for advanced redis features such as Sentinel, Cluster and redis data models 
is included.

This version of lettuce has been tested against redis 2.8.13 and 3.0-beta8.

* Works with Java 6, 7 and 8
* synchronous and [asynchronous connections](https://github.com/mp911de/lettuce/wiki/Asynchronous-Connections)
* [Redis Sentinel](https://github.com/mp911de/lettuce/wiki/Redis-Sentinel)
* [Redis Cluster](https://github.com/mp911de/lettuce/wiki/Redis-Cluster)
* [Streaming API](https://github.com/mp911de/lettuce/wiki/Streaming-API)
* [CDI](https://github.com/mp911de/lettuce/wiki/CDI-Support) and [Spring](https://github.com/mp911de/lettuce/wiki/Spring-Support) support
* [Codecs](https://github.com/mp911de/lettuce/wiki/Codecs) (for UTF8/bit/JSON etc. representation of your data)
* multiple [Connection Interfaces](https://github.com/mp911de/lettuce/wiki/Connection-Interfaces)


See the [Wiki](https://github.com/mp911de/lettuce/wiki) for more docs.

I'm developing and maintaining actively the fork of https://github/wg/lettuce

Maven Artifacts/Download
----------------

Releases of lettuce are available in the maven central repository. Take also a look at the [Download](https://github.com/mp911de/lettuce/wiki/Download) page in the [Wiki](https://github.com/mp911de/lettuce/wiki).

```xml
<dependency>
  <groupId>biz.paluch.redis</groupId>
  <artifactId>lettuce</artifactId>
  <version>3.0.1.Final</version>
</dependency>
```

Shaded JAR-File (packaged dependencies  and relocated to the `com.lambdaworks` package to prevent version conflicts)

```xml
<dependency>
  <groupId>biz.paluch.redis</groupId>
  <artifactId>lettuce</artifactId>
  <version>3.0.1.Final</version>
  <classifier>shaded</classifier>
  <exclusions>
    <exclusion>
      <groupId>io.netty</groupId>
      <artifactId>netty-common</artifactId>
    </exclusion>

    <exclusion>
      <groupId>io.netty</groupId>
      <artifactId>netty-transport</artifactId>
    </exclusion>

    <exclusion>
      <groupId>com.google.guava</groupId>
      <artifactId>guava</artifactId>
    </exclusion>

    <exclusion>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-pool2</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```    

or snapshots at https://oss.sonatype.org/content/repositories/snapshots/

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
    
Redis data structures
--------

Redis uses data structures for same commands (CLUSTER NODES, COMMAND INFO, ROLE). lettuce has support for these data models
by providing parsers and structures. 

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

Performance
-----------

Lettuce is made for performance. Issuing (and returning) 1000 PING's over the sync API takes on a MacBook with Intel i7 an average of 190ms to complete all.
The async API can issue 1000 commands within 20ms.
    
License
-------
* [Apache License 2.0] (http://www.apache.org/licenses/LICENSE-2.0)
* Fork of https://github.com/wg/lettuce

Contributing
-------
Github is for social coding: if you want to write code, I encourage contributions through pull requests from forks of this repository. 
Create Github tickets for bugs and new features and comment on the ones that you are interested in.
