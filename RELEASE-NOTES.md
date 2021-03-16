Lettuce 6.0.3 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.0.3 service release! 
This release ships with bugfixes and selected enhancements along with dependency upgrades. 
 
Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.0.3.RELEASE possible.
Lettuce 6 supports Redis 2.6+ up to Redis 6.x. In terms of Java runtime, Lettuce requires at least Java 8 and works with Java 16. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.0.3.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.0.3.RELEASE/api/

Enhancements
------------
* Consider reinstating master-replica wording #1518 (Thanks to @perlun)
* Allow providing custom ClusterTopologyRefresh implementation. #1598 (Thanks to @alessandrosimi-sa)
* Introduce LettuceStrings.isEmpty(String) overload with optimized isEmpty checking #1609

Fixes
-----
* Sentinel lookup connection leaks if no master address reported #1558 (Thanks to @wwwjinlong)
* copyright replace bug #1589 (Thanks to @dengliming)
* NullPointerException in BoundedAsyncPool.createIdle() when availableCapacity=0 #1611 (Thanks to @fbotis)
* XREADGROUP skips messages if body is nil #1622 (Thanks to @asalabaev)
* TransactionCommand applied incorrectly #1625 (Thanks to @checky)
* Create traced endpoint when ready #1653 (Thanks to @anuraaga)

Other
-----
* Ping command is not supported in Pub/Sub connection #1601 (Thanks to @TsybulskyStepan)
* Typo in string reactive commands #1632 (Thanks to @paualarco)
* Cleanup MULTI state if MULTI command fails #1650
