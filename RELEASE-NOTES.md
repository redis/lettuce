lettuce 4.3.0 RELEASE NOTES
===========================

This release fixes several issues, provides a streamlined Sorted Sets API
and introduces new features for network partition recovery, zero-downtime 
Redis Cluster reconfiguration and improved resource handling. 
Lettuce 4.3 comes with a new `StringCodec` that uses optimized 
String encoding and decoding for UTF-8 and ASCII charsets.
Encoding and decoding is up to 5x faster now.

You will find the full change log at the end of this document.

lettuce 4.3.0 is not binary compatible with the last release as 
it changed the signature of `srandmember` to return `List` instead of `Set`.

Thanks to all contributors that made lettuce 4.3.0 possible.

lettuce 4.3.0 requires Java 8 and cannot be used with Java 6 or 7.

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
https://github.com/mp911de/lettuce/wiki/Configuring-Client-resources


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

**Since 4.3**

```java
commands.zcount(key, Range.create(1.0, 3.0));

commands.zrangebyscore(key, Range.unbounded());

commands.zrangebyscoreWithScores(key, Range.from(Boundary.including(1.0), Boundary.excluding(4.0));

commands.zrangebyscoreWithScores(key, Range.unbounded(), Limit.create(2, 2));
```


Connection pooling deprecation
------------------------------

It took quite a while but 4.3 finally deprecates Lettuce's existing connection pooling
support. That are in particular `RedisClient.pool(…)` and `RedisClient.asyncPool(…)`. 
These methods will be removed with lettuce 5.0.

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
cluster nodes. This helps lettuce to stick with the cluster that consists of the
most nodes.
 
See also: https://github.com/mp911de/lettuce/issues/355


If you need any support, meet lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues


Commands
--------
* Add support for TOUCH command #270
* Add support for variadic LPUSHX and RPUSHX #267
* Provide a more conceise API for Sorted Set query operations using Range/Limit #363
* Add support for ZREVRANGEBYLEX command #369 (Thanks to @christophstrobl)
* Add support for SWAPDB #375

Enhancements
------------
* Add SocketOptions to ClientOptions #269
* Add support for OpenSSL as SSL provider #249
* Replacement support for connection pooling #264
* Add ToByteBufEncoder and improved StringCodec #275
* Allow configuration of a trust store password #292
* Replace Guava Cache by ConcurrentHashMap #300
* Eager initialization of API wrappers in stateful connections #302 (Thanks to @jongyeol)
* Change MethodTranslator's loadfactor to 1.0 for sync APIs performance #305 (Thanks to @jongyeol)
* Reattempt initially failed Sentinel connections in Master/Slave API #306
* Replace synchronized setters with volatile fields #326 (Thanks to @guperrot)
* Add workaround for IPv6 parsing #332
* Provide multi-key-routing for exists and unlink commands using Redis Cluster #334
* Add ConnectionWatchdog as last handler #335
* Provide Timer as part of ClientResources #354 (Thanks to @plokhotnyuk)
* Add support for topology consensus #355
* Use Limit in SortArgs #364
* Add jitter backoff strategies for spreading reconnection timing #365 (Thanks to @jongyeol)
* Add EVAL and EVALSHA to ReadOnlyCommands #366 (Thanks to @amilnarski)
* Add support for ZADD INCR with ZAddArgs #367 (Thanks to @christophstrobl)
* Accept double in ZStoreArgs.weights #368 (Thanks to @christophstrobl)
* Consider role changes as trigger for update using MasterSlave connections #370 

Fixes
-----
* Fix JavaDoc for blocking list commands #272
* Guard key parameters against null values #287
* CommandArgs.ExperimentalByteArrayCodec fails to encode bulk string #288
* Guard value parameters against null values #291 (Thanks to @christophstrobl)
* Allow MasterSlave connection using Sentinel if some Sentinels are not available #304 (Thanks to @RahulBabbar)
* Allow coordinated cross-slot execution using Iterable #303 (Thanks to @agodet)
* Use at least 3 Threads when configuring default thread count #309
* Replace own partition host and port only if the reported connection point is empty #312
* Lettuce RedisClusterClient calls AUTH twice #313
* CommandHandler notifications called out of order #315
* Disable SYNC command #319
* Record sent-time on command queueing #314 (Thanks to @HaloFour)
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
* Support integer width multiplied offsets in BITFIELD #379 (Thanks to @christophstrobl)
* Propagate array sizes in MultiOutput #380 (Thanks to @christophstrobl)

Other
------
* Improve test synchronization #216
* Upgrade to stunnel 5.33 #290
* Upgrade rxjava to 1.1.6 #295
* Upgrade logging to log4j2 for tests #316
* Cleanup connections during test #318
* Upgrade netty to 4.0.40.Final and 4.1.4.Final #336
* Upgrade rxjava to 1.1.9 #337
* Upgrade to Guava 19 #351
* Upgrade to AssertJ 3.5.2 #352
* Upgrade netty to 4.0.41.Final and 4.1.5.Final #353
* Add test to verify behavior of GEODIST if a geoset is unknown #362
* Update license/author headers #387
* Upgrade to netty 4.0.42.Final/4.1.6.Final 390
* Upgrade to rxjava 1.2.1 391


lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki