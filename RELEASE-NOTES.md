lettuce 4.1.2 RELEASE NOTES
=========================
lettuce 4.1.2 is a bugfix release.

Fixes
-----
* Add log statement for resolved address #218 (Thanks to @mzapletal)
* Apply configured password/database number in MasterSlave connection #220
* Lazy initialization of PauseDetector and graceful shutdown #223 (Thanks to @sf-git)
* Fix RedisURI validation #229 (Thanks to @nivekastoreth)
* Add latencyutils and hdrhistogram to binary distribution #231

lettuce requires a minimum of Java 8 to build and run. It is tested continuously against the latest Redis source-build.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
                or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki