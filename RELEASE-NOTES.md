Lettuce 5.1.8 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.1.8 service release! 
This release ships bug fixes and dependency upgrades. Upgrading to
the new version is recommended.

Thanks to all contributors who made Lettuce 5.1.8.RELEASE possible.

Lettuce requires Java 8 to build and run and #HaveYouSeen, it runs on Java 13. 
It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at:

* Google Group (General discussion, announcements and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.1.8.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.1.8.RELEASE/api/

Enhancements
------------
* TopologyComparators performance issue #1011 (Thanks to @alessandrosimi-sa)
* Attach topology retrieval exceptions when Lettuce cannot retrieve a topology update #1024 (Thanks to @StillerRen)

Fixes
-----
* ClassCastException occurs when using RedisCommandFactory with custom commands #1075 (Thanks to @mdebellefeuille)
* EventLoop thread blocked by EmitterProcessor.onNext(…) causes timeouts #1086 (Thanks to @trishaarao79)
* RedisClusterNode without slots is never considered having same slots as an equal object #1089 (Thanks to @y2klyf)

Other
-----
* Upgrade to OpenWebBeans 2.0.11 #1062
* Upgrade to netty 4.1.38.Final #1093
* Upgrade to Reactor Core 3.2.11.RELEASE #1094
* Upgrade to Mockito 3.0 #1095
* Upgrade to JUnit 5.5.1 #1096
* Update plugin versions #1098
