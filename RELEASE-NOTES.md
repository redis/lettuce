lettuce 5.0.0 Beta 1 RELEASE NOTES
==================================

This is a major release coming with several breaking changes and new interesting
features. It's similar to the simultaneously released version 4.3.0 but 
comes with major changes.

Lettuce 5.0 removes deprecated interfaces RedisConnection and RedisAsyncConnection
and their segregated interfaces in favor of StatefulRedisConnection and RedisCommands et al.

This release removes dependencies to Guava. Lettuce requires only Netty and Project Reactor
which brings us to the next change. The reactive API is based on Reactive Streams by using 
Project Reactor types `Mono` and `Flux` instead of RxJava 1 and `Observable`.
Existing users can convert `Mono` and `Flux` by using `rxjava-reactive-streams`.

Finally, this release introduces the dynamic Redis Commands API. This programming
model enables you to declare command methods and invoke commands to your needs and
support Redis Modules without waiting for lettuce to support new commands.

This beta/candidate release is to give you a chance to try out new features, 
get feedback and improve for the final release.

You will find the full change log at the end of this document.

Thanks to all contributors that made lettuce 5.0.0 possible.

lettuce 5.0.0 requires Java 8 and cannot be used with Java 6 or 7.


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


Migration to Reactive Streams (Project Reactor)
-----------------------------------------------

Lettuce 4.0 introduced a reactive API based on RxJava 1 and `Observable`.
This was the beginning of reactive Redis support. Lettuce used `Observable`
all over the place as other reactive types like `Single` and `Completable` were
still beta or in development.

Since that time, a lot changed in the reactive space. RxJava 2 is still in the
works towards a final release supporting Reactive Streams, while other composition
libraries are already available and polish on sharp edges. This means, 
it was just a matter of time that lettuce adopts Reactive Streams.

This also means, no `null` values and usage of dedicated value types to express
value multiplicity (`0|1` and `0|1|N`) on the API.

With lettuce 5.0, the reactive API uses Project Reactor, `Mono` and `Flux`.

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

See also: https://github.com/mp911de/lettuce/wiki/Reactive-API-%285.0%29


Value, KeyValue, and other value types
--------------------------------------

This release enhances existing value types and introduces new types
to reduce `null` usage and facilitate functional programming. 

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


Dynamic Redis Commands API
--------------------------

The Redis Command Interface abstraction provides a dynamic way for typesafe Redis 
command invocation. It allows you to declare an interface with command methods to 
significantly reduce boilerplate code required to invoke a Redis command.

Redis is a data store supporting over 190 documented commands and over 450 command permutations. 
Command growth and keeping track with upcoming modules are challenging for client 
developers and Redis user as there is no full command coverage for each module in a single Redis client.

Invoking a custom command with lettuce several lines of code to define command structures
pass in arguments and specify the return type.

```java
RedisCodec<String, String> codec = new StringCodec();
RedisCommands<String, String> commands = ...

String response = redis.dispatch(CommandType.SET, new StatusOutput<>(codec),
                new CommandArgs<>(codec)
                       .addKey(key)
                       .addValue(value));
```

The central interface in lettuce Command Interface abstraction is `Commands`. 
This interface acts primarily as a marker interface to help you to discover 
interfaces that extend this one. You can declare your own command interfaces 
and argument sequences where the command name is derived from the method name or 
provided with `@Command`. Introduction of new commands does not require you 
to wait for a new lettuce release but they can invoke commands through own declaration. 
That interface could be also supporting different key and value types, depending on the use-case.

Commands are executed applying synchronization, asynchronous and in a reactive fashion, 
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

You get a whole lot new possibilities with Redis Command Interfaces. This release
provides initial support. Future versions are likely to support RxJava 1/2 reactive
types so RxJava 1 users have a migration path that allows using native types without
further conversion.

See also: https://github.com/mp911de/lettuce/wiki/Redis-Command-Interfaces


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
These methods are removed with lettuce 5.0.

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
* Remove deprecated interfaces and methods #156
* Remove Google Guava usage #217
* Add SocketOptions to ClientOptions #269
* Add support for OpenSSL as SSL provider #249
* Replacement support for connection pooling #264
* Add ToByteBufEncoder and improved StringCodec #275
* Allow configuration of a trust store password #292
* Replace Guava Cache by ConcurrentHashMap #300
* Eager initialization of API wrappers in stateful connections #302 (Thanks to @jongyeol)
* Change MethodTranslator's loadfactor to 1.0 for sync APIs performance #305 (Thanks to @jongyeol)
* Reattempt initially failed Sentinel connections in Master/Slave API #306
* Decouple CommandHandler #317
* Use TransactionResult for transaction results #320
* Replace synchronized setters with volatile fields #326 (Thanks to @guperrot)
* Add workaround for IPv6 parsing #332
* Provide multi-key-routing for exists and unlink commands using Redis Cluster #334
* Migrate lettuce reactive API to Reactive Streams #349
* Provide Value types #350
* Add ConnectionWatchdog as last handler #335
* Provide Timer as part of ClientResources #354 (Thanks to @plokhotnyuk)
* Add support for topology consensus #355
* Use Limit in SortArgs #364
* Add jitter backoff strategies for spreading reconnection timing #365 (Thanks to @jongyeol)
* Add EVAL and EVALSHA to ReadOnlyCommands #366 (Thanks to @amilnarski)
* Add support for ZADD INCR with ZAddArgs #367 (Thanks to @christophstrobl)
* Accept double in ZStoreArgs.weights #368 (Thanks to @christophstrobl)
* Consider role changes as trigger for update using MasterSlave connections #370
* Provide a dynamic Redis Command API #371
* Support lettuce parameter types #381
* Expose Value.map methods #386 

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
* Allow empty values in BITFIELD using the reactive API #378
* Support integer width multiplied offsets in BITFIELD #379 (Thanks to @christophstrobl)
* Propagate array sizes in MultiOutput #380 (Thanks to @christophstrobl)

Other
------
* Improve test synchronization #216
* Upgrade to stunnel 5.33 #290
* Upgrade logging to log4j2 for tests #316
* Upgrade to AssertJ 3.5.2 #352
* Add test to verify behavior of GEODIST if a geoset is unknown #362
* Update license/author headers #387
* Upgrade to netty 4.0.42.Final/4.1.6.Final 390
* Upgrade to Project Reactor 3.0.3 #395


lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki
