Lettuce 5.1.6 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.1.6 service release! 
This release ships bug fixes and dependency upgrades. Upgrading to
the new version is recommended.

Thanks to all contributors who made Lettuce 5.1.6.RELEASE possible.

Lettuce requires a minimum of Java 8 to build and run and #RunsLikeHeaven on Java 11. 
It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.1.6.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.1.6.RELEASE/api/

Enhancements
------------
* Improve mutators for ClientOptions, ClusterClientOptions, and ClientResources #1003

Fixes
-----
* ClassCastException occurs when using RedisCluster with custom-command-interface and Async API #994 (Thanks to @tamanugi)
* Application-level exceptions in Pub/Sub notifications mess up pub sub decoding state and cause timeouts #997 (Thanks to @giridharkannan)
* RedisClient.shutdown hangs because event loops terminate before connections are closed #998 (Thanks to @Poorva17)

Other
-----
* Upgrade to Reactor Core 3.2.8.RELEASE #1006

