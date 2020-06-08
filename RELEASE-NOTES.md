Lettuce 5.3.1 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.3.1 service release! 
This release ships with 10 tickets fixed along of dependency upgrades.
Most notable changes are fixes around `PauseDetector` acquisition which may cause infinite loops during metrics logging and therefore command timeouts.
 
Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 5.3.1.RELEASE possible.
Lettuce requires a minimum of Java 8 to build and run and is compatible with Java 14. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements, and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.3.1.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.3.1.RELEASE/api/

Enhancements
------------
* Add template method for EventLoopGroup creation #1273 (Thanks to @konstantin-grits)
* Add charset option to ScanArgs.match(…) #1285 (Thanks to @gejun123456)

Fixes
-----
* PauseDetector acquisition hang in DefaultCommandLatencyCollector #1300 (Thanks to @ackerL)
* NullPointerException thrown during AbstractRedisAsyncCommands.flushCommands #1301 (Thanks to @mruki)
* xpending(K, Consumer, Range, Limit) fails with ERR syntax error using Limit.unlimited() #1302 (Thanks to @nagaran1)
* Remove duplicated command on asking #1304 (Thanks to @koisyu)

Other
-----
* Replace io.lettuce.core.resource.Futures utility with Netty's PromiseCombiner #1283
* Upgrade dependencies #1305
* Add FAQ section to reference docs #1307
