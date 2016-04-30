lettuce 3.4.3 RELEASE NOTES
=========================

lettuce 3.4.3 is a bugfix release.

Fixes
-----
* Add log statement for resolved address #218 (Thanks to @mzapletal)
* Lazy initialization of PauseDetector and graceful shutdown #223 (Thanks to @sf-git)
* Fix RedisURI validation #229 (Thanks to @nivekastoreth)
* Add latencyutils and hdrhistrogram to binary distribution #231


lettuce requires a minimum of Java 8 to build and Java 6 run. It is tested
continuously against Redis 3.0 and the unstable branch.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
                or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki
