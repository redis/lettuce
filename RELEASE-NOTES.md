lettuce 3.4.2 RELEASE NOTES
=========================

lettuce 3.4.2 is a bugfix release to fix issues with shared client resources and the shaded jar.

Fixes
-----
* Switch tests to AssertJ #13
* Do not shut down the event executor in AbstractRedisClient #194
* Remove initial call to PooledClusterConnectionProvider.closeStaleConnections #195
* Use RefCounters to track open resources #196
* Adjust dependency repackaging for rx-java and hdrutils in shaded jar #198 (Thanks to @CodingFabian)
* Fix comparison for computationThreadPoolSize #205 (Thanks to @danhyun)

Other
-----
* Switch travis-ci to container build #203

lettuce requires a minimum of Java 8 to build and Java 6 run. It is tested
continuously against Redis 3.0 and the unstable branch.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
                or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki
