Lettuce 5.0.3 RELEASE NOTES
===========================

This is the third bugfix release for Lettuce 5 shipping with 19 tickets resolved. This release fixes an issue that left cluster topology refresh connections open. Besides that, this release contains bugfixes, optimizations, and improved resilience behavior.
 
Upgrading is strongly recommended for Redis Cluster users.  

Reference documentation: https://lettuce.io/core/release/reference/.
JavaDoc documentation: https://lettuce.io/core/release/api/.

```xml
<dependency>
  <groupId>io.lettuce</groupId>
  <artifactId>lettuce-core</artifactId>
  <version>5.0.3.RELEASE</version>
</dependency>
```

You can find the full change log at the end of this document. Thanks to all contributors that made Lettuce 5.0.3.RELEASE possible.

Lettuce 5.0.3.RELEASE requires Java 8 and cannot be used with Java 6 or 7.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/

Enhancements
------------
* Use object pooling for collections inside a single method and Command/CommandArgs with a small scope #459
* CommandHandler.write() is O(N^2) #709 (Thanks to @gszpak)
* Optimize Partitions/RedisClusterNode representation #715
* Unnecessary copying of byteBuf in CommandHandler.decode() #725 (Thanks to @gszpak)

Fixes
-----
* Mono returned by RedisPubSubReactiveCommands#subscribe does not return result #717 (Thanks to @ywtsang)
* RuntimeExceptions thrown by implementations of RedisCodec do not fail TransactionCommands #719 (Thanks to @blahblahasdf)
* Connection Leak in Cluster Topology Refresh #721 (Thanks to @cweitend)
* RedisPubSubAdapter.message() being called with wrong channel #724 (Thanks to @adimarco)
* Batched commands may time out although data was received #729
* DefaultEndpoint future listener recycle lose command context on requeue failures #734 (Thanks to @gszpak)
* firstResponseLatency is always negative #740 (Thanks to @nickvollmar)
* EXEC does not fail on EXECABORT #743 (Thanks to @dmandalidis)

Other
-----
* Upgrade to Netty 4.1.22 #744
* Upgrade to RxJava 1.3.7 #745
* Upgrade to Spring Framework 4.3.14 #746
* Upgrade to Mockito 2.17 #747
* Upgrade to AssertJ 3.9.1 #748
* Upgrade to Log4j 2.11.0 #749
* Upgrade to commons-lang3 3.7 #750


Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
