Lettuce 5.3.6 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.3.6 service release! 
This release ships with 7 tickets fixed along with dependency upgrades. 
 
Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 5.3.6.RELEASE possible.
Lettuce 6 supports Redis 2.6+ up to Redis 6.x. In terms of Java runtime, Lettuce requires at least Java 8 and works with Java 15. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.3.6.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.3.6.RELEASE/api/

Enhancements
------------
* Add support for local addr in CLIENT KILL #1536

Fixes
-----
* LettuceStrings does not handle -nan which is returned by FT.INFO in redisearch #1482 (Thanks to @krm1312)
* Reactive stream spec violation when using command timeout #1576 (Thanks to @martin-tarjanyi)

Other
-----
* Upgrade to Netty 4.1.54.Final #1541
* netty 4.1.56 #1556 (Thanks to @sullis)
* Update copyright years to 2021 #1573
* Upgrade dependencies #1578

