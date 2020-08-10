Lettuce 5.3.3 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.3.3 service release! 
This release ships with 7 tickets fixed along with dependency upgrades. 
The upgrade is recommended for Redis Cluster users as it fixes a resource leak that leads to lingering connections caused by topology refresh.
 
Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 5.3.3.RELEASE possible.
Lettuce requires a minimum of Java 8 to build and run and is compatible with Java 16. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements, and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.3.3.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.3.3.RELEASE/api/

Enhancements
------------
* Feature request: add a cluster-capable version of `flushallAsync` #1359 (Thanks to @jchambers)
* BoundedAsyncPool object is ready to be manipulated with even though a connection is not created yet #1363 (Thanks to @little-fish)

Fixes
-----
* Lingering topology refresh connections when using dynamic refresh sources #1342 (Thanks to @tpf1994)
* Wrong cast in StringCodec may lead to IndexOutOfBoundsException #1367 (Thanks to @dmandalidis)

Other
-----
* Deprecate our Spring support classes #1357
* Consistently translate execution exceptions #1370
* Upgrade to Reactor 3.3.9.RELEASE #1384
