Lettuce 5.0.2 RELEASE NOTES
===========================

This is the second bugfix release for Lettuce 5 shipping with 21 tickets resolved. It contains fixes for improved
resilience behavior.

Upgrading is recommended.  

Reference documentation: https://lettuce.io/core/release/reference/.
JavaDoc documentation: https://lettuce.io/core/release/api/.

```xml
<dependency>
  <groupId>io.lettuce</groupId>
  <artifactId>lettuce-core</artifactId>
  <version>5.0.2.RELEASE</version>
</dependency>
```

You can find the full change log at the end of this document. Thanks to all contributors 
that made Lettuce 5.0.2.RELEASE possible.

Lettuce 5.0.2.RELEASE requires Java 8 and cannot be used with Java 6 or 7.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/

Enhancements
------------
* Do not fail if COMMAND command fails on startup #685 (Thanks to @pujian1984)

Fixes
-----
* CommandHandler.rebuildQueue() causes long locks #615 (Thanks to @nikolayspb)
* Cannot close connection when refreshing topology #656 (Thanks to @dangtranhoang)
* Weights param should be ignored if it is empty #657 (Thanks to @garfeildma)
* MasterSlave getNodeSpecificViews NPE with sync API #659 (Thanks to @boughtonp)
* RandomServerHandler can respond zero bytes #660
* ConcurrentModificationException when connecting a RedisClusterClient #663 (Thanks to @blahblahasdf)
* Switch RedisSubscription.subscriber to volatile #664
* Recovered Sentinels in Master/Slave not reconnected #668
* Handling dead Sentinel slaves #669 (Thanks to @vleushin)
* Support SLAVE_PREFERRED at valueOf method #671 (Thanks to @be-hase)
* RedisCommandTimeoutException after two subsequent MULTI calls without executing the transaction #673 (Thanks to @destitutus)
* Fix ConnectionWatchDog won't reconnect problem in edge case #679 (Thanks to @kojilin)
* At least once mode keeps requeueing commands on non-recoverable errors #680 (Thanks to @mrvisser)
* Retain ssl/tls config from seed uris in Master/Slave context #684 (Thanks to @acmcelwee)
* NOAUTH after full queue and reconnect #691
* RedisURI.create("localhost") causes NPE #694

Other
-----
* Upgrade to Reactor Bismuth SR5 #698
* Upgrade to netty 4.1.21.Final #699
* Upgrade to RxJava 1.3.6 #700
* Upgrade to RxJava 2.1.9 #701
* Upgrade to Reactor Bismuth SR5 #704


Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
