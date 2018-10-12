Lettuce 5.1.0 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.1.0 release! 
This release contains new features, bugfixes, and enhancements.
 
Most notable changes are:

* Support for Redis Streams
* Asynchronous pool implementation
* `SCAN` streams, and initial 
* Global command timeouts (disabled by default)
* Asynchronous connect methods for Master/Replica and Sentinel
* Brave (OpenZipkin) tracing support
* Lettuce build is now Java 11 compatible

We started efforts to adapt improvements in Redis terminology about Master/Replica 
with this release. This release ships documentation changes referring to the 
existing API as Master/Replica where possible.

We will introduce with the upcoming releases support for the new API methods 
(`REPLICAOF`) without breaking the public API. Expect breaking changes in a much 
later release which gives plenty of time to upgrade. 

Find the full change log at the end of this document that lists all 108 tickets.

Thanks to all contributors who made Lettuce 5.1.0.RELEASE possible.

Lettuce requires a minimum of Java 8 to build and run and is compatible with Java 11. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.1.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.1.0.RELEASE/api/


New Exceptions for Redis Responses
----------------------------------

This release introduces new Exception types for the following Redis responses:

* `LOADING`: `RedisLoadingException`
* `NOSCRIPT`: `RedisNoScriptException`
* `BUSY`: `RedisBusyException`

All exception types derive from `RedisCommandExecutionException` and do not 
require changes in application code.


Redis Streams
-----------------------
Redis 5.0 is going to ship with support for a Stream data structure. 
A stream is a log of events that can be consumed sequentially. A Stream 
message consists of an id and a body represented as hash (or `Map<K, V>`).

Lettuce provides access to Stream commands through `RedisStreamCommands` supporting
synchronous, asynchronous, and reactive execution models. All Stream commands
are prefixed with `X` (`XADD`, `XREAD`, `XRANGE`).

Stream messages are required to be polled. Polling can return either in a non-blocking
way without a message if no message is available, or, in a blocking way.
`XREAD` allows to specify a blocking duration in which the connection is blocked
until either the timeout is exceeded or a Stream message arrives.

The following example shows how to append and read messages from a Redis Stream:

```java
// Append a message to the stream
String messageId = redis.xadd("my-stream", Collections.singletonMap("key", "value"));

// Read a message
List<StreamMessage<String, String>> messages = redis.xread(StreamOffset.from("my-stream", messageId));


redis.xadd("my-stream", Collections.singletonMap("key", "value"));

// Blocking read
List<StreamMessage<String, String>> messages = redis.xread(XReadArgs.Builder.block(Duration.ofSeconds(2)), 
                                                           StreamOffset.latest("my-stream"));
```

Redis Streams support the notion of consumer groups. A consumer group is a group of 
one or more consumers that tracks the last consumed Stream message and allows 
explicit acknowledgment of consumed messages. 

```java
// Setup stream, see https://github.com/antirez/redis/issues/4824
redis.xadd("my-stream", Collections.singletonMap("key", "value"));

// Create consumer group
redis.xgroupCreate("my-stream", "my-group", "$");
redis.xadd("my-stream", Collections.singletonMap("key", "value"));

// Read stream messages in the context of a consumer
List<StreamMessage<String, String>> messages = redis.xreadgroup(Consumer.from("my-stream", "consumer1"),
                XReadArgs.Builder.noack(),
                StreamOffset.lastConsumed(key));

// process message

…

// Acknowledge message
redis.xack(key, "group", messages.get(0).getId());
```

Scan Stream
-----------

Lettuce 5.1 ships with a reactive `SCAN` stream API that allows reactive and
demand-aware usage of Redis' `SCAN` commands. `SCAN` is an interator-based command
that requires multiple round-trips to Redis to scan the keyspace or 
a particular data structure. Instead of calling `SCAN` from your application code, 
you can use `ScanStream` as entrypoint to `SCAN`, `HSCAN`, `SSCAN`, and `ZSCAN` operations.
`ScanStream` returns a `Flux` that can be consumed as a single stream without
the need of continuing the actual iteration. That's covered by Lettuce for you.

The `LIMIT` argument controls the batchsize. Demand (backpressure) is translated
into cursor call. If you stop consuming the stream, no further `SCAN` calls are issued.
 
```java
RedisReactiveCommands<String, String> reactive = redis.getStatefulConnection().reactive();

Flux<String> keys = ScanStream.scan(reactive, ScanArgs.Builder.limit(200));
```

Asynchronous non-blocking Connection Pooling
--------------------------------------------

Right now, applications that utilize connection pooling in combination with a 
non-blocking API (asynchronous or reactive API users) are limited to Apache Commons Pool
which is a blocking object pool implementation.

This release ships with non-blocking pooling support through `AsyncPool` that
is obtained from `AsyncConnectionPoolSupport`. The pooling API allows for various
implementations. As of Lettuce 5.1, a bounded pool is provided that allows 
limiting and that signals pool exhaustion by emitting `NoSuchElementException`.

The pooling API returns `CompletableFuture` instances to synchronize acquire and 
release operations. `AsyncPool` does not require external dependencies and features
an improved performance profile in comparison to Apache Commons Pool 2. 
This non-blocking pooling API is not a replacement for the blocking pool 
although it's possible that may provide a blocking layer that exports 
the pooling functionality through `org.apache.commons.pool2.ObjectPool`. 

The following example show how to use `AsyncPool`. 

```java
AsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
        () -> client.connectAsync(StringCodec.UTF8, uri),
        BoundedPoolConfig.builder()
                    .minIdle(4)
                    .maxIdle(8)
                    .maxTotal(16)
                    .testOnAcquire()
                    .build());

// Acquire connection and do some work
CompletableFuture<StatefulRedisConnection<String, String>> acquire = pool.acquire();

acquire.thenAccept(connection -> {

    connection.async().set("key", "value").whenComplete((v, e) -> {
        pool.release(connection);
    });
});


// later
CompletableFuture<Void> closeFuture = pool.closeAsync();
```

Global Command Timeouts
-----------------------

Lettuce can be used through various APIs. The most common one is the
synchronous API which emulates blocking behavior on top of a reactive 
and non-blocking driver. A natural aspect of blocking APIs are timeouts
when it comes to I/O. Using the asynchronous or reactive API do not have
timeouts as there was no active component that tracked command runtime durations.

With Lettuce 5.1, you get the possibility to configure a global command timeout
through `ClientOptions` and `TimeoutOptions` that can determine timeouts
on a `RedisCommand` basis. This timeout applies on a command basis
and completes commands with `RedisCommandTimeoutException`.  

You can change the timeout on running connections through `RedisURI.setTimeout(…)`, 
`RedisClient.setDefaultTimeout(…)`, and `StatefulConnection.setTimeout(…)` if
`TimeoutOptions` are enabled - global command timeouts are disabled by default. 

```java
TimeoutOptions timeoutOptions = TimeoutOptions.enabled(Duration.ofMinutes(1));

ClientOptions.builder().timeoutOptions(timeoutOptions).build();
```

Add tracing support using Brave (OpenZipkin)
--------------------------------------------

Lettuce now supports command tracing using Brave. 
Tracing can be configured through `ClientResources`. The parent span is propagated either 
through `Tracing.currentTracer()` (synchronous/asynchronous usage) or 
by registering a Brave Span in the Reactor context when using Reactive execution.

Lettuce wraps Brave data models to support tracing in a vendor-agnostic way if 
Brave is on the class path.

Usage example:

```java
Tracing tracing = …;

ClientResources clientResources = ClientResources.builder().tracing(BraveTracing.create(tracing)).build();

RedisClient client = RedisClient.create(clientResources, redisUri);

StatefulRedisConnection<String, String> connection = client.connect();

connection.sync().get(…) // Tracing tries to obtain the current tracer from Tracing.currentTracer()

connection.reactive().get(…) // Tracing tries to obtain the current tracer from Reactor's Context
```

A note on reactive Tracing: Reactive Tracing with Brave requires either a Span or a 
TraceContext object to be available in Reactor's Context.

Commands
--------
* Add AUTH option to MIGRATE command #733
* Add MASTER type to KillArgs #760
* Add support for ZPOPMIN, ZPOPMAX, BZPOPMIN, BZPOPMAX commands #778
* Add REPLACE option to RESTORE. #783 (Thanks to @christophstrobl)
* Add XGROUP DESTROY #789
* Add support for CLIENT UNBLOCK #812
* Support for approximate trimming in XAddArgs #846

Enhancements
------------
* Support for command timeouts (async, reactive) #435
* Use object pooling for collections inside a single method and Command/CommandArgs with a small scope #459
* Cancel commands after disconnect in at-most-once mode. #547
* Add support for Redis streams #606
* Introduce dedicated exceptions for NOSCRIPT and BUSY responses #620 (Thanks to @DaichiUeura)
* Non-blocking connection pooling #631
* Introduce fast-path publishing in RedisPublisher #637
* Add reactive scanning #638
* Asynchronous connection initialization #640
* Create reusable abstraction for non-blocking and keyed connection provider #642
* Expose asynchronous connect method for Master/Slave connections #643
* Add SocketAddressOutput to directly parse SENTINEL get-master-addr-by-name output #644
* Misleading wasRolledBack method #662 (Thanks to @graineri)
* Read from random slave preferred #676 (Thanks to @petetanton)
* Introduce exception to represent Redis LOADING response #682
* Do not fail if COMMAND command fails on startup #685 (Thanks to @pujian1984)
* Consider adding host/port mapper #689
* CommandHandler.write() is O(N^2) #709 (Thanks to @gszpak)
* Cluster topology lookup should not replaces self-node details with host and port from RedisURI when RedisURI is load balancer #712 (Thanks to @warrenzhu25)
* Optimize Partitions/RedisClusterNode representation #715
* Unnecessary copying of byteBuf in CommandHandler.decode() #725 (Thanks to @gszpak)
* Add unknown node as trigger for adaptive refresh #732
* Add tracing support using Brave (OpenZipkin) #782
* Improve builders and resources with Java 8 default/static interface methods #791
* ZSCAN match pattern encoding issue #792 (Thanks to @silvertype)
* Accept brave.Tracing instead of brave.Tracer #798
* Bind Master/Slave transactional commands to Master during an ongoing transaction #800
* FutureSyncInvocationHandler the statement "command.get ()" in the handlerInvocation method is unnecessary #809 (Thanks to @zhangweidavid)
* Fall back to initial seed nodes on topology refresh when dynamicRefreshSources is enabled #822 (Thanks to @stuartharper)
* Assert Java 11 build compatibility #841

Fixes
-----
* PING responses are not decoded properly if Pub/Sub connection is subscribed #579
* Lettuce doesn't fail early & cleanly with a host in protected mode #608 (Thanks to @exercitussolus)
* CommandHandler.rebuildQueue() causes long locks #615 (Thanks to @nikolayspb)
* Request queue size is not cleared on reconnect #616 (Thanks to @nikolayspb)
* BITPOS should allow to just specify start. #623 (Thanks to @christophstrobl)
* HMGET proxy not working as expected #627 (Thanks to @moores-expedia)
* Consider binary arguments using command interfaces as keys using binary codecs #628
* Command.isDone() not consistent with CompletableFuture.isDone() #629
* Race condition in RedisPublisher DEMAND.request() and DEMAND.onDataAvailable() #634 (Thanks to @mayamoon)
* RedisPublisher.request(-1) does not fail #635
* Capture subscription state before logging in RedisPublisher #636
* Provide Javadoc path for Project Reactor #641
* Debug logging of ConnectionWatchdog has wrong prefix after reconnect. #645 (Thanks to @mlex)
* Cannot close connection when refreshing topology #656 (Thanks to @dangtranhoang)
* Weights param should be ignored if it is empty #657 (Thanks to @garfeildma)
* MasterSlave getNodeSpecificViews NPE with sync API #659 (Thanks to @boughtonp)
* RandomServerHandler can respond zero bytes #660
* ConcurrentModificationException when connecting a RedisClusterClient #663 (Thanks to @blahblahasdf)
* Switch RedisSubscription.subscriber to volatile #664
* Recovered Sentinels in Master/Slave not reconnected #668
* Handling dead Sentinel slaves #669 (Thanks to @vleushin)
* Move SocketAddressResolver resolution back to calling thread #670
* Support SLAVE_PREFERRED at valueOf method #671 (Thanks to @be-hase)
* RedisCommandTimeoutException after two subsequent MULTI calls without executing the transaction #673 (Thanks to @destitutus)
* Fix ConnectionWatchDog won't reconnect problem in edge case #679 (Thanks to @kojilin)
* At least once mode keeps requeueing commands on non-recoverable errors #680 (Thanks to @mrvisser)
* Retain ssl/tls config from seed uris in Master/Slave context #684 (Thanks to @acmcelwee)
* NOAUTH after full queue and reconnect #691
* RedisURI.create("localhost") causes NPE #694
* Async connect methods report original cause #708
* Mono returned by RedisPubSubReactiveCommands#subscribe does not return result #717 (Thanks to @ywtsang)
* RuntimeExceptions thrown by implementations of RedisCodec do not fail TransactionCommands #719 (Thanks to @blahblahasdf)
* Connection Leak in Cluster Topology Refresh #721 (Thanks to @cweitend)
* Ensure Master/Slave topology refresh connections are closed #723
* RedisPubSubAdapter.message() being called with wrong channel #724 (Thanks to @adimarco)
* Batched commands may time out although data was received #729
* DefaultEndpoint future listener recycle lose command context on requeue failures #734 (Thanks to @gszpak)
* AsyncPool, AsyncConnectionPoolSupport are nowhere to be found #739 (Thanks to @fabienrenaud)
* firstResponseLatency is always negative #740 (Thanks to @nickvollmar)
* EXEC does not fail on EXECABORT #743 (Thanks to @dmandalidis)
* Warning when refreshing topology #756 (Thanks to @theliro)
* DefaultEndpoint.QUEUE_SIZE becomes out of sync, preventing command queueing #764 (Thanks to @nivekastoreth)
* DefaultEndpoint contains System.out.println(…) #765
* Do not retry completed commands through RetryListener #767
* MULTI is dispatched to slave nodes using SLAVE readFrom #779 (Thanks to @Yipei)
* Reduce service name in BraveTracing to just Redis #797
* Javadoc mentions Delay.exponential() is capped at 30 milliseconds #799 (Thanks to @phxql)
* Read From Slaves is not working #804 (Thanks to @EXPEbdodla)
* GEORADIUS WITHCOORD returns wrong coordinate on multiple results #805 (Thanks to @dittos)
* RedisClusterCommands does not extend RedisStreamCommands #821
* smembers returns elements in non-deterministic order #823 (Thanks to @alezandr)
* StackOverflowError on ScanStream.scan(…).subscribe() #824
* ZINCRBY member should be value-typed #826 (Thanks to @fuyuanpai)
* Lua script execution containing non-ascii characters fails #844 (Thanks to @wenzuowei110)
* RedisState fails to resolve CommandType for known commands #851

Other
-----
* Upgrade to HdrHistogram 2.1.10 #653
* Upgrade Redis versions on TravisCI #655
* Upgrade to Log4j 2.11.0 #749
* Extend documentation for argument objects #761
* Upgrade to JavaParser 3.6.3 #769
* Update What's new for Lettuce 5.1 in reference docs #777
* Improve Javadoc of QUIT method #781
* Upgrade to netty 4.1.29.Final #836
* Upgrade to Reactor Bismuth-SR11 #838
* Upgrade to Brave 5.2.0 #839
* Upgrade to RxJava 2.2.2 #840
* Upgrade to Commons Pool 2.6 #854
* Upgrade to Mockito 2.22 #855
* Upgrade to commons-lang3 3.8 #857
* Upgrade to Spring Framework 4.3.19 #858
* Upgrade to Reactor Californium #859
