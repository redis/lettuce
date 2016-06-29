lettuce 4.2.1 RELEASE NOTES
===========================

This is a bugfix release that fixes issues with reconnection, connection handling
and unexpected connection termination. lettuce 4.2.1 is also compatible with netty 4.0 and 4.1.

lettuce 4.2.0 requires Java 8 and cannot be used with Java 6 or 7.


Commands
--------
* Add support for TOUCH command #270

Fixes
-----
* Add missing package-info #263
* Fix JavaDoc for blocking list commands #272
* Apply synchronous command timeout from the given context using RedisClient #273
* Ensure netty 4.1 compatibility #274
* Unschedule topology refresh on cluster client shutdown #276
* Record command upon write #277
* Disable ConnectionWatchdog when closing a disconnected connection #278
* Perform selective relocation of org.* packages #280
* Remove use of internal rx OperatorConcat class #286 (Thanks to HaloFour)
* Fix experimental byte array Codec #288
* Guard command encoding against null #291 (Thanks to @christophstrobl)

Other
------
* Upgrade netty to 4.0.37.Final #294
* Upgrade rxjava to 1.1.6 #295


lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki
