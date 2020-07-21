Lettuce 5.3.2 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.3.2 service release! 
This release ships with 15 tickets fixed along with dependency upgrades. 
Most notable enhancement of this release is that Lettuce ships with configuration files for an improved experience when compiling applications to Graal Native Images which make use of Lettuce.
 
Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 5.3.2.RELEASE possible.
Lettuce requires a minimum of Java 8 to build and run and is compatible with Java 16. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements, and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.3.2.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.3.2.RELEASE/api/

Enhancements
------------
* Add support for STRALGO #1280
* Allow for more customisation of the tracing span #1303 (Thanks to @JaidenAshmore)
* Support for GraalVM Native Images #1316 (Thanks to @ilopmar)
* Add support for LPOS #1320
* reduce method(decode)'s bytecode size #1324 (Thanks to @hellyguo)
* SSL handshake doesn't respect timeouts #1326 (Thanks to @feliperuiz)

Fixes
-----
* Write race condition while migrating/importing a slot #1218 (Thanks to @phyok)
* ArrayOutput stops response parsing on empty nested arrays #1327 (Thanks to @TheCycoONE)
* Synchronous dispatch of MULTI returns null #1335 (Thanks to @tzxyz)
* RedisAdvancedClusterAsyncCommandsImpl scriptKill is incorrectly calling scriptFlush #1340 (Thanks to @azhukayak)
* RedisAdvancedClusterAsyncCommands.scriptKill now calls scriptKill instead of scriptFlush #1341 (Thanks to @dengliming)

Other
-----
* Remove JavaRuntime class and move LettuceStrings to internal package #1329
* Consistently use Javadoc wording in BoundedPoolConfig.Builder #1337 (Thanks to @maestroua)
* Upgrade to Reactor Core 3.3.8.RELEASE #1353
* Upgrade to netty 4.1.51.Final #1354
