lettuce 4.3.1 RELEASE NOTES
===========================

This release fixes several issues that remained undiscovered for quite a while, 
it ships with [9 tickets](https://github.com/mp911de/lettuce/milestone/25?closed=1) fixed.

Find the full change log at the end of this document.

Thanks to all contributors who made lettuce 4.3.1 possible.


Fixes
-----
* Apply proxy wrapper to obtained pooled connections #411 (Thanks to @krisjey)
* Allow databases greater than 15 in RedisURI greater 15 #420 (Thanks to @ChitraGuru)
* Expose a protected default constructor for RedisClusterClient #438 (Thanks to @wuhuaxu)
* RoundRobinSocketAddressSupplier may contain more RedisClusterNodes than the current topology view #440 (Thanks to kojilin)
* Partitions.addPartition and reload not correctly synchronized #442
* pingBeforeActivateConnection and authentication fails using Redis Sentinel #448 (Thanks to @coolmeen)
* PooledClusterConnectionProvider.close does not close connections #460

Other
------
* Use EnumSet to determine Read-Only commands #457
* Upgrade to TravisCI trusty #461


lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki