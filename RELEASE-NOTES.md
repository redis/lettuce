lettuce 4.4.0 RELEASE NOTES
===========================

This release ships with [96 tickets](https://github.com/mp911de/lettuce/milestone/24?closed=1) fixed.

It contains a range of performance improvements to lower GC pressure, 
introducing kqueue native transport on BSD-like systems and asynchronous,
connection initialization for Redis Cluster and Cluster Pub/Sub subscriptions.

This release is compatible with Java 9 (class-path mode) requiring 
netty 4.1.11.Final.


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


Scan Iterators
--------------
Scan operations (`SCAN`, `HSCAN`, `SSCAN`, `ZSCAN`) can be used through an
enhanced `ScanIterator` API. Scan iterators implement `Iterator` allowing
Java 8 stream usage and work with synchronous Redis Standalone and Redis
Cluster connections. Scan iterators remove the complexity of cursor handling
for initial/subsequent `SCAN` commands and do not require a different approach
whether you use Redis Standalone or Redis Cluster.


```java
RedisCommands<String, String> commands = …
ScanIterator<String> scan = ScanIterator.scan(commands, ScanArgs.Builder.limit(50).match("foo"));

// Iterator use
while(scan.hasNext()) {
    // …
}

// Java 8 Stream
scan.stream().filter(…).map(…).forEach(…)
```
  

Find the full change log at the end of this document.

Thanks to all contributors who made lettuce 4.4.0 possible.
Lettuce 4.4.0 requires Java 8 or Java 9.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/

Commands
-----
* Adopt to read-only variant GEORADIUS(BYMEMBER)_RO #564 
* Support CLIENT and INFO commands for Redis Sentinel #415

Enhancements
-----
* Allow Pub/Sub subscriptions on a node selection within Redis Cluster #385
* Document RedisClient and RedisClusterClient implementation hooks #392 (Thanks to @HaloFour)
* Provide scan iterators for SCAN/HSCAN/SSCAN and ZSCAN commands #397
* Extend RedisClusterClient and RedisClusterFactoryBean to take multiple hosts #401
* Default to epoll transport if available #402
* Simplify NodeSelection implementation #408
* Allow setting client name with RedisURI #416
* Extend RedisConnectionStateListener.onRedisConnected by adding SocketAddress #419
* Connect asynchronously in cluster topology refresh #424 (Thanks to @szabowexler)
* Propagate asynchronous connection initialization #429
* Reuse stale connections collection when closing stale connections #443
* Add ReadFrom.SLAVE_PREFERRED #452 (Thanks to @Spikhalskiy)
* Avoid or reduce connection attempts to nodes in fail state #453 (Thanks to @Spikhalskiy)
* Store ReadOnly commands and query these as bitmask #457
* Initialize RedisStateMachine.LongProcessor in static initializer #481 (Thanks to @hellyguo)
* Add shutdownAsync for RedisClient #485 (Thanks to @jongyeol)
* Resolve hostnames using netty's DNS resolver #498
* Introduce NettyCustomizer API #499
* Use asynchronous connect in NodeSelection API #500
* Gracefully degrade to successfully connected Cluster read candidates #503 (Thanks to @eliotw)
* CommandHandler performs expensive queue check if requestQueueSize != Integer.MAX_VALUE #507 (Thanks to @CodingFabian)
* Consider using ThreadLocalRandom instead of Random in  RedisAdvancedCluster…CommandsImpl #510
* Lots of memory occupied by firstInteger in CommandArgs #511 (Thanks to @CodingFabian)
* Close/reconnect the connection when Redis protocol is corrupted #512 (Thanks to @jongyeol)
* Allow setting the Redis password as char-array #513
* Replace synchronized reconnect in ConnectionWatchdog with asynchronous connect #514
* Align log prefix for CommandHandler and ConnectionWatchdog #538
* Add support for kqueue transport #539
* Add support for client certificate authentication #540
* Reduce cross-thread queue sharing #544
* Use pre-instantiated ByteBufProcessor in RedisStateMachine to find EOL #557
* Replace CountDownLatch in AsyncCommand with AtomicFieldUpdater #559
* Allocate lazily of array outputs targets #573
* Enhance SingularArgument caching #575
* Reuse CommandHandler decoding via PubSubCommandHandler #576
* Introduce MethodTranslator caching #580


Fixes
-----
* Fail to second call pool.borrowObject() using StatefulRedisMasterSlaveConnection in 4.3.0.Final #411 (Thanks to @krisjey)
* Deprecate addListener/removeListener methods from execution-specific Pub/Sub interfaces. #412
* Unexpected 'Invalid database number' exception for database > 15 #420 (Thanks to @ChitraGuru)
* Dynamic API: array of arguments are not supported #421 (Thanks to @coolmeen)
* ClusterClientOptions.socketOptions returns ClientOptions.Builder #425 (Thanks to @maxd)
* Lettuce suppresses initial exception in Redis Cluster #427 (Thanks to @maxd)
* CDI can't create RedisClusterClient because of missing default constructor #438 (Thanks to @wuhuaxu)
* RoundRobinSocketAddressSupplier may contain more RedisClusterNodes than the current topology view #440
* Partitions.addPartition and reload not correctly synchronized #442
* pingBeforeActivateConnection and authentication fails using Redis Sentinel #448 (Thanks to @coolmeen)
* PooledClusterConnectionProvider.close does not close connections #460
* Possible race condition in ReconnectHandler.prepareClose() #465
* Dynamic API: Interface implementation fails for non-string connections #467
* ChannelInitializer (PING before connect) waits indefinite #470
* RedisClient.shutdown() does not synchronize on clientResources.shutdown future #475
* Unknown channel option warnings in log using UDS #476 (Thanks to @magdkudama)
* BlockingOperationException using RedisClusterClient with rxjava keys operation #486 (Thanks to @jeniok)
* Latency metrics is reset even if set to false #487 (Thanks to @ameenhere)
* firstResponseLatency depends on command completion time #491
* Shutdown PauseDetector if DefaultCommandLatencyCollector is shut down and PauseDetector is no longer required #516
* LatencyStats leaves PauseDetectorListener registered #517
* Shaded JAR contains too many dependencies #519
* Fix static initialization of PauseDetector when LatencyUtils are not on the class path #520
* jcl-over-slf4j is a compile dependency instead of test dependency #526
* NumberFormatException on "inf" zscore #528 (Thanks to @DarkSeraphim)
* Connection pool deadlock #531 (Thanks to @DarkSeraphim)
* Improve Epoll availability detection #535
* Close connections in pooling destroy hook #545
* Adopt to changed SLOWLOG output #551
* CommandHandler.write contains isConnected verification #556
* Reduce (eliminate byte[]) duplication during slot hash calculation #558
* clientSetname does not set client name on default connection #563
* Avoid getBytes() calls on constant value in ExperimentalByteArrayCodec #574(Thanks to @CodingFabian)
* Prevent dead listener callback failure in shutdown sequence #581 


Other
-----
* Test with JDK 9 Early Access builds #310
* Fix README #410 (Thanks to @changeworld)
* Annotate deprecated items with @Deprecated #414
* Disable WAIT as Read-Only command #418
* Upgrade to netty 4.1.7/4.0.43 #436
* Fix ClientResources usage in tests #445
* Enable JMH tests on TravisCI #454
* Upgrade to TravisCI trusty #461
* Upgrade to Mockito 2 #469
* Improve JavaDoc #472
* Upgrade dependencies #474
* Switch optional dependencies from provided to optional #482
* Update Netty version to 4.1.9 #489 (Thanks to @odiszapc)
* Move to lettuce.io #493
* Switch to codecov #504
* Upgrade integrations from mp911de/lettuce to lettuce-io/lettuce-core #506
* Upgrade to RxJava 1.2.10  #524
* Upgrade to netty 4.1.10 #532
* Upgrade to netty 4.0.11 #537
* Upgrade to netty 4.1.12 #548
* Allow Redis version pinning for build #552
* Upgrade to netty 4.1.13/4.0.49 #565
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
