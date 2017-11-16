lettuce 4.4.1 RELEASE NOTES
===========================

This release ships with 15 tickets fixed along with a few dependency upgrades. 
The most significant change improves retry behavior with more than 
1000 queued commands and an issue when using default methods on Java 9.

You can find the full change log at the end of this document. 
Thanks to all contributors that made Lettuce 5.0.1.RELEASE possible.

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
* Fix encapsulated default method lookup on interfaces #614
* CommandHandler.rebuildQueue() causes long locks #615 (Thanks to @nikolayspb)
* Request queue size is not cleared on reconnect #616 (Thanks to @nikolayspb)
* BITPOS should allow to just specify start #623 (Thanks to @christophstrobl)
* Command.isDone() not consistent with CompletableFuture.isDone() #629
* Provide Javadoc path for Project Reactor #641
* Debug logging of ConnectionWatchdog has wrong prefix after reconnect #645 (Thanks to @mlex)

Other
-----
* Upgrade to netty 4.0.53.Final/4.1.17.Final #646
* Upgrade to Spring Framework 4.3.12 #648
* Upgrade to Commons Pool 2.4.3 #650
* Upgrade to RxJava 1.3.3 #651
* Upgrade to HdrHistogram 2.1.10 #653
* Upgrade Redis versions on TravisCI #655

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
