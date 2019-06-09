Lettuce 5.1.7 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.1.7 service release! 
This release ships bug fixes and dependency upgrades. Upgrading to
the new version is recommended.

Thanks to all contributors who made Lettuce 5.1.7.RELEASE possible.

Lettuce requires Java 8 to build and run and #RunsLikeHeaven on Java 11. 
It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at:

* Google Group (General discussion, announcements and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.1.7.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.1.7.RELEASE/api/

Enhancements
------------
* TopologyComparators performance issue #1011 (Thanks to @alessandrosimi-sa)
* Attach topology retrieval exceptions when Lettuce cannot retrieve a topology update #1024 (Thanks to @StillerRen)

Fixes
-----
* "Knowing Redis" section in documentation has the wrong link for meanings  #1050 (Thanks to @Raghaava)

Other
-----
* Upgrade to netty 4.1.35.Final #1017
* Migrate off TopicProcessor to EmitterProcessor #1032
* Upgrade to Netty 4.1.36.Final #1033
* Upgrade to JUnit 5.4.2 #1034
* Upgrade to Mockito 2.27.0 #1035
* Upgrade to RxJava 2.2.8 #1036
* Upgrade build plugin dependencies #1037
* RedisURI: fix missing "the" in Javadoc #1049 (Thanks to @perlun)
* Upgrade to RxJava 2.2.9 #1054
* Upgrade to jsr305 3.0.2 #1055
* Upgrade TravisCI build to Xenial/Java 11 #1056
