lettuce 4.1.1 RELEASE NOTES
=========================
lettuce 4.1.1 is a bugfix release to fix issues with shared client resources,
the shaded jar, thread interrupts and, a bug that came back to live: pfmerge invokes PFADD instead of PFMERGE.

Two issues made it also into this release that are not bugfixes: Support CLUSTER NODES with busport and
  Allow configuration of max redirect count for cluster connections.

Fixes
-----
* pfmerge invokes PFADD instead of PFMERGE #158
* Fix NPE in when command output is null #187
* Set interrupted bit after catching InterruptedException #192
* Do not shut down the event executor in AbstractRedisClient #194
* Remove initial call to PooledClusterConnectionProvider.closeStaleConnections #195
* Use RefCounters to track open resources #196
* Adjust dependency repackaging for rx-java and hdrutils in shaded jar #198 (Thanks to @CodingFabian)
* Fix comparison for computationThreadPoolSize #205 (Thanks to @danhyun)

Other
------
* Switch tests to AssertJ #13
* Support CLUSTER NODES with busport #184
* Allow configuration of max redirect count for cluster connections #191
* Switch travis-ci to container build #203
* Refactor Makefile #207

lettuce requires a minimum of Java 8 to build and run. It is tested continuously against the latest Redis source-build.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
                or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki