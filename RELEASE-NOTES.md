Lettuce 5.3.5 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.3.5 service release! 
This release ships with 5 tickets fixed along with dependency upgrades. 
 
Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 5.3.5.RELEASE possible.
Lettuce 6 supports Redis 2.6+ up to Redis 6.x. In terms of Java runtime, Lettuce requires at least Java 8 and works with Java 15. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements, and releases): https://groups.google.com/g/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.3.5.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.3.5.RELEASE/api/

Fixes
-----
* Lettuce with Tracing enabled fails to connect to a Redis Sentinel #1470 (Thanks to @jsonwan)
* Lettuce doesn't handle deleted stream items (NullPointerException) #1474 (Thanks to @chemist777)

Other
-----
* Correctly report isDone if Command completed with completeExceptionally #1433
* Upgrade dependencies #1476
* Remove JUnit 4 dependency management #1477

