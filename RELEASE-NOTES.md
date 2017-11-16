Lettuce 5.0.1 RELEASE NOTES
===========================

This is the first bugfix release for Lettuce 5 shipping with 13 tickets fixed. It contains 
fixes for resilience and error scenario handling and the dynamic command interfaces support. 
It also fixes an issue in the reactive API when using two or more threads to consume a 
RedisPublisher where it's possible the publisher never completes.

Upgrading is strongly recommended upgrade when using the reactive API.  

Reference documentation: https://lettuce.io/core/release/reference/.
JavaDoc documentation: https://lettuce.io/core/release/api/.

```xml
<dependency>
  <groupId>io.lettuce</groupId>
  <artifactId>lettuce-core</artifactId>
  <version>5.0.1.RELEASE</version>
</dependency>
```

You can find the full change log at the end of this document. Thanks to all contributors 
that made Lettuce 5.0.1.RELEASE possible.

Lettuce 5.0.1.RELEASE requires Java 8 and cannot be used with Java 6 or 7.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/


Enhancements
------------
* Add SocketAddressOutput to directly parse SENTINEL get-master-addr-by-name output #644

Fixes
-----
* Lettuce doesn't fail early & cleanly with a host in protected mode #608 (Thanks to @exercitussolus)
* CommandHandler.rebuildQueue() causes long locks #615 (Thanks to @nikolayspb)
* Request queue size is not cleared on reconnect #616 (Thanks to @nikolayspb)
* BITPOS should allow to just specify start #623 (Thanks to @christophstrobl)
* HMGET proxy not working as expected #627 (Thanks to @moores-expedia)
* Consider binary arguments using command interfaces as keys using binary codecs #628
* Command.isDone() not consistent with CompletableFuture.isDone() #629
* Race condition in RedisPublisher DEMAND.request() and DEMAND.onDataAvailable() #634 (Thanks to @mayamoon)
* RedisPublisher.request(-1) does not fail #635
* Capture subscription state before logging in RedisPublisher #636
* Provide Javadoc path for Project Reactor #641
* Debug logging of ConnectionWatchdog has wrong prefix after reconnect #645 (Thanks to @mlex)


Other
-----
* Upgrade to netty 4.1.17.Final #646
* Upgrade to Spring Framework 4.3.12 #648
* Upgrade to Commons Pool 2.4.3
* Upgrade to RxJava 1.3.3 #651
* Upgrade to RxJava2 2.1.6 #652
* Upgrade to HdrHistogram 2.1.10 #653

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
