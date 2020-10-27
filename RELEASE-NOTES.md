Lettuce 6.0.1 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.0.1 service release! 
This release ships with bugfixes and selected enhancements along with dependency upgrades. 
 
Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.0.1.RELEASE possible.
Lettuce 6 supports Redis 2.6+ up to Redis 6.x. In terms of Java runtime, Lettuce requires at least Java 8 and works with Java 15. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements, and releases): https://groups.google.com/g/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.0.1.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.0.1.RELEASE/api/

Enhancements
------------
* Add anyReplica setting for ReadFrom #1444 (Thanks to @omer-cilingir)

Fixes
-----
* Fix EXEC without MULTI when using coroutines over async #1441 (Thanks to @sokomishalov)
* Lettuce with Tracing enabled fails to connect to a Redis Sentinel #1470 (Thanks to @jsonwan)
* Lettuce doesn't handle deleted stream items (NullPointerException) #1474 (Thanks to @chemist777)

Other
-----
* Fix integration test password #1445
* Un-Deprecate io.lettuce.core.LettuceFutures #1453 (Thanks to @andrewsensus)
* Switch to Flux/Mono.expand(…) for ScanStream #1458
* Enable TCP NoDelay by default #1462
* Adapt tests to changed Redis response #1473
* Upgrade dependencies #1476
* Remove JUnit 4 dependency management #1477
