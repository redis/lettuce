lettuce 4.4.4 RELEASE NOTES
===========================

This is the fourth bugfix release for Lettuce 4.4 shipping with 12 tickets fixed along with a few dependency upgrades.
This release contains fixes for improved resilience behavior.
 
Upgrading is recommended.  

You can find the full change log at the end of this document. 
Thanks to all contributors that made Lettuce 4.4.4.Final possible.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/


Enhancements
------------
* CommandHandler.write() is O(N^2) #709 (Thanks to @gszpak)

Fixes
-----
* RuntimeExceptions thrown by implementations of RedisCodec do not fail TransactionCommands #719 (Thanks to @blahblahasdf)
* RedisPubSubAdapter.message() being called with wrong channel #724 (Thanks to @adimarco)
* firstResponseLatency is always negative #740 (Thanks to @nickvollmar)
* EXEC does not fail on EXECABORT #743 (Thanks to @dmandalidis)

Dependency upgrades
-------------------
* Upgrade to netty 4.1.22.Final #744
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
