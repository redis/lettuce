Lettuce 6.0.8 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.0.8 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.0.8.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 6.x. In terms of Java runtime, Lettuce requires at least Java 8 and works with Java 17. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.0.8.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.0.8.RELEASE/api/

Fixes
-----
* SSL CN name checked failed after Redis Sentinel failover #1812
* `ClassCastException` when no `LATENCY_UTILS_AVAILABLE` or `HDR_UTILS_AVAILABLE` #1829
* Static command interface methods are erroneously verified against command names #1833
* Coroutine commands that result in a `Flow` can hang #1837

Other
-----
* Upgrade dependencies #1853
* Upgrade build to Java 17 #1854
