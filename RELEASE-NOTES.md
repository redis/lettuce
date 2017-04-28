Lettuce 5.0.0 M2 RELEASE NOTES
==================================

This is the second milestone release for Lettuce 5.0. We made a few breaking changes in this release:
 
 1. We moved the artifact coordinates from biz.paluch.redis:lettuce to io.lettuce:lettuce-core
 2. We relocated packages from biz.paluch.redis to io.lettuce.core. The migration path is
    straight-forward by replacing the old package name in your imports with the new package name.
 3. No one (not even me) knew whether it was lettuce or Lettuce when mentioning the driver
    on a website, mail or some documentation. We fixed that: Lettuce is now written with an uppercase L. 
    We also have a beautiful logo and project website at https://lettuce.io.
 4. We removed some deprecated methods, see below for full details.
 
Besides the breaking changes, this release comes with 96 completed issues.

This release introduces a new reference guide that is shipped along the regular artifacts. The reference guide
is bound to a particular version and does not change over time, such as the Wiki.

Lettuce defaults to native transport (epoll) on Linux systems if the epoll dependency is available.
You can disable this behavior by setting the system property `io.lettuce.core.epoll` to `false`.

Browse the docs at https://lettuce.io/core/snapshot/reference/.


Command Interface Batching
--------------------------
Command interfaces support command batching to collect multiple commands in a batch queue and 
flush the batch in a single write to the transport. Command batching executes commands in a 
deferred nature. This means that at the time of invocation no result is available.
 Batching can be only used with synchronous methods without a return value (void) or asynchronous 
 methods returning a RedisFuture.

Command batching can be enabled on two levels:

* On class level by annotating the command interface with @BatchSize. 
  All methods participate in command batching.
* On method level by adding CommandBatching to the arguments. 
  Method participates selectively in command batching.

```java
@BatchSize(50)
interface StringCommands extends Commands {

    void set(String key, String value);

    RedisFuture<String> get(String key);

    RedisFuture<String> get(String key, CommandBatching batching);
}

StringCommands commands = …

commands.set("key", "value"); // queued until 50 command invocations reached.
                              // The 50th invocation flushes the queue.

commands.get("key", CommandBatching.queue()); // invocation-level queueing control
commands.get("key", CommandBatching.flush()); // invocation-level queueing control,
                                              // flushes all queued commands
```

Read more: https://github.com/lettuce-io/lettuce-core/wiki/Redis-Command-Interfaces#batch-execution

Asynchronous Connection Propagation
-----------------------------------

Lettuce is inherently non-blocking, yet we synchronize in some places that causes blocking behavior. 
Cluster node connections is one of these areas.
Commands dispatched to a cluster node require a connection to be dispatched. We connected, waited until
the connection is available and then we proceeded. This synchronization keeps threads busy.

With this release, we are fully non-blocking. Cluster node connections are obtained now asynchronously and
we use the connection state (via `CompletableFuture`) to asynchronously dispatch commands.

Asynchronous connection propagation solves a few issues such as netty deadlocks if cluster commands are
sent within a future callback or in a reactive sequence.


Thanks to all contributors that made Lettuce 5.0.0 possible.

Lettuce 5.0.0 requires Java 8 and cannot be used with Java 6 or 7.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/


Commands
--------
* Adopt fixed GEODIST/GEOHASH behavior in tests #362
* Support CLIENT and INFO commands for Redis Sentinel #415
* Disable WAIT as Read-Only command #418

Enhancements
------------
* Use MethodHandle.Lookup.findSpecial(…) for default method lookup #310
* Use StringCodec from Utf8StringCodec #393
* Add reactive wrapper converters for RxJava1 and 2 #383
* Provide synchronous ScanIterator API #397
* Support value ranges #384
* Allow Pub/Sub subscriptions on a node selection within Redis Cluster #385
* [BREAKING] Remove deprecated LettuceFutures.await method #388
* [BREAKING] Remove ExperimentalByteArrayCodec and enhance ByteArrayCodec #389
* Propagate backpressure to the transport channel #394
* Default to epoll transport if available #402
* Simplify NodeSelection implementation #408
* [BREAKING] Remove addListener/removeListener methods from execution-specific Pub/Sub interfaces #412
* [BREAKING] Remove close method from Redis Sentinel command interfaces #415
* Allow setting client name with RedisURI #416
* Extend RedisConnectionStateListener.onRedisConnected by adding SocketAddress #419
* Decompose array parameters into single command arguments #421
* Consider key/value type of parameter wrappers in codec resolution #423
* Connect asynchronously in cluster topology refresh #424
* Perform resilient Master/Slave topology refresh ping #424
* Return ClusterClientOptions.Builder in socketOptions and sslOptions methods #425
* Use ResolvableType for type resolution #426
* Propagate cause if cluster topology refresh can't connect to any node #427
* Propagate asynchronous connection initialization #429
* Dynamic API: Support command batching #432
* Reuse stale connections collection when closing stale connections #443
* Add ReadFrom.SLAVE_PREFERRED #452
* Share cluster node connection progress with request threads #453
* Send collection of commands as batch through StatefulConnection and ChannelWriter #456
* Use EnumSet to determine Read-Only commands #457
* Encapsulate fields in Commands implementations and command builder #458
* Refactor command creation and execution to ExecutableCommand creation #463
* Use String-Codec for COMMAND #467
* Create a static call chain in InvocationProxyFactory #468
* Initialize RedisStateMachine.LongProcessor in static initializer #481 (Thanks to @hellyguo)
* Add shutdownAsync to AbstractRedisClient #485 (Thanks to @jongyeol)
* Propagate asynchronous connections to ClusterConnectionProvider #486 (@Thanks to @jeniok)
* Resolve hostnames using netty's DNS resolver #498
* Introduce NettyCustomizer API #499 (Thanks to @jongyeol)
* Use asynchronous node connect in NodeSelection #500
* Gracefully degrade to successfully connected Cluster read candidates #503 (Thanks to @eliotw)
* Implement counting Deque #507 (Thanks to @CodingFabian)
* Use ThreadLocalRandom instead of Random #510
* Replace consumer completion List in CommandWrapper with Array #511
* Consider ConnectionFuture state in ClusterDistributionChannelWriter #511
* Replace first arg fields in CommandArgs with CommandArgsAccessor #511
* Merge Command completion state fields to a single status field #511
* Move command latency metrics to LatencyMeteredCommand #511
* Close the connection when Redis protocol is corrupted #512 (Thanks to @jongyeol)
* Allow setting the Redis password as char-array #513
* Replace synchronized reconnect in ConnectionWatchdog with asynchronous connect #514
* Improve cleanup of PauseDetector #516
* Upgrade AsyncExecutions to use CompletableFuture #522

Fixes
-----
* Update code samples in readme to Lettuce 4.0 #410
* Apply proxy wrapper to obtained pooled connections #411
* Allow databases greater than 15 in RedisURI #420
* Consider Wildcard types in type information and assignability #431
* Expose a protected default constructor for RedisClusterClient #438
* Ensure RoundRobinSocketAddressSupplier consistency before returning a SocketAddress #440
* Fix Partitions.addPartition(…) and Partitions.reload(…) synchronization #442
* Fix authenticated PING before connect with Redis Sentinel #448
* Close connections in PooledClusterConnectionProvider.close() #460
* Read connect future in ReconnectHandler to local variable #465
* Apply command timeout to PING before connection activation #470
* Synchronize ClientResources shutdown in client shutdown #475
* Skip SO_KEEPALIVE and TCP_NODELAY options for Unix Domain Socket connections #476 (Thanks to @magdkudama)
* Fix cummulative metrics collection #487 (Thanks to @ameenhere)
* Stop LatencyStats on removal #517
* Guard PauseDetectorWrapper initialization against absence of LatencyUtils #520

Other
------
* Upgrade to Java 9-compatible build #310
* Mark NodeSelection API as stable #389
* Document RedisClient and RedisClusterClient implementation hooks #392
* Upgrade to netty 4.1.7 #436
* Fix ClientResources usage in tests #445
* Enable JMH tests on TravisCI #454
* Create benchmarks for RedisClient, RedisClusterClient and ClusterDistributionChannelWriter #454
* Use Reactor's StepVerifier in tests #455
* Upgrade to TravisCI trusty #461
* Upgrade to Mockito 2.7.5 #469
* Improve JavaDoc #472
* Upgrade dependencies #474
* Switch optional dependencies from provided to optional #482
* Use appropriate EventLoopGroup in SingleThreadedReactiveClusterClientTest #486
* Update Netty version to 4.1.9 #489 (Thanks to @odiszapc)
* Calculate first response metrics from sent time #491
* [BREAKING] Move to lettuce.io #493
* Create reference documentation #494
* Switch to codecov #504
* Upgrade to Reactor 3.1.0 SNAPSHOT #505
* Upgrade integrations from mp911de/lettuce to lettuce-io/lettuce-core #506
* Upgrade to Project Reactor 3.1.0.M1 #523
* Upgrade to RxJava 1.2.10 #524
* Upgrade to RxJava2 2.0.9 #525
* Switch jcl-over-slf4j dependency to test scope #526

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
