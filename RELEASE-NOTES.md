lettuce 3.5.0 RELEASE NOTES
===========================

lettuce 3.5.0 is here. This release follows the same renumbering like 4.2.0.
 
This release comes with SSL support for Redis Cluster, support for new commands
and a series of bug fixes.

**Important note for users of connection-pooling and latency tracking**

Dependencies were streamlined with this release. Apache's `commons-pool2` and
`latencyutils` are now _optional_ dependencies. If you use connection-pooling or
latency tracking, please include these dependencies explicitly otherwise these
features will be disabled.

lettuce 3.5.0 is fully binary compatible with the last release and can be used
as a drop-in replacement for 3.4.x.  This update is strongly recommended for
lettuce 3.x users as it fixes some critical connection synchronization bugs.

Thanks to all contributors that made lettuce 3.5.0 possible.

lettuce 3.5.0 requires Java 6. Please note that some commands are
only available in the Redis unstable branch (such as variadic LPUSHX/RPUSHX).


Redis Cluster and SSL
---------------------
Redis introduces an option to announce a specific IP address/port using
`cluster-announce-ip` and `cluster-announce-port`. This is useful for
Docker and NAT'ed setups. Furthermore, you can "hide" your Redis Cluster nodes
behind any other proxy like `stunnel`. A Redis Cluster node will announce the
specified port/IP which can map to `stunnel`, and you get an SSL-protected
Redis Cluster. Please note that `cluster-announce-ip` is not part of Redis 3.2
but will be released in future versions.

Redis Cluster SSL works pretty much the same as Redis Standalone with SSL. You
can configure SSL and other SSL/TLS options using `RedisURI`.

```java
RedisURI redisURI = RedisURI.Builder.redis(host(), 7443)
                                .withSsl(true)
                                .withVerifyPeer(false)
                                .build();

RedisClusterClient redisClusterClient = RedisClusterClient.create(redisURI);
RedisConnection<String, String> connection = redisClusterClient.connect();
```

You should disable the `verifyPeer` option if the SSL endpoints cannot provide a
valid certificate. When creating a `RedisClusterClient` using
`RedisClusterClientFactoryBean` the `verifyPeer` option is disabled by default.

Lettuce was successfully tested with Azure Redis with SSL and authentication.

Read more: https://github.com/mp911de/lettuce/wiki/Redis-Cluster-%284.0%29


Redis Modules
-------------

Redis module support is a very young feature. lettuce provides a custom command
API to dispatch own commands. 

lettuce provides with 3.4.0 `dispatch(…)` methods on the synchronous and asynchronous 
API type to provide a simple interface for command invocation.

```java
RedisCodec<String, String> codec = new Utf8StringCodec();

String response = redis.dispatch(CommandType.SET,
        new StatusOutput<>(codec),
        new CommandArgs<>(codec)
                .addKey(key)
                .addValue(value));
```

Calls to `dispatch(…)` on the synchronous API are blocking calls, calls on the
asynchronous API return a `RedisFuture<T>`.

Using `dispatch(…)` allows to invoke arbitrary commands and works together
within transactions and Redis Cluster. Exposing this API also allows choosing a
different `RedisCodec` for particular operations.

Read more: https://github.com/mp911de/lettuce/wiki/Custom-commands%2C-outputs-and-command-mechanics


If you need any support, meet lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues


Commands
--------
* Add support for CLUSTER BUMPEPOCH command #179
* Support extended MIGRATE syntax #197
* Add support for GEORADIUS STORE and STOREDIST options #199
* Add support for BITFIELD command #206
* Add support for SPOP key count #235
* Add support for GEOHASH command #239
* Add simple interface for custom command invocation for sync/async/reactive APIs #245
* Add support for variadic LPUSHX and RPUSHX #267
* Add support for TOUCH command #270

Enhancements
------------
* Add log statement for resolved address #218 (Thanks to @mzapletal)
* Expose DNS Resolver #236
* Make latencyutils and commons-pool2 dependencies optional #237
* Support Redis Cluster with SSL #268

Fixes
-----
* Lettuce fails sometimes at shutdown: DefaultClientResources.shutdown #194
* Extensive calls to PooledClusterConnectionProvider.closeStaleConnections #195
* Shared resources are closed altough still users are present #196
* Lettuce does not repackage new dependencies #198 (Thanks to @CodingFabian)
* Fix comparison for computationThreadPoolSize #205 (Thanks to @danhyun)
* Gracefully shutdown DefaultCommandLatencyCollector.PAUSE_DETECTOR #223 (Thanks to @sf-git and @johnou)
* RedisClient.connect(RedisURI) fails for unix socket based URIs #229 (Thanks to @nivekastoreth)
* HdrHistogram and LatencyUtils are not included in binary distribution #231
* Cache update in Partitions is not thread-safe #234
* GEORADIUSBYMEMBER, GEORADIUS and GEOPOS run into NPE when using Redis Transactions #241
* LettuceFutures.awaitAll throws RedisCommandInterruptedException when awaiting failed commands #242
* Perform multiple connection attempts when connecting a Redis Cluster #244
* Fix command sequence on connection activation #253 (Thanks to @long-xuan-nguyen)
* Cluster topology refresh: Failed connections are not closed bug #255
* Connection lifecycle state DISCONNECTED is considered a connected sate #257
* Writing commands while a disconnect is in progress leads to a race-condition #258
* Canceled commands lead to connection desynchronization #262 (Thanks to @long-xuan-nguyen)
* Fix JavaDoc for blocking list commands #272
* Unschedule topology refresh on cluster client shutdown #276
* Record command upon write #277
* Disable ConnectionWatchdog when closing a disconnected connection #278
* Perform selective relocation of org.* packages #280
* Guard key parameters against null values #287
* Guard command encoding against null #291 (Thanks to @christophstrobl)

Other
------
* Switch remaining tests to AssertJ #13
* Switch travis-ci to container build #203
* Code cleanups #215
* Upgrade to stunnel 5.33 #290
* Upgrade netty to 4.0.37.Final #294
* Upgrade rxjava to 1.1.6 #295


lettuce requires a minimum of Java 8 to build and Java 6 run. It is tested
continuously against the unstable branch.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
                or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki
