Lettuce 5.2.1 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.2.1 release! 
This release ships with mostly bug fixes and dependency upgrades addressing 9 tickets in total.
 
Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 5.2.1.RELEASE possible.
Lettuce requires a minimum of Java 8 to build and run and is compatible with Java 13. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements, and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.2.1.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.2.1.RELEASE/api/

Fixes
-----
* StackOverflowError in RedisPublisher #1140 (Thanks to @csunwold)
* Incorrect access on io.lettuce.core.ReadFrom.isOrderSensitive() #1145 (Thanks to @orclev)
* Consider ReadFrom.isOrderSensitive() in cluster scan command #1146
* Improve log message for nodes that cannot be reached during reconnect/topology refresh #1152 (Thanks to @drewcsillag)

Other
-----
* Simplify condition to invoke "resolveCodec" method in AnnotationRedisCodecResolver #1149 (Thanks to @machi1990)
* Upgrade to netty 4.1.43.Final #1161
* Upgrade to RxJava 2.2.13 #1162
* Add ByteBuf.touch(…) to aid buffer leak investigation #1164
* Add warning log if MasterReplica(…, Iterable<RedisURI>) contains multiple Sentinel URIs #1165
