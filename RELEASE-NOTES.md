Lettuce 5.3.4 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.3.4 service release! 
This release ships with 6 tickets fixed along with dependency upgrades. 
 
Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 5.3.4.RELEASE possible.
Lettuce requires a minimum of Java 8 to build and run and is compatible with Java 15. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements, and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.3.4.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.3.4.RELEASE/api/

Fixes
-----
* Wrong cast in StringCodec may lead to IndexOutOfBoundsException #1367 (Thanks to @dmandalidis)
* Sentinel authentication failed when using the pingBeforeActivateConnection parameter #1401 (Thanks to @viniciusxyz)
* LPOS command sends FIRST instead of RANK #1410 (Thanks to @christophstrobl)

Other
-----
* Upgrade to Project Reactor 3.3.10.RELEASE #1411
* Upgrade to netty 4.1.52.Final #1412
* Upgrade test/optional dependencies #1413
