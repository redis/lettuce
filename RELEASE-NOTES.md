Lettuce 6.2.0 RELEASE NOTES
==============================

The Lettuce team is delighted to announce general availability of Lettuce 6.2.

This is a massive release thanks to all the community contributions. Most notable changes
that ship with this release are:

* Support for Redis 7.0 commands and modifier/argument updates

Lettuce 6 supports Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires
at least Java 8 and works with Java 18.

Thanks to all contributors who made Lettuce 6.2.0 possible.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.2.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.2.0.RELEASE/api/

Commands
------------

* Add support for `double` timeouts for `BZPOP` and `BLPOP`/`BRPOP` commands #1772
* Add support for `SINTERCARD` #1902
* Add support for `LMPOP` and `BLMPOP` commands #1903
* Add support for `CLUSTER DELSLOTSRANGE` and `CLUSTER ADDSLOTSRANGE` commands #1904
* Add NX/XX/GT/LT options to `EXPIRE` commands #1905
* Add support for `ZINTERCARD` command #1982
* Add support `NOW`, `FORCE` and `ABORT` args for `SHUTDOWN` command #1985
* Add support for EVAL_RO and EVALSHA_RO commands #1987
* Support `CONFIG GET/SET` with multiple parameters #1988
* Add support for CLIENT NO-EVICT command #1989
* Add support for ACL DRYRUN command #1992
* Add support for `REPLICAOF` and `CLUSTER REPLICAS` commands #2020
* Add support for `CLUSTER SHARDS` command #2025
* Add support for `XGROUP … ENTRIESREAD` #2141
* Add support for `EXPIRETIME` and `PEXPIRETIME` #2142
* Assert Redis 7.0 compatibility #2143
* Add support for `SENTINEL REPLICAS` command #2144
* Add support for `SORT_RO` #2145

Enhancements
------------

* Allow configuring a default ThreadFactoryProvider to create ClientResources #1711
* Introduce API to allow for extending RedisClusterClient and its connections #1754
* Password Rotation support - Add a CredentialProvider interface/class and set your own
  provider #1774
* Emit command latency events using JFR #1820
* Support RedisConnectionStateListener registration for individual connection #1838
* NodeSelection `dispatch` output gets reused #1876
* Specify concrete scheduler name #1898
* Avoid routing to cluster replicas that are loading data #1923
* Introduce `nodeFilter` `Predicate` to filter `Partitions` #1942
* Add exception type for `READONLY` errors #1943
* Do not use pause detector for `CommandLatencyCollector` by default #1995
* Rename `ReadFrom.NEAREST` to `LOWEST_LATENCY` to clarify its functionality #1997
* Deprecate connection-related methods on commands API #2028
* Promote `AsyncCloseable` to public API #2030
* Improving performance of RedisAdvancedClusterAsyncCommandsImpl::mget #2042
* Redis cluster refresh of large clusters keeps I/O threads busy #2045
* Reduces overhead of RedisClusterNode.forEachSlot #2058
* Implement `AutoCloseable` in `AbstractRedisClient` #2076
* StatefulRedisClusterConnectionImpl getConnection always calls connection provider with
  Intent.WRITE #2095
* Assert Redis 7.0 compatibility #2143

Fixes
-----

* Could not initialize JfrConnectionCreatedEvent #1767
* Fix potential null-dereference in CommandHandler #1703
* Cannot refresh Redis Cluster topology #1706
* `RedisPubSubCommands`,`RedisClusterPubSubCommands`, and other types missing
  in `reflect-config.json` #1710
* Additional Redis PubSub types missing from reflect-config.json #1737
* AbstractRedisClient#shutdown() never ends when Redis is unstable #1768
* Closing ClusterNodeEndpoint asynchronously doesn't retrigger buffered commands #1769
* CopyArgs should honor replace parameter value instead of doing null check only #1777
* Exception thrown during RedisClusterClient shutdown #1800
* SSL CN name checked failed after Redis Sentinel failover #1812
* `ClassCastException` when no `LATENCY_UTILS_AVAILABLE` or `HDR_UTILS_AVAILABLE` #1829
* Static command interface methods are erroneously verified against command names #1833
* Coroutine commands that result in a `Flow` can hang #1837
* Fix `ACL SETUSER` when adding specified categories #1839
* Fix `ACL SETUSER` when setting noCommands #1846
* `ACL SETUSER` not retaining argument order #1847
* io.lettuce.core.protocol.CommandExpiryWriter cannot be cast to
  io.lettuce.core.protocol.DefaultEndpoint #1882
* Attempting to execute commands during a JVM shutdown will cause a
  RedisConnectionFailureException / IllegalStateException: Shutdown in progress #1883
* Tracing incorrect when command execute timeout #1901
* Run flush commands with `FlushMode` on upstreams only #1908
* `RoundRobinSocketAddressSupplier` is not refreshing when `RedisURI` changes #1909
* `ClientResources.mutate()` leads to `DefaultClientResources was not shut down properly`
  if the original instance is GC'd #1917
* `BoundedPoolConfig` with `-1`  as `maxTotal` does not work as unlimited pool #1953
* `CommandArgs` using `Long.MIN_VALUE` results in `0` #2019
* `SslClosedEngineException` thrown after exceeding connection init timeout #2023
* `NodeTopologyView` passes null-Value to `StringReader` constructor #2035
* `MasterReplicaConnectionProvider` with zero connections
  causes `IllegalArgumentException` #2036
* `DefaultCommandLatencyCollector.PauseDetectorWrapper` defines `PauseDetector` in method
  signatures #2056
* decorrelatedJitter and fullJitter delay calculation overflows #2115
* XPending command cannot be called with IDLE param without defining CONSUMER #2132
* Routing XREADGROUP command in Redis cluster #2140
* Ensure `PooledClusterConnectionProvider` uses correct write-connection #2146
* CommandHandler's validateWrite method has a calculated overflow problem #2150

Other
-----

* Edit wiki for versioned pages #1699
* RedisURI: improve deprecation notice in withPassword() #1707
* Improve kotlin api generator to provide replacement for a deprecated method #1759
* cancelCommandsOnReconnectFailure should have appropriate warnings. #1787
* Upgrade to Reactor 3.4 #1804
* Upgrade to Commons Pool 2.10.0 #1805
* Upgrade to Micrometer 1.7 #1806
* Upgrade to Kotlin 1.5 #1807
* Dependency upgrades #1808
* Fixed obvious typo in MicrometerOptions.Builder.minLatency(Duration) #1826
* Upgrade dependencies #1853
* Upgrade build to Java 17 #1854
* Deprecate `AbstractRedisClient.setDefaultTimeout(…)` to avoid ambiguity
  with `RedisURI.getTimeout()` #1868
* Replace reactor's deprecated API #1885
* A question about the letuce CI project view #1914
* Bump log4j-core to 2.15.0 #1934
* Upgrade log4j to 2.17 to address CVE-2021-45105 #1941
* Extend copyright license years to 2022 #1952
* `RedisCoroutinesCommands` calls are locked after calling multi() #1954
* Add tests for Sentinel with ACL authentication #1956
* Fix Javadoc link in README #1959
* Upgrade dependencies #1962
* Docs have some minor spacing and grammar issues #1993
* Fix a little typo #2003
* Use HTTPS for links #2026
* Separate profile to support m1 chip #2043
* Bump netty to fix vulnerability issue #2098
* Move off deprecated `DirectProcessor` API #2136
* Upgrade to Netty 4.1.79.Final #2153
* Upgrade to Micrometer 1.9.2 #2154
* Upgrade to Reactor 3.4.21 #2155
* Upgrade to Reactive Streams 1.0.4 #2156
