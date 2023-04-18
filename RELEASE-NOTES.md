Lettuce 6.2.4 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.2.4 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.2.4.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 19. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.2.4.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.2.4.RELEASE/api/

Enhancements
------------
* Add capability of FailOver with takeOver option #2358
* Improve `AdaptiveRefreshTriggeredEvent` to provide the cause and contextual details #2338
* Refine `RedisException` instantiation to avoid exception instances if they are not used #2353

Fixes
-----
* Fix long overflow in `RedisSubscription#potentiallyReadMore` #2383
* Consistently implement CompositeArgument in arg types #2387

Other
-----
* README.md demo has a error #2377
* Fix Set unit test sscanMultiple fail in redis7 #2349

