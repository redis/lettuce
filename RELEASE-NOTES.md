Lettuce 6.0.2 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.0.2 service release! 
This release ships with bugfixes and selected enhancements along with dependency upgrades. 
 
Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.0.2.RELEASE possible.
Lettuce 6 supports Redis 2.6+ up to Redis 6.x. In terms of Java runtime, Lettuce requires at least Java 8 and works with Java 15. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.0.2.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.0.2.RELEASE/api/

Enhancements
------------
* API generator problems #1499 (Thanks to @sokomishalov)
* Provide VoidOutput for Fire&Forget command usage #1529 (Thanks to @jaredpetersen)
* Improve unsupported error logging for CommandOutput #1532
* Add support for local addr in CLIENT KILL #1536

Fixes
-----
* LettuceStrings does not handle -nan which is returned by FT.INFO in redisearch #1482 (Thanks to @krm1312)
* Improperly decoding command responses #1512 (Thanks to @johnny-costanzo)
* Fix timeout parameter for nanoseconds in RedisURI #1528 (Thanks to @izeye)
* Lettuce 6.0.2 fails with GraalVM 20.3 #1562 (Thanks to @atrianac)
* Reactive stream spec violation when using command timeout #1576 (Thanks to @martin-tarjanyi)

Other
-----
* Remove or replace ClusterRule #1478
* Implement set(double) in NestedMultiOutput #1486 (Thanks to @jruaux)
* Start HashWheelTimer in ClientResources to avoid blocking calls in EventLoop #1489
* Reduce build matrix to Java 8, 11, 15, and EA #1519
* Upgrade to Netty 4.1.54.Final #1541
* netty 4.1.56 #1556 (Thanks to @sullis)
* Move Mailing list forum to GitHub discussions #1557
* Let coroutines `dispatch`-method be flowable #1567 (Thanks to @sokomishalov)
* Update copyright years to 2021 #1573
* Upgrade dependencies #1578
