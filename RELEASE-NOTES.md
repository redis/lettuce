Lettuce 6.1 RC1 RELEASE NOTES
==============================

The Lettuce team is delighted to announce the availability of Lettuce 6.1 RC1.

This is a massive release thanks to all the community contributions. This release ships mostly with command updates to support Redis 6.2.

Besides that, Lettuce publishes now events to Java Flight Recorder if your Java runtime supports JFR events (JDK 8 update 262 or later).

Lettuce 6 supports Redis 2.6+ up to Redis 6.x. In terms of Java runtime, Lettuce requires at least Java 8 and works with Java 16.

Thanks to all contributors who made Lettuce 6.1.0.RC1 possible.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.1.0.RC1/reference/
* Javadoc: https://lettuce.io/core/6.1.0.RC1/api/

Commands
------------
* Add support for IDLE option in XPENDING #1537
* Support for ACL commands #1538 (Thanks to @GraemeMitchell84)
* Add support for MINID trimming strategy and the LIMIT argument to XADD and XTRIM #1582
* Add support for GEOADD [CH] [NX|XX] options #1584
* Add support for HRANDFIELD and ZRANDMEMBER commands #1605
* Add support for GETEX, GETDEL commands #1606
* Add FlushMode to FLUSHALL and FLUSHDB, and to SCRIPT FLUSH #1608
* Add support for MIGRATE AUTH2 option #1633
* Add support for RESTORE ABSTTL #1634

Enhancements
------------
* Add support for Java Flight Recorder #1430
* Add hostname support to Sentinel #1635
* Introduce GeoValue value type #1643

Fixes
-----
* XREADGROUP skips messages if body is nil #1622 (Thanks to @asalabaev)
* TransactionCommand applied incorrectly #1625 (Thanks to @checky)
* ScoredValueUnitTests test fail #1647 (Thanks to @dengliming)
* Create traced endpoint when ready #1653 (Thanks to @anuraaga)

Other
-----
* Ping command is not supported in Pub/Sub connection #1601 (Thanks to @TsybulskyStepan)
* Fix formatting indent in kotlin files #1603 (Thanks to @sokomishalov)
* Upgrade kotlin to 1.4.30, kotlinx-coroutines to 1.4.2 #1620 (Thanks to @sokomishalov)
* Typo in string reactive commands #1632 (Thanks to @paualarco)
* Let nullable ScoredValue factory methods return Value<V> instead of ScoredValue<V> #1644
* Cleanup MULTI state if MULTI command fails #1650
* Upgrade to Kotlin 1.4.31 #1657
* Use kotlin and kotlinx-coroutines bills of materials #1660 (Thanks to @sokomishalov)
* Fix travis links due to its domain name migration #1661 (Thanks to @sokomishalov)
* Rename zrandmemberWithscores to zrandmemberWithScores #1663 (Thanks to @dengliming)
