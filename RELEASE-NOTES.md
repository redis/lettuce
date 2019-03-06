Lettuce 5.1.5 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.1.5 service release! 
This release ships with two critical fixes, primarily for the recently introduced reactive signal emission on
non-I/O threads, so reactive single-connection systems can utilize more threads for
item processing and are not limited to a single thread.
The issue that is being fixed is retention of signal ordering as signals can be dispatched out of order.

Thanks to all contributors who made Lettuce 5.1.5.RELEASE possible.

Lettuce requires a minimum of Java 8 to build and run and #RunsLikeHeaven on Java 11. 
It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.1.5.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.1.5.RELEASE/api/

Fixes
-----
* Result is lost when published on another executor #986 (Thanks to @trueinsider)
* Cancel ClusterTopologyRefreshTask in RedisClusterClient.shutdownAsync() #989 (Thanks to @johnsiu)

Other
-----
* Upgrade to AssertJ 3.12.0 #983
* Upgrade to AssertJ 3.12.1 #991
