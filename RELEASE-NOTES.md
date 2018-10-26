Lettuce 5.1.2 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.1.2 service release! 
This release ships with 9 tickets fixed. Upgrading is strongly recommended 
for users of the  reactive API.

Thanks to all contributors who made Lettuce 5.1.2.RELEASE possible.

Lettuce requires a minimum of Java 8 to build and run and #RunsLikeHeaven on Java 11. 
It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.1.2.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.1.2.RELEASE/api/

Fixes
-----
* Revert generics signature change in ConnectionPoolSupport #886
* ClassCastException occurs when executing RedisClusterClient::connectPubSub with global timeout feature #895 (Thanks to @be-hase)
* Flux that reads from a hash, processes elements and writes to a set, completes prematurely #897 (Thanks to @vkurland)
* Fixed stackoverflow exception inside CommandLatencyCollectorOptions #899 (Thanks to @LarryBattle)

Other
-----
* Upgrade to Redis 5 GA #893
* Upgrade to RxJava 2.2.3 #901
* Upgrade to Spring Framework 4.3.20.RELEASE #902
* Upgrade to netty 4.1.30.Final #903
* Upgrade to Reactor Core 3.2.2.RELEASE #904
