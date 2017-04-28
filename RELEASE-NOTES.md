lettuce 4.3.2 RELEASE NOTES
===========================

This release fixes several issues and some enhancements. 
it ships with [17 tickets](https://github.com/lettuce-io/lettuce-core/milestone/26?closed=1) fixed.

Lettuce was moved meanwhile to a new organization in GitHub: https://github.com/lettuce-io and
has a new website at https://lettuce.io. Maven coordinates for the 4.x branch remain stable.

If you're using command latency metrics, please note that first response metrics are now
calculated from the sent time and no longer from the command completion time.

Find the full change log at the end of this document.

Thanks to all contributors who made lettuce 4.3.2 possible.

Fixes
-----
* Read connect future in ReconnectHandler to local variable #465
* Close Standalone/Sentinel connections on connection failure #471 (Thanks to @jongyeol)
* Synchronize ClientResources shutdown in client shutdown #475
* Skip SO_KEEPALIVE and TCP_NODELAY options for Unix Domain Socket connections #476 (Thanks to @magdkudama)
* Fix cummulative metrics collection #487 (Thanks to @ameenhere)
* Stop LatencyStats on removal #517
* Guard PauseDetectorWrapper initialization against absence of LatencyUtils #520

Enhancements
------------
* Initialize RedisStateMachine.LongProcessor in static initializer #481 (Thanks to @hellyguo)
* Apply command timeout to PING before connection activation #470
* Close the connection when Redis protocol is corrupted #512 (Thanks to @jongyeol)
* Improve cleanup of PauseDetector #516
* CommandHandler performs expensive queue check if requestQueueSize != Integer.MAX_VALUE #507 (Thanks to @CodingFabian)

Other
------
* Improve JavaDoc #472
* Update Netty version to 4.1.9 #489 (Thanks to @odiszapc)
* Calculate first response metrics from sent time #491
* Upgrade to RxJava 1.2.10 #524
* Switch to codecov #504

lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
