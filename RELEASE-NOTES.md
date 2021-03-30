Lettuce 6.1.0 RELEASE NOTES
==============================

The Lettuce team is delighted to announce general availability of Lettuce 6.1.

This is a massive release thanks to all the community contributions. 
Most notable changes that ship with this release are:

* Support for Redis 6.2 commands and modifier/argument updates
* Micrometer integration
* `CommandListeners` API to intercept Redis commands
* extended Keep-Alive options
* Coroutine variant of `ScanStream`
* TCP NoDelay enabled by default
* Experimental support for `io_uring`
* Java Flight Recorder Integration for Connection and Cluster Events

Lettuce 6 supports Redis 2.6+ up to Redis 6.x. In terms of Java runtime, Lettuce requires at least Java 8 and works with Java 16.

Thanks to all contributors who made Lettuce 6.1.0 possible.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.1.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.1.0.RELEASE/api/


Micrometer Integration
----------------------

Lettuce ships an integration for Micrometer. Commands are tracked by using two Micrometer Times: `lettuce.command.firstresponse` and `lettuce.command.completion`. The following tags are attached to each timer:

* `command`: Name of the command (`GET`, `SET`, …)
* `local`: Local socket (localhost/127.0.0.1:45243 or ANY when local distinction is disabled, which is the default behavior)
* `remote`: Remote socket (localhost/127.0.0.1:6379)

To enable Micrometer, create  `MicrometerCommandLatencyRecorder` from `MeterRegistry` and register it in `ClientResources`:

```java
MeterRegistry meterRegistry = …;
MicrometerOptions options = MicrometerOptions.create();
ClientResources resources = ClientResources.builder().commandLatencyRecorder(new MicrometerCommandLatencyRecorder(meterRegistry, options)).build();

RedisClient client = RedisClient.create(resources);
```

Make sure to have Micrometer on your class path (example from a Maven `pom.xml`):

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
    <version>${micrometer.version}</version>
</dependency>
```

See also: https://github.com/lettuce-io/lettuce-core/wiki/Command-Latency-Metrics#command.latency.metrics.micrometer


CommandListeners
----------------

Command listeners allow intercepting Redis Command calls before the command is sent and upon completion (success/failure). A `CommandListener` can be registered with `RedisClient` and `RedisClusterClient`:

```java
RedisClient client = …;

client.addListener(new CommandListener() {
    @Override
    public void commandStarted(CommandStartedEvent event) {
        Map<String, Object> context = event.getContext();
        context.put(…);
    }

    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
        Map<String, Object> context = event.getContext();
    }

    @Override
    public void commandFailed(CommandFailedEvent event) {
        CommandListener.super.commandFailed(event);
    }
});


StatefulRedisConnection<String, String> connection = client.connect();
```
Command events allow attaching contextual details that can be accessed upon completion.

Experimental support for io_uring
---------------------------------

We've adopted Netty's experimental io_uring support for Linux-based systems to allow participating in improved I/O performance.

io_uring will be enabled automatically if you're running on Linux and you have the dependency on your classpath:

```xml
<dependency>
    <groupId>io.netty.incubator</groupId>
    <artifactId>netty-incubator-transport-native-io_uring</artifactId>
    <version>${netty.transport-native-io_uring.version}</version>
    <classifier>linux-x86_64</classifier>
</dependency>
```

When having both native transports enabled (io_uring and epoll), then io_uring has precedence over epoll.
We'd love to hear from you how io_uring works out for you.

Java Flight Recorder Integration
--------------------------------

Lettuce emits Connection and Cluster events as Java Flight Recorder events. `EventBus` emits all events to `EventRecorder` and the actual event bus.

`EventRecorder` verifies whether your runtime provides the required JFR classes (available as of JDK 8 update 262 or later) and if so, then it creates Flight Recorder variants of the event and commits these to JFR.

The following events are supported out of the box:

**Redis Connection Events**

* Connection Attempt
* Connect, Disconnect, Connection Activated, Connection Deactivated
* Reconnect Attempt and Reconnect Failed

**Redis Cluster Events**

* Topology Refresh initiated
* Topology Changed
* ASK and MOVED redirects

**Redis Master/Replica Events**

* Sentinel Topology Refresh initiated
* Master/Replica Topology Changed

Events come with a rich set of event attributes such as channelId, epId (endpoint Id), Redis URI and many more.

You can record data by starting your application with:

```shell
java -XX:StartFlightRecording:filename=recording.jfr,duration=10s …
```

Hostname support for Redis Sentinel
-----------------------------------

Since Redis 6.2, Redis Sentinel and Redis itself can use hostnames to announce its replication partner. 
Lettuce accepts this information as `SocketAddress` and passes on the address to Netty which then can perform the hostname resolution.

Please pay special attention when using `ReadFrom.subnet(…)` as `ReadFrom` operates on the raw addressing information reported by Redis without applying further hostname resolution.


Option to configure SSL/TLS verification level
----------------------------------------------

`RedisURI.setVerifyPeer(…)` now accepts `SslVerifyMode` which can be one of `NONE` (corresponds with `setVerifyPeer(false)`), `CA` and `FULL` (corresponds with `setVerifyPeer(true)`).

Peer verification settings update on `SSLParameters` for `CA` and `FULL` verification mode and `NONE` overwrites `trustManager(…)` during SSL bootstrapping.
                    

Commands
------------
* Add support for `SET … GET` option #1442
* Add support for `(B)LMOVE` source destination LEFT|RIGHT LEFT|RIGHT command #1448
* Add support for `ZMSCORE key member [member ...]` command #1449
* Add support for `ZINTER`/`ZUNION` commands #1450
* Add support for `ZADD` `GT`/`LT` options #1451
* Add support for `SMISMEMBER key member [member ...]` command #1452
* Support `NOMKSTREAM` option in `XADD` command #1502
* Support option `CREATECONSUMER` in `XGROUP` command #1505 (Thanks to @dengliming)
* Add support for `ZRANGESTORE` command #1506
* Add support for `ZDIFF` and `ZDIFFSTORE` commands #1507
* Add support for `COPY` command #1508
* Add support for local addr in `CLIENT KILL` #1536
* Add support for IDLE option in XPENDING #1537
* Support for ACL commands #1538 (Thanks to @GraemeMitchell84)
* Add support for `LPOP` and `RPOP` with `COUNT` #1545
* Missing support for `TYPE` parameter of SCAN command #1559 (Thanks to @mvmn)
* Add support for `GEOSEARCH` and `GEOSEARCHSTORE` #1561
* Add support for `ReadFrom.subnet` #1569 (Thanks to @yueki1993)
* Add support for MINID trimming strategy and the LIMIT argument to XADD and XTRIM #1582
* Add exclusive range query to `XPENDING` #1585
* Add support for GEOADD [CH] [NX|XX] options #1584
* Add support for HRANDFIELD and ZRANDMEMBER commands #1605
* Add support for GETEX, GETDEL commands #1606
* Add support for `PXAT`/`EXAT` arguments to `SET` command #1607
* Add FlushMode to FLUSHALL and FLUSHDB, and to SCRIPT FLUSH #1608
* Add support for MIGRATE AUTH2 option #1633
* Add support for RESTORE ABSTTL #1634
* Add support for XAUTOCLAIM #1574
* Add support for CLIENT KILL USER #1672 (Thanks to @dengliming)
* Add support IDLETIME and FREQ args to RESTORE command #1683 (Thanks to @dengliming)
* Add support for OBJECT FREQ #1684 (Thanks to @dengliming)


Enhancements
------------
* Add Micrometer integration #795
* Add support for `CommandListeners` #1382 (Thanks to @sokomishalov)
* Add support for Java Flight Recorder #1430
* Provide a Coroutine variant of ScanStream/ScanIterator #1435
* Introduce extended Keep-Alive options #1437
* Add anyReplica setting for `ReadFrom` #1444 (Thanks to @omer-cilingir)
* Option to configure SSL/TLS verification level #1460 (Thanks to @Lucas3oo)
* Use Netty's asynchronous DNS resolver #1498 (Thanks to @yueki1993)
* Extend `CommandDetailParser` to include ACL details #1503
* Consider reinstating master-replica wording #1518 (Thanks to @perlun)
* Add support for io_uring #1522
* Provide `VoidOutput` for Fire&Forget command usage #1529 (Thanks to @jaredpetersen)
* Improve unsupported error logging for `CommandOutput` #1532
* Allow providing custom ClusterTopologyRefresh implementation. #1598 (Thanks to @alessandrosimi-sa)
* Introduce `LettuceStrings.isEmpty(String)` overload with optimized isEmpty checking #1609
* Add hostname support to Sentinel #1635
* Introduce GeoValue value type #1643
* Add overload to ScanArgs to accept a binary `match` #1675

Fixes
-----
* Fix `EXEC` without `MULTI` when using coroutines over async #1441 (Thanks to @sokomishalov)
* Lettuce with Tracing enabled fails to connect to a Redis Sentinel #1470 (Thanks to @jsonwan)
* Lettuce doesn't handle deleted stream items (`NullPointerException`) #1474 (Thanks to @chemist777)
* LettuceStrings does not handle `-nan` which is returned by `FT.INFO` in redisearch #1482 (Thanks to @krm1312)
* Improperly decoding command responses #1512 (Thanks to @johnny-costanzo)
* Fix timeout parameter for nanoseconds in `RedisURI` #1528 (Thanks to @izeye)
* Fix build break when missing netty-dns-resolver #1546 (Thanks to @yueki1993)
* Sentinel lookup connection leaks if no master address reported #1558 (Thanks to @wwwjinlong)
* Lettuce 6.0.1 fails with GraalVM 20.3 #1562 (Thanks to @atrianac)
* Reactive stream spec violation when using command timeout #1576 (Thanks to @martin-tarjanyi)
* Fix copyright replace bug for Kotlin api generator #1588 (Thanks to @dengliming)
* Fix NullPointerException in BoundedAsyncPool.createIdle() when at capacity #1611
* XREADGROUP skips messages if body is nil #1622 (Thanks to @asalabaev)
* TransactionCommand applied incorrectly #1625 (Thanks to @checky)
* ScoredValueUnitTests test fail #1647 (Thanks to @dengliming)
* Create traced endpoint when ready #1653 (Thanks to @anuraaga)

Other
-----
* Fix integration test password #1445
* Un-Deprecate `io.lettuce.core.LettuceFutures` #1453 (Thanks to @andrewsensus)
* Switch to `Flux/Mono.expand(…)` for `ScanStream` #1458
* Enable TCP NoDelay by default #1462
* Update contrib guide #1472
* Adapt tests to changed Redis response #1473
* Upgrade dependencies #1476
* Remove JUnit 4 dependency management #1477
* Replace `ClusterRule` #1478
* Remove Redis Command retrieval for Redis Cluster Connections #1481
* Implement `set(double)` in `NestedMultiOutput` #1486 (Thanks to @jruaux)
* Start `HashWheelTimer` in `ClientResources` to avoid blocking calls in EventLoop #1489
* `DefaultClientResources`: fix typos #1497 (Thanks to @perlun)
* API generator problems #1499 (Thanks to @sokomishalov)
* Reduce build matrix to Java 8, 11, 15, and EA #1519
* Upgrade to Netty 4.1.54.Final #1541
* netty 4.1.56 #1556 (Thanks to @sullis)
* Move Mailing list forum to GitHub discussions #1557
* Let coroutines `dispatch`-method be flowable #1567 (Thanks to @sokomishalov)
* Update copyright years to 2021 #1573
* Upgrade to netty 4.1.58.Final #1610
* Ping command is not supported in Pub/Sub connection #1601 (Thanks to @TsybulskyStepan)
* Fix formatting indent in kotlin files #1603 (Thanks to @sokomishalov)
* Upgrade kotlin to 1.4.30, kotlinx-coroutines to 1.4.2 #1620 (Thanks to @sokomishalov)
* Typo in string reactive commands #1632 (Thanks to @paualarco)
* Let nullable ScoredValue factory methods return Value<V> instead of ScoredValue<V> #1644
* Cleanup MULTI state if MULTI command fails #1650
* Upgrade to Kotlin 1.4.31 #1657
* Use kotlin and kotlinx-coroutines bills of materials #1660 (Thanks to @sokomishalov)
* Fix travis links due to its domain name migration #1661 (Thanks to @sokomishalov)
* Rename zrandmemberWithscores to zrandmemberWithScores #1663 (Thanks to @dengliming)
* Change KeepAlive Default Interval to 75 #1671 (Thanks to @yangbodong22011)
* Fix comment in FlushMode #1677 (Thanks to @dengliming)
* Fix CI/Travis build #1678
* Ensure Kotlin 1.3 compatibility #1694
* Upgrade to Kotlin 1.4.32 #1697
* Upgrade to Micrometer 1.6.5 #1698
