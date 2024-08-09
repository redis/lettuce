# Getting Started

You can get started with Lettuce in various ways.

## 1. Get it

### For Maven users

Add these lines to file pom.xml:

``` xml
<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
    <version>6.4.0.RELEASE</version>
</dependency>
```

### For Ivy users

Add these lines to file ivy.xml:

``` xml
<ivy-module>
  <dependencies>
    <dependency org="io.lettuce" name="lettuce-core" rev="6.4.0.RELEASE"/>
  </dependencies>
</ivy-module>
```

### For Gradle users

Add these lines to file build.gradle:

``` groovy
dependencies {
  implementation 'io.lettuce:lettuce-core:6.4.0.RELEASE'
}
```

### Plain Java

Download the latest binary package from
<https://github.com/redis/lettuce/releases> and extract the
archive.

## 2. Start coding

So easy! No more boring routines, we can start.

Import required classes:

``` java
import io.lettuce.core.*;
```

and now, write your code:

``` java
RedisClient redisClient = RedisClient.create("redis://password@localhost:6379/0");
StatefulRedisConnection<String, String> connection = redisClient.connect();
RedisCommands<String, String> syncCommands = connection.sync();

syncCommands.set("key", "Hello, Redis!");

connection.close();
redisClient.shutdown();
```

Done!

Do you want to see working examples?

- [Standalone Redis](https://github.com/redis/lettuce/blob/main/src/test/java/io/lettuce/examples/ConnectToRedis.java)

- [Standalone Redis with SSL](https://github.com/redis/lettuce/blob/main/src/test/java/io/lettuce/examples/ConnectToRedisSSL.java)

- [Redis Sentinel](https://github.com/redis/lettuce/blob/main/src/test/java/io/lettuce/examples/ConnectToRedisUsingRedisSentinel.java)

- [Redis Cluster](https://github.com/redis/lettuce/blob/main/src/test/java/io/lettuce/examples/ConnectToRedisCluster.java)

- [Connecting to a ElastiCache Master](https://github.com/redis/lettuce/blob/main/src/test/java/io/lettuce/examples/ConnectToElastiCacheMaster.java)

- [Connecting to ElastiCache with Master/Replica](https://github.com/redis/lettuce/blob/main/src/test/java/io/lettuce/examples/ConnectToMasterSlaveUsingElastiCacheCluster.java)

- [Connecting to Azure Redis Cluster](https://github.com/redis/lettuce/blob/main/src/test/java/io/lettuce/examples/ConnectToRedisClusterSSL.java)

