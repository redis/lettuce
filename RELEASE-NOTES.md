Lettuce 5.1.3 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.1.3 service release! 
This release ships with 9 tickets fixed. Upgrading is strongly recommended 
for users of Pub/Sub with the byte-array codec.

Thanks to all contributors who made Lettuce 5.1.3.RELEASE possible.

Lettuce requires a minimum of Java 8 to build and run and #RunsLikeHeaven on Java 11. 
It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.1.3.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.1.3.RELEASE/api/

Enhancements
------------
* Add shutdown logging to client, ClientResources, and EventLoopGroupProvider #918
* Optimization: Use Cluster write connections for read commands when using ReadFrom.MASTER #923
* ByteBuf.release() was not called before it's garbage-collected #930 (Thanks to @zhouzq)

Fixes
-----
* PubSubEndpoint.channels and patterns contain duplicate binary channel/pattern names #911 (Thanks to @lwiddershoven)
* DefaultCommandMethodVerifier reports invalid parameter count #925 (Thanks to @GhaziTriki)

Other
-----
* Document MasterSlave connection behavior on partial node failures #894 (Thanks to @jocull)
* Upgrade to Reactor Core 3.2.3.RELEASE #931
* Upgrade to netty 4.1.31.Final #932
* Upgrade to RxJava 2.2.4 #933
