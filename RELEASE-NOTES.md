Lettuce 4.3.3 RELEASE NOTES
==========================

This release fixes bugs and ships some enhancements. One of them adopts to Redis recent
GEORADIUS(BYMEMBER)_RO change for Redis Cluster. Find all [12 tickets](https://github.com/lettuce-io/lettuce-core/milestone/29?closed=1) assigned in GitHub's milestone view.

Lettuce was moved meanwhile to a new organization in GitHub: https://github.com/lettuce-io and
has a new website at https://lettuce.io. Maven coordinates for the 4.x branch remain stable.

Find the full change log at the end of this document.

Thanks to all contributors who made Lettuce 4.3.3 possible.

Fixes
-----
* Fix zset score parsing for infinite scores #528 (Thanks to @DarkSeraphim)
* Remove method synchronization in ConnectionPoolSupport #531 (Thanks to @DarkSeraphim)
* Activate connection before signaling activation completion #536 (Thanks to @goldobin)
* Close connections in pooling destroy hook #545 (Thanks to @robbiemc)
* Apply client name via clientSetname(…) also to default connection #563

Enhancements
------------
* Improve epoll availability detection #535
* Align log prefix for CommandHandler and ConnectionWatchdog #538
* Adopt to read-only variant GEORADIUS(BYMEMBER)_RO #564

Other
------
* Upgrade to netty 4.1.10 #532
* Adopt to changed SLOWLOG output #551
* Allow Redis version pinning for build #552
* Upgrade to netty 4.1.13/4.0.49 #565

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
