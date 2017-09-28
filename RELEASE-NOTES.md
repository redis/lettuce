Lettuce 5.0.0 RELEASE NOTES
===========================

After a 13 months development phase and 208 solved tickets, 
it is my pleasure to announce general availability of Lettuce 5.0.

This is a major release coming with several breaking changes and new interesting
features Java 9 compatibility. 

Lettuce 5 introduces the dynamic Redis Commands API. This programming
model enables you to declare command methods and invoke commands to your needs and
support Redis Modules without waiting for Lettuce to support new commands.

Lettuce defaults to native transport (epoll, kqueue) on Linux respective macOS systems 
if the native dependency is available. You can disable this behavior by setting the 
system property `io.lettuce.core.epoll` and `io.lettuce.core.kqueue` to `false`.

Lettuce 5 comes with breaking changes; it removes deprecated interfaces 
RedisConnection and RedisAsyncConnection and their segregated interfaces in 
favor of StatefulRedisConnection and RedisCommands et al.

Major breaking changes:

 1. We moved the artifact coordinates from biz.paluch.redis:lettuce to io.lettuce:lettuce-core
 2. We relocated packages from biz.paluch.redis to io.lettuce.core. The migration path is
    straight-forward by replacing the old package name in your imports with the new package name.
 3. The documentation has moved from http://redis.paluch.biz to https://lettuce.io.
 4. Removed Guava.
 5. We removed some deprecated methods, see below for full details.

Lettuce requires only Netty and Project Reactor which brings us to the next change. 
The reactive API is based on Reactive Streams by using 
Project Reactor types `Mono` and `Flux` instead of RxJava 1 and `Observable`.
If you require RxJava's `Single` and `Observable` in your code, 
then use publisher adapters in `rxjava-reactive-streams` to adapt `Mono` and `Flux`.


This release introduces a new reference guide that is shipped along the regular artifacts. 
The reference guide is bound to a particular version and does not change over time, 
such as the Wiki.

Reference documentation: https://lettuce.io/core/release/reference/.
JavaDoc documentation: https://lettuce.io/core/release/api/.

```xml
<dependency>
  <groupId>io.lettuce</groupId>
  <artifactId>lettuce-core</artifactId>
  <version>5.0.0.RELEASE</version>
</dependency>
```

You can find the full change log, containing all changes since the first 5.0 milestone release,
at the end of this document. Watch out for BREAKING changes.

Thanks to all contributors that made Lettuce 5 possible.


Dynamic Redis Commands API
--------------------------

The Redis Command Interface abstraction provides a dynamic way for typesafe Redis 
command invocation. It allows you to declare an interface with command methods to 
significantly reduce boilerplate code required to invoke a Redis command.

Redis is a data store supporting over 190 documented commands and over 450 
command permutations.  Command growth and keeping track with upcoming 
modules are challenging for client developers and Redis user as there is no 
full command coverage for each module in a single Redis client.

Invoking a custom command with Lettuce requires several lines of code to 
define command structures pass in arguments and specify the return type.

```java
RedisCodec<String, String> codec = new StringCodec();
RedisCommands<String, String> commands = ...

String response = redis.dispatch(CommandType.SET, new StatusOutput<>(codec),
                new CommandArgs<>(codec)
                       .addKey(key)
                       .addValue(value));
```

The central interface in Lettuce Command Interface abstraction is `Commands`. 
This interface acts primarily as a marker interface to help you to discover 
interfaces that extend this one. You can declare your own command interfaces 
and argument sequences where the command name is derived from the method name or 
provided with `@Command`. Introduction of new commands does not require you 
to wait for a new Lettuce release but they can invoke commands through own declaration. 
That interface could be also supporting different key and value types, depending on the use-case.

Commands are executed synchronously, asynchronous or with a reactive execution model, 
depending on the method declaration.

```java
public interface MyRedisCommands extends Commands {

    String get(String key); // Synchronous Execution of GET

    @Command("GET")
    byte[] getAsBytes(String key); // Synchronous Execution of GET returning data as byte array

    @Command("SET") // synchronous execution applying a Timeout
    String setSync(String key, String value, Timeout timeout);

    Future<String> set(String key, String value); // asynchronous SET execution

    @Command("SET")
    Mono<String> setReactive(String key, String value); // reactive SET execution using SetArgs

    @CommandNaming(split = DOT) // support for Redis Module command notation -> NR.RUN
    double nrRun(String key, int... indexes);
}

RedisCommandFactory factory = new RedisCommandFactory(connection);

MyRedisCommands commands = factory.getCommands(MyRedisCommands.class);

String value = commands.get("key");
```

You get a whole lot new possibilities with Redis Command Interfaces. 
One of them is transparent reactive type adoption. Lettuce's reactive API is based on Reactive Streams, 
however with command interfaces you can declare a RxJava 1 or RxJava 2 return 
type and Lettuce will handle the adoption for you. 
RxJava 1 users have a migration path that allows using native types without
further conversion.

See also: https://lettuce.io/core/5.0.0.RELEASE/reference/#redis-command-interfaces

Command Interface Batching
--------------------------

Command interfaces support command batching to collect multiple commands in a 
batch queue and flush the batch in a single write to the transport. 
Command batching executes commands in a deferred nature. This means that 
at the time of invocation no result is available. Batching can be only used 
with synchronous methods without a return value (void) or asynchronous 
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

Read more: https://lettuce.io/core/5.0.0.RELEASE//reference/#command-interfaces.batch


Migration to Reactive Streams (Project Reactor)
-----------------------------------------------

Lettuce 4.0 introduced a reactive API based on RxJava 1 and `Observable`.
This was the beginning of reactive Redis support. Lettuce used `Observable`
all over the place as other reactive types like `Single` and `Completable` were
still beta or in development.

Since that time, a lot changed in the reactive space. RxJava 2 is the successor
of RxJava 1 which has now reached end of life. RxJava 2 is not entirely 
based on Reactive Streams and baselines to Java 6 while other composition
libraries can benefit from a Java 8. 

This also means, no `null` values and usage of dedicated value types to express
value multiplicity (`0|1` and `0|1|N`) on the API.

With Lettuce 5.0, the reactive API uses Project Reactor with
its `Mono` and `Flux` types.

**4.3 and earlier**

```java
Observable<Long> del(K... keys);

Observable<K> keys(K pattern);

Observable<V> mget(K... keys);
```

**5.0**

```java
Mono<Long> del(K... keys);

Flux<K> keys(K pattern);

Flux<KeyValue<K, V>> mget(K... keys);
```

Switching from RxJava 1 to Project Reactor use requires switching the library. Most
operators use similar or even same names. If you're required to stick to RxJava 1,
the use `rxjava-reactive-streams` to adopt reactive types (RxJava 1 <-> Reactive Streams).

Migrating to Reactive Streams requires value wrapping to indicate absence of values.
You will find differences in comparison to the previous API and to the sync/async API
in cases where commands can return `null` values. Lettuce 5.0 comes with
new `Value` types that are monads encapsulating a value (or their absence).

See also: https://lettuce.io/core/5.0.0.RELEASE/reference/#reactive-api


Value, KeyValue, and other value types
--------------------------------------

The reactive story facilitates immutable types so this release enhances existing value 
types and introduces new types to reduce `null` usage and facilitate functional programming. 

Value types are based on `Value` and `KeyValue`/`ScoredValue` extend from there.
Value is a wrapper type encapsulating a value or its absence. A `Value` can
be created in different ways:

```java
Value<String> value = Value.from(Optional.of("hello"));

Value<String> value = Value.fromNullable(null);

Value<String> value = Value.just("hello");

KeyValue<Long, String> value = KeyValue.from(1L, Optional.of("hello"));
 
KeyValue<String, String> value = KeyValue.just("key", "hello");
```

It transforms to `Optional` and `Stream` to integrate with other 
functional uses and allows value mapping.

```java
Value.just("hello").stream().filter(…).count();

KeyValue.just("hello").optional().isPresent();

Value.from(Optional.of("hello")).map(s -> s + "-world").getValue();

ScoredValue.just(42, "hello").mapScore(number -> number.doubleValue() * 3.14d).getScore();
```

You will also find that all public fields of value types are encapsulated with
getters and these fields are no longer accessible.


Backoff/Delay strategies
------------------------

_Thanks to @jongyeol_

When running cloud-based services with a multitude of services that use Redis,
then network partitions impact Redis server connection heavily once 
the partition ends. A network partition impacts all disconnected applications 
at the same time and all nodes start reconnecting more or less at the same time.

As soon as the partition ends, the majority of applications reconnect at the same time.
Jitter backoff strategies leverage the impact as the time of reconnecting is randomized.

Lettuce comes with various backoff implementations:

* Equal Jitter
* Full Jitter
* Decorrelated Jitter

These are configured in `ClientResources`:

```java
DefaultClientResources.builder()
        .reconnectDelay(Delay.decorrelatedJitter())
        .build();

DefaultClientResources.builder()
        .reconnectDelay(Delay.equalJitter())
        .build();
```

See also: https://www.awsarchitectureblog.com/2015/03/backoff.html and
https://lettuce.io/core/5.0.0.RELEASE/reference/#clientresources.advanced-settings


New API for Z...RANGE commands
-------------------------------

Sorted Sets range commands come with a streamlined API regarding method overloads.
Commands like `ZRANGEBYSCORE`, `ZRANGEBYLEX`, `ZREMRANGEBYLEX` and several others
now declare methods accepting `Range` and `Limit` objects instead of an growing
parameter list. The new `Range` allows score and value types applying the proper
binary encoding.

**4.2 and earlier**

```java
commands.zcount(key, 1.0, 3.0)

commands.zrangebyscore(key, "-inf", "+inf")

commands.zrangebyscoreWithScores(key, "[1.0", "(4.0")

commands.zrangebyscoreWithScores(key, "-inf", "+inf", 2, 2)
```

**Since 5.0**

```java
commands.zcount(key, Range.create(1.0, 3.0));

commands.zrangebyscore(key, Range.unbounded());

commands.zrangebyscoreWithScores(key, Range.from(Boundary.including(1.0), Boundary.excluding(4.0));

commands.zrangebyscoreWithScores(key, Range.unbounded(), Limit.create(2, 2));
```

Good bye, Guava
---------------

Lettuce 5.0 no longer uses Google's Guava library. Guava was a good friend
back in the Java 6-compatible days where `Future` synchronization and callbacks
were no fun to use. That changed with Java 8 and `CompletableFuture`.

Other uses like `HostAndPort` or `LoadingCache` could be either inlined or
replaced by Java 8's Collection framework.


Removal of deprecated interfaces and methods
--------------------------------------------

This release removes deprecated interfaces `RedisConnection` and `RedisAsyncConnection`
and their segregated interfaces in favor of `StatefulRedisConnection` and `RedisCommands`.
 
You will notice slight differences when using that API. Transactional commands and 
database selection are no longer available through the Redis Cluster API as the 
old API was derived from the standalone API. `RedisCommands` and `RedisAsyncCommands`
are no longer `Closeable`. Please use `commands.getStatefulConnection().close()` to 
close a connection. This change removes ambiguity over closing the commands interface
over closing the connection.

Connection pooling replacement
------------------------------

It took quite a while but 4.3 deprecated Lettuce's existing connection pooling
support. That are in particular `RedisClient.pool(…)` and `RedisClient.asyncPool(…)`. 
These methods are removed with Lettuce 5.0.

Connection pooling had very limited support and would require additional overloads
that clutter the API to expose pooling for all supported connections.
This release brings a replacement, that is customizable
and does not pollute the API. `ConnectionPoolSupport` provides methods to
create a connection pool accepting a factory method and pool configuration.

Returned connection objects are proxies that return the connection to its pool
when calling `close()`. `StatefulConnection` implement `Closeable` to
allow usage of try-with-resources.

```java
GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
        .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig());


try(StatefulRedisConnection<String, String> connection = pool.borrowObject()) {
    // Work
}

pool.close();
```


Redis Cluster topology refresh consensus
----------------------------------------

Cluster topology refreshing can lead in some cases (dynamic topology sources)
to orphaning. This can happen if a cluster node is removed from the cluster and
lettuce decides to accept the topology view of that removed node. Lettuce 
gets stuck with that node and is not able to use the remaining cluster.

This release introduces `PartitionsConsensus` strategies to determine the most
appropriate topology view if multiple views are acquired. The strategy can be
customized by overriding 
`RedisClusterClient.determinePartitions(Partitions, Map<RedisURI, Partitions>)`.

Lettuce defaults choosing the topology view with the majority of previously known
cluster nodes. This helps Lettuce to stick with the cluster that consists of the
most nodes.
 
See also: https://github.com/lettuce-io/lettuce-core/issues/355


Asynchronous Connections in Redis Cluster
-----------------------------------------

RedisClusterClient now connects asynchronously without intermediate 
blocking to cluster nodes. The connection progress is shared between
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


Thanks to all contributors that made Lettuce 5.0.0.RELEASE possible.

Lettuce 5.0.0.RELEASE requires Java 8 and cannot be used with Java 6 or 7.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/


Commands
--------
* Add support for TOUCH command #270
* Add support for variadic LPUSHX and RPUSHX #267
* Adopt fixed GEODIST/GEOHASH behavior in tests #362
* Provide a more conceise API for Sorted Set query operations using Range/Limit #363
* Add EVAL and EVALSHA to ReadOnlyCommands #366 (Thanks to @amilnarski)
* Add support for ZADD INCR with ZAddArgs #367 (Thanks to @christophstrobl)
* Add support for ZREVRANGEBYLEX command #369 (Thanks to @christophstrobl)
* Add support for SWAPDB #375
* Support CLIENT and INFO commands for Redis Sentinel #415
* Disable WAIT as Read-Only command #418

Enhancements
------------
* [BREAKING] Remove deprecated interfaces and methods #156
* [BREAKING] Remove Google Guava usage #217
* Add support for OpenSSL as SSL provider #249
* Add SocketOptions to ClientOptions #269
* [BREAKING] Replacement support for connection pooling #264
* Add ToByteBufEncoder and improved StringCodec #275
* Allow configuration of a trust store password #292
* Replace Guava Cache by ConcurrentHashMap #300
* Eager initialization of API wrappers in stateful connections #302 (Thanks to @jongyeol)
* Change MethodTranslator's loadfactor to 1.0 for sync APIs performance #305 (Thanks to @jongyeol)
* Reattempt initially failed Sentinel connections in Master/Slave API #306
* Use MethodHandle.Lookup.findSpecial(…) for default method lookup #310
* Decouple CommandHandler #317
* [BREAKING] Use TransactionResult for transaction results #320
* Replace synchronized setters with volatile fields #326 (Thanks to @guperrot)
* Add workaround for IPv6 parsing #332
* Add ConnectionWatchdog as last handler #335
* Provide multi-key-routing for exists and unlink commands using Redis Cluster #334
* [BREAKING] Migrate lettuce reactive API to Reactive Streams #349
* Provide Value types #350
* Provide Timer as part of ClientResources #354 (Thanks to @plokhotnyuk)
* Add support for topology consensus #355
* Use Limit in SortArgs #364
* Add jitter backoff strategies for spreading reconnection timing #365 (Thanks to @jongyeol)
* Accept double in ZStoreArgs.weights #368 (Thanks to @christophstrobl)
* Consider role changes as trigger for update using MasterSlave connections #370
* Provide a dynamic Redis Command API #371
* Support lettuce parameter types #381
* Add reactive wrapper converters for RxJava1 and 2 #383
* Support value ranges #384
* Allow Pub/Sub subscriptions on a node selection within Redis Cluster #385
* Expose Value.map methods #386
* [BREAKING] Remove deprecated LettuceFutures.await method #388
* [BREAKING] Remove ExperimentalByteArrayCodec and enhance ByteArrayCodec #389
* Use StringCodec from Utf8StringCodec #393
* Provide synchronous ScanIterator API #397
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
* [BREAKING] Use String-Codec for COMMAND #467
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
* [BREAKING] Drop netty 4.0 compatibility #518
* Upgrade AsyncExecutions to use CompletableFuture #522
* Support default methods on Command Interfaces #527
* Improve Epoll availability detection #535
* Align log prefix for CommandHandler and ConnectionWatchdog #538
* Add support for kqueue transport #539
* Add support for client certificate authentication #540
* Adopt to changed SLOWLOG output #551
* Use pre-instantiated ByteBufProcessor in RedisStateMachine to find EOL #557
* SlotHash.getSlot(…) duplicates bytes while slot calculation #558
* Replace CountDownLatch in AsyncCommand with AtomicFieldUpdater #559
* Expose asynchronous connection creation API #561
* [BREAKING] Refactor CONFIG GET output from List<String> to Map<String, String> #562
* clientSetname does not set client name on default connection #563 
* Adopt to read-only variant GEORADIUS(BYMEMBER)_RO #564
* [BREAKING] Switch to Duration for timeouts #570
* Optimization for ValueListOutput #573 (Thanks to @CodingFabian)
* Enhance argument caching #575
* Introduce MethodTranslator caching #580
* Execute RedisAdvancedClusterCommands.scriptLoad(…) on all nodes #590
* Reduce default shutdown timeout #613

Fixes
-----
* Fix JavaDoc for blocking list commands #272
* Guard key parameters against null values #287
* CommandArgs.ExperimentalByteArrayCodec fails to encode bulk string #288
* Guard value parameters against null values #291 (Thanks to @christophstrobl)
* Allow coordinated cross-slot execution using Iterable #303 (Thanks to @agodet)
* Allow MasterSlave connection using Sentinel if some Sentinels are not available #304 (Thanks to @RahulBabbar)
* Use at least 3 Threads when configuring default thread count #309
* Replace own partition host and port only if the reported connection point is empty #312
* Lettuce RedisClusterClient calls AUTH twice #313
* Record sent-time on command queueing #314 (Thanks to @HaloFour)
* CommandHandler notifications called out of order #315
* [BREAKING] Disable SYNC command #319
* ASK bit not set on ASK redirection #321 (Thanks to @kaibaemon)
* Check for isUnsubscribed() before calling subscriber methods #323 (Thanks to @vleushin)
* Avoid timeouts for cancelling command #325 (Thanks to @jongyeol)
* Guard command completion against exceptions #331
* Store error at output-level when using NestedMultiOutput #328 (Thanks to @jongyeol)
* Fix master and slave address parsing for IPv6 addresses #329 (Thanks to @maksimlikharev)
* Fix srandmember return type from Set to List #330 (Thanks to @jongyeol)
* Add synchronization to Partitions/Use read-view for consistent Partition usage during Partitions updates #333 (Thanks to @OutOfBrain)
* Run cluster command redirects on event executor threads #340 (Thanks to @oklahomer)
* Close connections in PooledClusterConnectionProvider on connection failures #343
* Consider number of redirections instead of executions for cluster commands #344 (Thanks to @Spikhalskiy)
* Ensure unique reconnect scheduling #346
* Guard ConnectionWatchdog against NPE from missing CommandHandler #358
* Fix RedisAdvancedClusterAsyncCommandsImpl.msetnx return value #376 (Thanks to @mjaow)
* Allow hostnames in MasterSlaveTopologyProvider when parsing in master_host #377 (Thanks to @szabowexler)
* Allow empty values in BITFIELD using the reactive API #378
* Support integer width multiplied offsets in BITFIELD #379 (Thanks to @christophstrobl)
* Propagate array sizes in MultiOutput #380 (Thanks to @christophstrobl)
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
* Connection pool deadlock #531 (Thanks to @DarkSeraphim)
* NumberFormatException on `inf` zscore #528 (Thanks to @DarkSeraphim)
* Fix FastCountingDeque not recording correct size. #544 (Thanks to @kojilin)
* Lettuce doesn't call QUIT when a pooled connection is destroyed. #545 (Thanks to @robbiemc)
* NPE in RedisStateMachine #576 (Thanks to @nikolayspb)
* Prevent dead listener callbacks in shutdown sequence #581
* Javadoc in BitFieldArgs contains invalid links #583
* Fix IllegalArgumentException in RedisClient.connectSentinel #588 (Thanks to @andrewsensus)
* UnsupportedOperationException (List#add) in NestedMultiOutput #589 (Thanks to @zapl)
* GEOPOS fails with a single member in the var args #591 (Thanks to @FerhatSavci)

Other
------
* Upgrade to stunnel 5.33 #290
* Upgrade to Java 9-compatible build #310
* Upgrade logging to log4j2 for tests #316
* Upgrade to AssertJ 3.5.2 #352
* Add test to verify behavior of GEODIST if a geoset is unknown #362
* Update license/author headers #387
* Upgrade to netty 4.0.42.Final/4.1.6.Final 390
* Mark NodeSelection API as stable #389
* Document RedisClient and RedisClusterClient implementation hooks #392
* Upgrade to Project Reactor 3.0.3 #395
* Disable shaded jar #396
* Upgrade to netty 4.1.7 #436
* Fix ClientResources usage in tests #445
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
* Upgrade integrations from lettuce-io/lettuce-core to lettuce-io/lettuce-core #506
* Upgrade to Project Reactor 3.1.0.M1 #523
* Upgrade to RxJava 1.2.10 #524
* Upgrade to RxJava2 2.0.9 #525
* Switch jcl-over-slf4j dependency to test scope #526
* Upgrade to netty 4.1.10 #532
* Upgrade to netty 4.0.11 #537
* Upgrade to netty 4.1.12 (4.0.48) #548
* Upgrade to Reactor 3.1 M2 #549
* Allow Redis version pinning for build #552
* Upgrade to netty 4.1.13/4.0.49 #565
* Upgrade to Reactor 3.1.0 M3 (Bismuth M3) #567
* Use BOM to manage netty and Spring dependencies #568
* Upgrade dependencies #569
* Upgrade to Reactor Core 3.1.0 RC1 #602
* Upgrade to netty 4.1.15 #600
* Release Lettuce 5.0 GA #607
* Extend documentation #610

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
