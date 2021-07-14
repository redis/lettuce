Lettuce 6.1.4 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.1.4 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.1.4.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 6.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 16. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.1.4.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.1.4.RELEASE/api/

Fixes
-----
* Exception thrown during RedisClusterClient shutdown #1800

Other
-----
* Upgrade to `netty-incubator-transport-native-io_uring` `0.0.8.Final` #1798
* Dependency upgrades #1808
