Lettuce 5.0.0 RC1 RELEASE NOTES
==================================

This release comes with 29 completed issues and drops support for netty 4.0.

This release introduces ships with few breaking changes such as `Duration` use 
for timeouts and delays and changes the return type for `configGet` 
from `List<String>` to `Map<String, String>`.

Lettuce now contains a range of performance improvements to lower GC pressure, 
introduces kqueue native transport on BSD-like systems and asynchronous,
connection initialization.

Browse the docs at https://lettuce.io/core/snapshot/reference/.

This release asserts compatibility in class-path mode with Java 9 requiring 
at least netty 4.1.11.Final.


Redis Cluster Pub/Sub on Node-Selections
----------------------------------------
`RedisClusterClient.connectPubSub()` now returns a 
`StatefulRedisClusterPubSubConnection` that allows registration of 
`RedisClusterPubSubListener`s and subscription on particular cluster nodes.

Cluster node-specific subscriptions allow usage of keyspace notifications. 
Keyspace notifications are different from userspace Pub/Sub since keyspace 
notifications don't broadcast to the entire cluster but are published only 
on the node the notification happens. A common usecase is when a key expires 
in the cluster.

```
StatefulRedisClusterPubSubConnection connection = client.connectPubSub();

connection.addListener(…);

connection.setNodeMessagePropagation(true);

RedisClusterPubSubCommands<String, String> sync = connection.sync();
sync.slaves().commands().psubscribe("__key*__:expire");
```


Native Transports
-----------------
Lettuce now uses native transports by default, if the operating system is 
qualified and dependencies are available. Lettuce supports epoll (on 
Linux-based systems) since 4.0 and since this version kqueue (BSD-based
systems, like macOS).

Epoll usage can be disabled with system properties by setting 
`io.lettuce.core.epoll=false`. In a similar way, kqueue can be disabled 
with `io.lettuce.core.kqueue=false`.

Epoll dependency:

```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-transport-native-epoll</artifactId>
    <version>${netty-version}</version>
    <classifier>linux-x86_64</classifier>
</dependency>
```

Kqueue dependency:

```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-transport-native-kqueue</artifactId>
    <version>${netty-version}</version>
    <classifier>osx-x86_64</classifier>
</dependency>
```


Asynchronous Connections in Redis Cluster
-----------------------------------------
RedisClusterClient now connects asynchronously without intermediate 
synchronization to cluster nodes. The connection progress is shared between
multiple threads that request a cluster node connection for the first time.
Previously, connection was sequential-synchronous. Each connection attempt
blocked subsequent attempts from other threads. If a cluster node connection
ran into a timeout, threads were penalized with a growing wait time. If, say, 10 
threads waited for a connection, the last thread had to wait up to 10 times 
the connection timeout.

Asynchronous connection initiates the connection once using internally a 
`Future` so multiple concurrent connection attempts share the resulting
`Future`. Errors fail now faster and cluster node usage is fully asynchronous
without synchronization and without the danger to run into a threading deadlock.


Thanks to all contributors that made Lettuce 5.0.0.RC1 possible.

Lettuce 5.0.0.RC1 requires Java 8 and cannot be used with Java 6 or 7.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/


Enhancements
------------
* Drop netty 4.0 compatibility #518
* Support default methods on Command Interfaces #527
* Align log prefix for CommandHandler and ConnectionWatchdog #538
* Add support for kqueue transport #539
* Add support for client certificate authentication #540
* Adopt to changed SLOWLOG output #551
* Use pre-instantiated ByteBufProcessor in RedisStateMachine to find EOL #557
* SlotHash.getSlot(…) duplicates bytes while slot calculation #558
* Replace CountDownLatch in AsyncCommand with AtomicFieldUpdater #559
* Expose asynchronous connection creation API #561
* [BREAKING] Refactor CONFIG GET output from List<String> to Map<String, String> #562
* Adopt to read-only variant GEORADIUS(BYMEMBER)_RO #564
* [BREAKING] Switch to Duration for timeouts #570

Fixes
-----
* NumberFormatException on `inf` zscore #528 (Thanks to @DarkSeraphim)
* Connection pool deadlock #531 (Thanks to @DarkSeraphim)
* Improve Epoll availability detection #535
* Fix FastCountingDeque not recording correct size. #544 (Thanks to @kojilin)
* Lettuce doesn't call QUIT when a pooled connection is destroyed. #545 (Thanks to @robbiemc)
* clientSetname does not set client name on default connection #563

Other
------
* Test with JDK 9 Early Access builds #310
* Upgrade to netty 4.1.10 #532
* Upgrade to netty 4.0.11 #537
* Upgrade to netty 4.1.12 (4.0.48) #548
* Upgrade to Reactor 3.1 M2 #549
* Allow Redis version pinning for build #552
* Upgrade to netty 4.1.13/4.0.49 #565
* Upgrade to Reactor 3.1.0 M3 (Bismuth M3) #567
* Use BOM to manage netty and Spring dependencies #568
* Upgrade dependencies #569

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
