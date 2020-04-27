Lettuce 5.3.0 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.3.0 release! 
We decided to add another release before Lettuce goes 6.0. Our wiki explains [which versions are supported](https://github.com/lettuce-io/lettuce-core/wiki/Lettuce-Versions).
This release ships with 28 tickets fixed and contains a couple API revisions. Most notable is the revised SSL configuration API for PEM-encoded certificate support and TLS protocol selection.
Note that this release ships also a change in the `randomkey()` method signature fixing the return type.

Please also note carefully if you're using zero-timeouts. With this release, zero timeouts map to infinite command timeouts. 
While the documentation was already correct about that issue, the implementation didn't consider zero-timeouts.
 
Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 5.3.0.RELEASE possible.
Lettuce requires a minimum of Java 8 to build and run and is compatible with Java 14. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements, and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.3.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.3.0.RELEASE/api/

Enhancements
------------
* Allow client to pick a specific TLS version and introduce PEM-based configuration #1167 (Thanks to @amohtashami12307)
* Add mutate() to SocketOptions #1193
* Add CLIENT ID command #1197
* Relax field count check in CommandDetailParser #1200
* Lettuce not able to reconnect automatically to SSL+authenticated ElastiCache node #1201 (Thanks to @chadlwilson)
* HMSET deprecated in version 4.0.0 #1217 (Thanks to @hodur)
* Allow selection of Heap or Direct buffers for CommandHandler.buffer #1223 (Thanks to @dantheperson)
* Support JUSTID flag of XCLAIM command #1233 (Thanks to @christophstrobl)
* Add support for KEEPTTL with SET #1234
* Add support for RxJava 3 #1235
* Introduce ThreadFactoryProvider to DefaultEventLoopGroupProvider for easier customization #1243

Fixes
-----
* BoundedAsyncPool doesn't work with a negative maxTotal #1181 (Thanks to @sguillope)
* Issuing `GEORADIUS_RO` on a replica fails when no masters are available. #1198 (Thanks to @leif-erikson)
* TLS setup fails to a master reported by sentinel #1209 (Thanks to @ae6rt)
* Lettuce metrics creates lots of long arrays, and gives out of memory error.  #1210 (Thanks to @omjego)
* CommandSegments.StringCommandType does not implement hashCode()/equals() #1211
* Unclear documentation about quiet time for RedisClient#shutdown  #1212 (Thanks to @LychakGalina)
* Write race condition while migrating/importing a slot #1218 (Thanks to @phyok)
* randomkey return V not K #1240 (Thanks to @hosunrise)
* ConcurrentModificationException iterating over partitions #1252 (Thanks to @johnny-costanzo)
* Replayed activation commands may fail because of their execution sequence #1255 (Thanks to @robertvazan)
* Fix infinite command timeout #1260
* Connection leak using pingBeforeActivateConnection when PING fails #1262 (Thanks to @johnny-costanzo)
* Lettuce blocked when connecting to Redis #1269 (Thanks to @jbyjby1)
* Stream commands are not considered for ReadOnly routing  #1271 (Thanks to @redviper)

Other
-----
* Disable RedisURIBuilderUnitTests failing on Windows OS #1204 (Thanks to @kshchepanovskyi)
* Provide a default port(DEFAULT_REDIS_PORT) to RedisURI's Builder #1205 (Thanks to @hepin1989)
* Un-deprecate ClientOptions.pingBeforeActivateConnection #1208
* Upgrade dependencies (netty to 4.1.49.Final) #1224, #1225, #1277, #1239
* RedisURI class does not parse password when using redis-sentinel #1232 (Thanks to @kyrogue)
* Reduce log level to DEBUG for native library logging #1238 (Thanks to @DevJoey)
* Upgrade to stunnel 5.56 #1246
* Add build profiles for multiple Java versions #1247
* Replace outdated Sonatype parent POM with plugin definitions #1258
* Upgrade dependencies #1259
* Upgrade to RxJava 3.0.2 #1261
* Reduce min thread count to 2 #1278
