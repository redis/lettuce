lettuce 4.4.1 RELEASE NOTES
===========================

This release ships with [5 tickets](https://github.com/lettuce-io/lettuce-core/milestone/30?closed=1) fixed
which are all bug fixes.


If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/


Fixes
-----
* Fix IllegalArgumentException in RedisClient.connectSentinel #588
* UnsupportedOperationException (List#add) in NestedMultiOutput #589
* GEOPOS fails with a single member in the var args #591
* Reduce logging of native transport state to INFO bug #596


Other
-----
* Upgrade to netty 4.0.51/4.1.15 #600

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
