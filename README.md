lettuce - Advanced Java Redis client
===============================

[![Build Status](https://travis-ci.org/mp911de/lettuce.svg)](https://travis-ci.org/mp911de/lettuce) [![Coverage Status](https://coveralls.io/repos/mp911de/lettuce/badge.svg?branch=master&service=github)](https://coveralls.io/github/mp911de/lettuce?branch=master) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/biz.paluch.redis/lettuce/badge.svg)](https://maven-badges.herokuapp.com/maven-central/biz.paluch.redis/lettuce)

Lettuce is a scalable thread-safe Redis client providing both synchronous and
asynchronous connections. Multiple threads may share one connection provided
they avoid blocking and transactional operations such as BLPOP, and MULTI/EXEC.
Multiple connections are efficiently managed by the excellent netty NIO
framework. Support for advanced Redis features such as Sentinel, Cluster and Redis data models
is included.

This version of lettuce has been tested against Redis 3.0.

* Works with Java 6, 7 and 8
* synchronous and [asynchronous connections](https://github.com/mp911de/lettuce/wiki/Asynchronous-Connections)
* [Redis Sentinel](https://github.com/mp911de/lettuce/wiki/Redis-Sentinel)
* [Redis Cluster](https://github.com/mp911de/lettuce/wiki/Redis-Cluster)
* [SSL](https://github.com/mp911de/lettuce/wiki/SSL-Connections) and [Unix Domain Socket](https://github.com/mp911de/lettuce/wiki/Unix-Domain-Sockets) connections
* [Streaming API](https://github.com/mp911de/lettuce/wiki/Streaming-API)
* [CDI](https://github.com/mp911de/lettuce/wiki/CDI-Support) and [Spring](https://github.com/mp911de/lettuce/wiki/Spring-Support) support
* [Codecs](https://github.com/mp911de/lettuce/wiki/Codecs) (for UTF8/bit/JSON etc. representation of your data)
* multiple [Connection Interfaces](https://github.com/mp911de/lettuce/wiki/Connection-Interfaces)

See the [Wiki](https://github.com/mp911de/lettuce/wiki) for more docs.

I'm developing and maintaining actively the fork of https://github/wg/lettuce

Communication
---------------

* Google Group: [lettuce-redis-client-users](https://groups.google.com/d/forum/lettuce-redis-client-users) or lettuce-redis-client-users@googlegroups.com
* [![Join the chat at https://gitter.im/mp911de/lettuce](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/mp911de/lettuce?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
* [Github Issues](https://github.com/mp911de/lettuce/issues)


Documentation
---------------

* [Wiki](https://github.com/mp911de/lettuce/wiki)
* [Javadoc](http://redis.paluch.biz/docs/api/releases/latest/)


Binaries/Download
----------------

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at http://search.maven.org.

Releases of lettuce are available in the maven central repository. Take also a look at the [Download](https://github.com/mp911de/lettuce/wiki/Download) page in the [Wiki](https://github.com/mp911de/lettuce/wiki).

Example for Maven:

```xml
<dependency>
  <groupId>biz.paluch.redis</groupId>
  <artifactId>lettuce</artifactId>
  <version>x.y.z</version>
</dependency>
```

Shaded JAR-File (packaged dependencies  and relocated to the `com.lambdaworks` package to prevent version conflicts)

```xml
<dependency>
  <groupId>biz.paluch.redis</groupId>
  <artifactId>lettuce</artifactId>
  <version>x.y.z</version>
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
  RedisClient client = RedisClient.create("redis://localhost")
  RedisStringsConnection<String, String> connection = client.connect()
  String value = connection.get("key")
```

Each Redis command is implemented by one or more methods with names identical
to the lowercase Redis command name. Complex commands with multiple modifiers
that change the result type include the CamelCased modifier as part of the
command name, e.g. zrangebyscore and zrangebyscoreWithScores.

See https://github.com/mp911de/lettuce/wiki/Basic-usage  for further details.

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

See https://github.com/mp911de/lettuce/wiki/Asynchronous-Connections for further details.

Pub/Sub
-------

```java
RedisPubSubConnection<String, String> connection = client.connectPubSub()
connection.addListener(new RedisPubSubListener<String, String>() { ... })
connection.subscribe("channel")
```

Building
-----------

Lettuce is built with Apache Maven. The tests require multiple running Redis instances for different test cases which
are configured using a ```Makefile```. All tests run against Redis branch 3.0

To build:

```
$ git clone https://github.com/mp911de/lettuce.git
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

For bugs, questions and discussions please use the [Github Issues](https://github.com/mp911de/lettuce/issues).

License
-------

* [Apache License 2.0] (http://www.apache.org/licenses/LICENSE-2.0)
* Fork of https://github.com/wg/lettuce

Contributing
-------

Github is for social coding: if you want to write code, I encourage contributions through pull requests from forks of this repository. 
Create Github tickets for bugs and new features and comment on the ones that you are interested in and take a look into [CONTRIBUTING.md](https://github.com/mp911de/lettuce/tree/master/CONTRIBUTING.md)
