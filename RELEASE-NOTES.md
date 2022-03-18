Lettuce 6.1.8 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.1.8 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.1.8.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 6.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 17. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.1.8.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.1.8.RELEASE/api/

Enhancements
------------
* Do not use pause detector for `CommandLatencyCollector` by default #1995
* Rename `ReadFrom.NEAREST` to `LOWEST_LATENCY` to clarify its functionality #1997
* Add support for REPLICAOF and CLUSTER REPLICAS commands #2020
* Improving performance of RedisAdvancedClusterAsyncCommandsImpl::mget #2042
* Redis cluster refresh of large clusters keeps I/O threads busy #2045

Fixes
-----
* `CommandArgs` using `Long.MIN_VALUE` results in `0` #2019
* `SslClosedEngineException` thrown after exceeding connection init timeout #2023
* `NodeTopologyView` passes null-Value to `StringReader` constructor #2035
* MasterReplicaConnectionProvider with zero connections causes IllegalArgumentException #2036

Other
-----
* Docs have some minor spacing and grammar issues #1993
* Fix a little typo #2003
* Use HTTPS for links #2026
* Separate profile to support m1 chip #2043
* Upgrade to netty 4.1.75.Final #2047
