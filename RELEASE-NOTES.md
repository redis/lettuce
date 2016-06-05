lettuce 4.2.0 RELEASE NOTES
===========================

That's the zero behind 4.2? Well, that's to not break OSGi support.  Now let's
talk about the more interesting things. Lettuce 4.2.0 is a major release and
completes development of several notable enhancements.

This release comes with SSL support, Publish/Subscribe and adaptive topology
refreshing for Redis Cluster. It features a major refactoring of the command
handling and several improvements for Cloud-based Redis services and
improvements to the Master/Slave API.

The usage of Guava was reduced for the most parts. Only the `LoadingCache`,
`InetAddresses` and `HostAndPort` components are still in use.  A future lettuce
5.0 version will eliminate the use of Guava completely.

**Important note for users of connection-pooling and latency tracking**

Dependencies were streamlined with this release. Apache's `commons-pool2` and
`latencyutils` are now _optional_ dependencies. If you use connection-pooling or
latency tracking, please include these dependencies explicitly otherwise these
features will be disabled.

Lettuce 4.2.0 was verified with several Cloud-based Redis services. It works
with different AWS ElastiCache usage patterns and is known to work with the
Azure Redis Cluster service supporting SSL and authentication (see below).

lettuce 4.2.0 is fully binary compatible with the last release and can be used
as a drop-in replacement for 4.1.x.  This update is strongly recommended for
lettuce 4.x users as it fixes some critical connection synchronization bugs.

Thanks to all contributors that made lettuce 4.2.0 possible.

lettuce 4.2.0 requires Java 8 and cannot be used with Java 6 or 7.


Redis Cluster Publish/Subscribe
-------------------------------
Redis Cluster
provides Publish/Subscribe features to broadcast messages across the cluster.
Using the standalone client allows using Publish/Subscribe with Redis Cluster
but comes with the limitation of high-availability/failover.

If a node goes down, the connection is lost until the node is available again.
lettuce addresses this issue with Redis Cluster Publish/Subscribe and provides a
failover mechanism.

Publish/Subscribe messages and subscriptions are operated on the default cluster
connection. The default connection is established with the node with the least
client connections to achieve a homogeneous connection distribution. It also
uses the cluster topology to failover if the currently connected node is down.

Publishing a message using the regular cluster connection is still possible
(since 4.0). The regular cluster connection calculates a slot-hash from the
channel (which is the key in this case). Publishing always connects to the
master node which is responsible for the slot although `PUBLISH` is not affected
by the keyspace/slot-hash rule.

Read more: https://github.com/mp911de/lettuce/wiki/Pub-Sub-%284.0%29


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
StatefulRedisClusterConnection<String, String> connection = redisClusterClient.connect();
```

You should disable the `verifyPeer` option if the SSL endpoints cannot provide a
valid certificate. When creating a `RedisClusterClient` using
`RedisClusterClientFactoryBean` the `verifyPeer` option is disabled by default.

Lettuce was successfully tested with Azure Redis with SSL and authentication.

Read more: https://github.com/mp911de/lettuce/wiki/Redis-Cluster-%284.0%29


Redis Cluster Topology Discovery and Refreshing
----------------------------------------------
The lettuce Redis Cluster Client
allows regular topology updates. lettuce 4.2.0 improves the existing topology
updates with adaptive refreshing and dynamic/static topology discovery.

Adaptive refresh initiates topology view updates based on events happened during
Redis Cluster operations. Adaptive triggers lead to an immediate topology
refresh. Adaptive updates are rate-limited using a timeout since events can
happen on a large scale. Adaptive refresh triggers are disabled by default and
can be enabled selectively:

* `MOVED_REDIRECT`
* `ASK_REDIRECT`
* `PERSISTENT_RECONNECTS`

Dynamic/static topology discovery sources are the second change to topology
refresh. lettuce uses by default dynamic discovery. Dynamic discovery retrieves
the initial topology from the seed nodes and determines additional nodes to
request their topology view. That is to reduce split-brain views by choosing the
view which is shared by the majority of cluster nodes.

Dynamic topology discovery also provides latency data and client count for each
node in the cluster. These details are useful for calculating the nearest node
or the least used node.

Dynamic topology discovery can get expensive when running large Redis Clusters
as all nodes from the topology are queried for their view. Static topology
refresh sources limit the nodes to the initial seed node set. Limiting nodes is
friendly to large clusters but it will provide latency and client count only for
the seed nodes.

Read more: https://github.com/mp911de/lettuce/wiki/Client-options#adaptive-cluster-topology-refresh


Redis Modules
-------------

Redis module support is a very young feature. lettuce provides a custom command
API to dispatch own commands. `StatefulConnection` already allows sending of
commands but requires wrapping of commands into the appropriate synchronization
wrapper (Future, Reactive, Fire+Forget).

lettuce provides with 4.2.0 `dispatch(…)` methods on each API type to provide a
simpler interface.

```java
RedisCodec<String, String> codec = new Utf8StringCodec();

String response = redis.dispatch(CommandType.SET,
        new StatusOutput<>(codec),
        new CommandArgs<>(codec)
                .addKey(key)
                .addValue(value));
```

Calls to `dispatch(…)` on the synchronous API are blocking calls, calls on the
asynchronous API return a `RedisFuture<T>` and calls on the Reactive API return
an `Observable<T>` which flat-maps collection responses.

Using `dispatch(…)` allows to invoke arbitrary commands and works together
within transactions and Redis Cluster. Exposing this API also allows choosing a
different `RedisCodec` for particular operations.

Read more: https://github.com/mp911de/lettuce/wiki/Custom-commands%2C-outputs-and-command-mechanics


CommandHandler refactoring
--------------------------
Command sending, buffering, encoding and receiving was refactored on a large scale.
Command encoding is performed in a separate handler and outside of `CommandHandler`.
It does not longer allocate an additional buffer to encode its arguments, but
arguments are directly written to the command buffer that is used to encode
single command/batch of commands. Fewer memory allocations help improving
performance and do not duplicate data.

Synchronization and locking were reworked as well. The critical path used for
writing commands is no longer locked exclusively but uses a shared locking with
almost lock-free synchronization.


Improvements to Master/Slave connections
----------------------------------------
lettuce introduced with 4.1 a Master/Slave API which is now more dynamic. It's
no longer required to connect to a Master node when using Master/Slave without
Sentinel as the Master/Slave API will discover the master by itself. Providing
one seed node enables dynamic lookup. The API is internally prepared for dynamic
updates which are used with Redis Sentinel.

A Sentinel-managed Master/Slave setup discovers configuration changes based on
Sentinel events and updates its topology accordingly.

Another change is the broader support of AWS ElastiCache Master/Slave setups.
AWS ElastiCache allows various patterns for usage. One of them is the automatic
failover. AWS ElastiCache exposes a connection point hostname and updates the
DNS record to point to the current master node. Since the JVM has a built-in
cache it's not trivial to adjust the DNS lookup and caching to the special needs
which are valid only for AWS ElastiCache connections. lettuce exposes a DNS
Lookup API that defaults to the JVM lookup. Lettuce ships also with
`DirContextDnsResolver` that allows own DNS lookups using either the
system-configured DNS or external DNS servers. This implementation comes without
caching and is suitable for AWS ElastiCache.

Another pattern is using AWS ElastiCache slaves. Before 4.2.0, a static setup
was required. Clients had to point to the appropriate node. The Master/Slave API
allows specifying a set of nodes which form a static Master/Slave setup. Lettuce
discovers the roles from the provided nodes and routes read/write commands
according to `ReadFrom` settings.

Read more: https://github.com/mp911de/lettuce/wiki/Master-Slave


If you need any support, meet lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues


Commands
--------
* Add support for CLUSTER BUMPEPOCH command #179
* Support extended MIGRATE syntax #197
* Add support for GEORADIUS STORE and STOREDIST options #199
* Support SCAN in RedisCluster #201
* Add support for BITFIELD command #206
* Add zadd method accepting ScoredValue #210
* Add support for SPOP key count #235
* Add support for GEOHASH command #239
* Add simple interface for custom command invocation for sync/async/reactive APIs #245

Enhancements
------------
* Cluster pub/sub and resilient subscriptions #138 (Thanks to @jpennell)
* Reactive API: Emit items during command processing #178
* Allow configuration of max redirect count for cluster connections #191
* Improve SCAN API #208
* Support Redis Cluster with SSL #209
* Improve CommandHandler locking #211
* Improve command encoding of singular commands and command batches #212 (Thanks to @cwolfinger)
* Add log statement for resolved address #218 (Thanks to @mzapletal)
* Apply configured password/database number in MasterSlave connection #220
* Improve command draining in flushCommands #228 (Thanks to @CodingFabian)
* Support dynamic master/slave connections #233
* Expose DNS Resolver #236
* Make latencyutils and commons-pool2 dependencies optional #237
* Support adaptive cluster topology refreshing and static refresh sources #240 (Thanks to @RahulBabbar)
* Add static builder() methods to builders enhancement #248
* Add factory for reconnection delay enhancement #250
* Add integer cache for CommandArgs enhancement #251

Fixes
-----
* pfmerge invokes PFADD instead of PFMERGE #158 (Thanks to @christophstrobl)
* Fix NPE in when command output is null #187 (Thanks to @rovarghe)
* Set interrupted bit after catching InterruptedException #192
* Lettuce fails sometimes at shutdown: DefaultClientResources.shutdown #194
* Extensive calls to PooledClusterConnectionProvider.closeStaleConnections #195
* Shared resources are closed altough still users are present #196
* Lettuce 4.1 does not repackage new dependencies #198 (Thanks to @    CodingFabian)
* Fix NPE in CommandHandler.write (Thanks to @cinnom) #213
* Gracefully shutdown DefaultCommandLatencyCollector.PAUSE_DETECTOR #223 (Thanks to @sf-git and @johnou)
* RedisClient.connect(RedisURI) fails for unix socket based URIs #229 (Thanks to @nivekastoreth)
* HdrHistogram and LatencyUtils are not included in binary distribution #231
* Cache update in Partitions is not thread-safe #234
* GEORADIUSBYMEMBER, GEORADIUS and GEOPOS run into NPE when using Redis Transactions #241
* LettuceFutures.awaitAll throws RedisCommandInterruptedException when awaiting failed commands #242
* Fix command sequence on connection activation #253 (Thanks to @long-xuan-nguyen)
* Cluster topology refresh: Failed connections are not closed bug #255
* Cluster topology refresh tries to connect twice for failed connection attempts #256
* Connection lifecycle state DISCONNECTED is considered a connected sate #257
* Writing commands while a disconnect is in progress leads to a race-condition #258
* Canceled commands lead to connection desynchronization #262 (Thanks to @long-xuan-nguyen)

Other
------
* Switch remaining tests to AssertJ #13
* Promote 4.x branch to main branch #155
* Add Wiki documentation for disconnectedBehavior option in ClientOptions #188
* Switch travis-ci to container build #203
* Refactor Makefile #207
* Code cleanups #215
* Reduce Google Guava usage #217
* Improve contribution assets #219
* Ensure OSGi compatibility #232
* Upgrade netty to 4.0.36.Final #238

lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki