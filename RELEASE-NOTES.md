Lettuce 5.0.0 RC2 RELEASE NOTES
==================================

This release ships with 12 issues resolved and upgraded dependencies that are 
breaking for Lettuce. 

Browse the docs at https://lettuce.io/core/snapshot/reference/.

This release asserts compatibility in class-path mode with Java 9 requiring 
at least netty 4.1.11.Final.

Thanks to all contributors that made Lettuce 5.0.0.RC2 possible.

Lettuce 5.0.0.RC2 requires Java 8 and cannot be used with Java 6 or 7.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/

Enhancements
------------
* Optimization for ValueListOutput #573 (Thanks to @CodingFabian)
* Enhance argument caching #575
* Introduce MethodTranslator caching #580
* Execute RedisAdvancedClusterCommands.scriptLoad(…) on all nodes #590

Fixes
-----
* NPE in RedisStateMachine #576 (Thanks to @nikolayspb)
* Prevent dead listener callbacks in shutdown sequence #581
* Javadoc in BitFieldArgs contains invalid links #583
* Fix IllegalArgumentException in RedisClient.connectSentinel #588 (Thanks to @andrewsensus)
* UnsupportedOperationException (List#add) in NestedMultiOutput #589 (Thanks to @zapl)
* GEOPOS fails with a single member in the var args #591 (Thanks to @FerhatSavci)

Other
------
* Upgrade to Reactor Core 3.1.0 RC1 #602
* Upgrade to netty 4.1.15 #600

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
