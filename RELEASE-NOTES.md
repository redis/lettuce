Lettuce 5.1.0 M1 RELEASE NOTES
===========================

This is the first preview release of Lettuce 5.1 shipping with improvements and 
bugfixes.
 
Most notable changes are:

* API Preview for Redis Streams
* Asynchronous pool implementation
* `SCAN` streams, and initial 
* Global command timeouts (disabled by default)
* Asynchronous connect methods for Master/Slave and Sentinel

Find the full change log at the end of this document that lists all 93 tickets.

Thanks to all contributors who made Lettuce 5.1.0.M1 possible.
Lettuce 5.1.0.M1 requires Java 8, Java 9, or Java 10.

Reference documentation: https://lettuce.io/core/5.1.0.M1/reference/.
JavaDoc documentation: https://lettuce.io/core/5.1.0.M1/api/.

New Exceptions for Redis Responses
----------------------------------

This release introduces new Exception types for the following Redis responses:

* `LOADING`: `RedisLoadingException`
* `NOSCRIPT`: `RedisNoScriptException`
* `BUSY`: `RedisBusyException`

All exception types derive from `RedisCommandExecutionException` and do not 
require changes in application code.


Redis Streams (Preview)
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

Please note that the Redis Stream implementation is not final yet and the API is subject to change if
the Redis API changes.

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


Thanks to all contributors that made Lettuce 5.1.0.M1 possible.

Lettuce 5.1.0.M1 requires Java 8 and cannot be used with Java 6 or 7.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/

Commands
--------
* Add AUTH option to MIGRATE command #733
* Add MASTER type to KillArgs #760

Enhancements
------------
* Support for command timeouts (async, reactive) #435
* Use object pooling for collections inside a single method and Command/CommandArgs with a small scope #459
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
* CommandHandler.write() is O(N^2) #709 (Thanks to @gszpak)
* Cluster topology lookup should not replaces self-node details with host and port from RedisURI when RedisURI is load balancer #712 (Thanks to @warrenzhu25)
* Optimize Partitions/RedisClusterNode representation #715
* Unnecessary copying of byteBuf in CommandHandler.decode() #725 (Thanks to @gszpak)
* Add unknown node as trigger for adaptive refresh #732

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

Other
-----
* Upgrade to netty 4.0.53.Final/4.1.17.Final #646
* Upgrade to Reactor Bismuth SR4 #647
* Upgrade to Spring Framework 4.3.12 #648
* Upgrade to Commons Pool 2.4.3 #650
* Upgrade to RxJava 1.3.3 #651
* Upgrade to RxJava2 2.1.6 #652
* Upgrade to HdrHistogram 2.1.10 #653
* Upgrade Redis versions on TravisCI #655
* Upgrade to Reactor Bismuth SR5 #698
* Upgrade to netty 4.1.21.Final #699
* Upgrade to RxJava 1.3.6 #700
* Upgrade to RxJava 2.1.9 #701
* Upgrade to Reactor Bismuth SR6 #704
* Upgrade to Netty 4.1.22 #744
* Upgrade to RxJava 1.3.7 #745
* Upgrade to Spring Framework 4.3.14 #746
* Upgrade to Mockito 2.17 #747
* Upgrade to AssertJ 3.9.1 #748
* Upgrade to Log4j 2.11.0 #749
* Upgrade to commons-lang3 3.7 #750
* Upgrade to netty 4.1.23.Final #755
* Upgrade to Reactor Bismuth SR8 #758
* Upgrade to RxJava 1.3.8 #759
* Extend documentation for argument objects #761
* Upgrade to JavaParser 3.6.3 #769
* Upgrade to netty 4.1.24.Final #770
* Upgrade to RxJava 2.1.13 #771

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
