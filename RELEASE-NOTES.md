Lettuce 6.0.4 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.0.4 service release!
This release ships with bugfixes and selected enhancements along with dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.0.4.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 6.x. In terms of Java runtime, Lettuce requires at least Java 8 and works with Java 16. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.0.4.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.0.4.RELEASE/api/

Enhancements
------------

* Add overload to ScanArgs to accept a binary `match` #1675
* Allow configuring a default ThreadFactoryProvider to create ClientResources #1711

Fixes
-----

* Fix potential null-dereference in `CommandHandler` #1703 (Thanks to @wbzj1110)
* `RedisPubSubCommands`,`RedisClusterPubSubCommands`, and other types missing
  in `reflect-config.json` #1710

Other
-----

* Upgrade to Netty 4.1.63.Final #1696
* RedisURI: improve deprecation notice in withPassword() #1707 (Thanks to @perlun)
* Upgrade to Reactor 3.3.16.RELEASE #1720
