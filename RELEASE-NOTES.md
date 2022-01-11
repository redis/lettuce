Lettuce 6.1.6 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.1.6 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.1.6.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 6.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 17. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.1.6.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.1.6.RELEASE/api/

Enhancements
------------
* Specify concrete scheduler name #1898
* Avoid routing to cluster replicas that are loading data #1923
* Introduce `nodeFilter` `Predicate` to filter `Partitions` #1942
* Add exception type for `READONLY` errors #1943

Fixes
-----
* io.lettuce.core.protocol.CommandExpiryWriter cannot be cast to io.lettuce.core.protocol.DefaultEndpoint #1882
*  Attempting to execute commands during a JVM shutdown will cause a RedisConnectionFailureException / IllegalStateException: Shutdown in progress #1883
* Tracing incorrect when command execute timeout #1901
* Run flush commands with `FlushMode` on upstreams only #1908
* `RoundRobinSocketAddressSupplier` is not refreshing when `RedisURI` changes #1909
* `ClientResources.mutate()` leads to `DefaultClientResources was not shut down properly` if the original instance is GC'd #1917
* `BoundedPoolConfig` with `-1`  as `maxTotal` does not work as unlimited pool #1953

Other
-----
* Upgrade build to Java 17 #1854
* A question about the letuce CI project view #1914
* Bump log4j-core to 2.15.0 #1934
* Upgrade log4j to 2.17 to address CVE-2021-45105 #1941
* Upgrade to Netty 4.1.72.Final #1950
* Extend copyright license years to 2022 #1952
* `RedisCoroutinesCommands` calls are locked after calling multi() #1954
* Release 6.1.6.RELEASE #1961
* Upgrade dependencies #1962
