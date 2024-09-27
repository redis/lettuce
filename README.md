<img src="https://avatars2.githubusercontent.com/u/25752188?v=4" width="50" height="50"> Lettuce - Advanced Java Redis client
===============================

 [![Integration](https://github.com/redis/lettuce/actions/workflows/integration.yml/badge.svg?branch=main)](https://github.com/redis/lettuce/actions/workflows/integration.yml)
 [![codecov](https://codecov.io/gh/redis/lettuce/branch/main/graph/badge.svg?token=pAstxAAjYo)](https://codecov.io/gh/redis/lettuce)
 [![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE.txt)
 [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.lettuce/lettuce-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.lettuce/lettuce-core)
 [![Javadocs](https://www.javadoc.io/badge/io.lettuce/lettuce-core.svg)](https://www.javadoc.io/doc/io.lettuce/lettuce-core)

[![Discord](https://img.shields.io/discord/697882427875393627.svg?style=social&logo=discord)](https://discord.gg/redis)
[![Twitch](https://img.shields.io/twitch/status/redisinc?style=social)](https://www.twitch.tv/redisinc)
[![YouTube](https://img.shields.io/youtube/channel/views/UCD78lHSwYqMlyetR0_P4Vig?style=social)](https://www.youtube.com/redisinc)
[![Twitter](https://img.shields.io/twitter/follow/redisinc?style=social)](https://twitter.com/redisinc)

Lettuce is a scalable thread-safe Redis client for synchronous,
asynchronous and reactive usage. Multiple threads may share one connection if they avoid blocking and transactional
operations such as `BLPOP` and  `MULTI`/`EXEC`.
Lettuce is built with [netty](https://github.com/netty/netty).
Supports advanced Redis features such as Sentinel, Cluster, Pipelining, Auto-Reconnect and Redis data models.

This version of Lettuce has been tested against the latest Redis source-build.

* [synchronous](https://redis.github.io/lettuce/user-guide/connecting-redis/#basic-usage), [asynchronous](https://redis.github.io/lettuce/user-guide/async-api/) and [reactive](https://redis.github.io/lettuce/user-guide/reactive-api/) usage
* [Redis Sentinel](https://redis.github.io/lettuce/ha-sharding/#redis-sentinel_1)
* [Redis Cluster](https://redis.github.io/lettuce/ha-sharding/#redis-cluster)
* [SSL](https://redis.github.io/lettuce/advanced-usage/#ssl-connections) and [Unix Domain Socket](https://redis.github.io/lettuce/advanced-usage/#unix-domain-sockets) connections
* [Streaming API](https://redis.github.io/lettuce/advanced-usage/#streaming-api)
* [CDI](https://redis.github.io/lettuce/integration-extension/#cdi-support)
* [Codecs](https://redis.github.io/lettuce/integration-extension/#codecss) (for UTF8/bit/JSON etc. representation of your data)
* multiple [Command Interfaces](https://github.com/redis/lettuce/wiki/Command-Interfaces-%284.0%29)
* Support for [Native Transports](https://redis.github.io/lettuce/advanced-usage/#native-transports)
* Compatible with Java 8++ (implicit automatic module w/o descriptors)

See the [reference documentation](https://redis.github.io/lettuce/) and [API Reference](https://www.javadoc.io/doc/io.lettuce/lettuce-core/latest/index.html) for more details.

## How do I Redis?

[Learn for free at Redis University](https://university.redis.com/)

[Build faster with the Redis Launchpad](https://launchpad.redis.com/)

[Try the Redis Cloud](https://redis.com/try-free/)

[Dive in developer tutorials](https://developer.redis.com/)

[Join the Redis community](https://redis.com/community/)

[Work at Redis](https://redis.com/company/careers/jobs/)

Documentation
---------------

* [Reference documentation](https://redis.github.io/lettuce/)
* [Javadoc](https://www.javadoc.io/doc/io.lettuce/lettuce-core/latest/index.html)

Binaries/Download
----------------

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at http://search.maven.org.

Releases of lettuce are available in the Maven Central repository. Take also a look at the [Releases](https://github.com/redis/lettuce/releases).

Example for Maven:

```xml
<dependency>
  <groupId>io.lettuce</groupId>
  <artifactId>lettuce-core</artifactId>
  <version>x.y.z</version>
</dependency>
```

If you'd rather like the latest snapshots of the upcoming major version, use our Maven snapshot repository and declare the appropriate dependency version.

```xml
<dependency>
  <groupId>io.lettuce</groupId>
  <artifactId>lettuce-core</artifactId>
  <version>x.y.z.BUILD-SNAPSHOT</version>
</dependency>

<repositories>
  <repository>
    <id>sonatype-snapshots</id>
    <name>Sonatype Snapshot Repository</name>
    <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>
```

Basic Usage
-----------

```java
RedisClient client = RedisClient.create("redis://localhost");
StatefulRedisConnection<String, String> connection = client.connect();
RedisStringCommands sync = connection.sync();
String value = sync.get("key");
```

Each Redis command is implemented by one or more methods with names identical
to the lowercase Redis command name. Complex commands with multiple modifiers
that change the result type include the CamelCased modifier as part of the
command name, e.g. zrangebyscore and zrangebyscoreWithScores.

See [Basic usage](https://redis.github.io/lettuce/user-guide/connecting-redis/#basic-usage) for further details.

Asynchronous API
------------------------

```java
StatefulRedisConnection<String, String> connection = client.connect();
RedisStringAsyncCommands<String, String> async = connection.async();
RedisFuture<String> set = async.set("key", "value");
RedisFuture<String> get = async.get("key");

LettuceFutures.awaitAll(set, get) == true

set.get() == "OK"
get.get() == "value"
```

See [Asynchronous API](https://redis.github.io/lettuce/user-guide/async-api/) for further details.

Reactive API
------------------------

```java
StatefulRedisConnection<String, String> connection = client.connect();
RedisStringReactiveCommands<String, String> reactive = connection.reactive();
Mono<String> set = reactive.set("key", "value");
Mono<String> get = reactive.get("key");

set.subscribe();

get.block() == "value"
```

See [Reactive API](https://redis.github.io/lettuce/user-guide/reactive-api/) for further details.

Pub/Sub
-------

```java
RedisPubSubCommands<String, String> connection = client.connectPubSub().sync();
connection.getStatefulConnection().addListener(new RedisPubSubListener<String, String>() { ... })
connection.subscribe("channel");
```

Building
-----------

Lettuce is built with Apache Maven. The tests require multiple running Redis instances for different test cases which
are configured using a ```Makefile```. Tests run by default against Redis `unstable`.

To build:

```
$ git clone https://github.com/redis/lettuce.git
$ cd lettuce/
$ make prepare ssl-keys
$ make test
```

* Initial environment setup (clone and build `redis`): ```make prepare```
* Setup SSL Keys: ```make ssl-keys```
* Run the build: ```make test```
* Start Redis (manually): ```make start```
* Stop Redis (manually): ```make stop```

Bugs and Feedback
-----------

For bugs, questions and discussions please use the [GitHub Issues](https://github.com/redis/lettuce/issues).

License
-------

* This repository is licensed under the "MIT" license. See [LICENSE](LICENSE).
* Fork of https://github.com/wg/lettuce

Contributing
-------

Github is for social coding: if you want to write code, I encourage contributions through pull requests from forks of this repository. 
Create Github tickets for bugs and new features and comment on the ones that you are interested in and take a look into [CONTRIBUTING.md](https://github.com/redis/lettuce/blob/main/.github/CONTRIBUTING.md)
