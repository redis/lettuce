Lettuce 6.1.9 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.1.9 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.1.9.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 17. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.1.9.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.1.9.RELEASE/api/

Enhancements
------------

* Reduces overhead of RedisClusterNode.forEachSlot #2058

Fixes
-----

* decorrelatedJitter and fullJitter delay calculation overflows #2115
* XPending command cannot be called with IDLE param without defining CONSUMER #2132
* Routing XREADGROUP command in Redis cluster #2140
* CommandHandler's validateWrite method has a calculated overflow problem #2150

Other
-----

* Bump netty to fix vulnerability issue #2098
* Upgrade to Netty 4.1.79.Final #2153
* Upgrade to Reactor 3.4.20 #2155
* Upgrade to Reactive Streams 1.0.4 #2156
