lettuce 4.4.5 RELEASE NOTES
===========================

This is the fifth bugfix release for Lettuce 4.4 shipping with 8 tickets fixed along with a few dependency upgrades.
 
Upgrading is recommended for all users.  

You can find the full change log at the end of this document. 
Thanks to all contributors that made Lettuce 4.4.5.Final possible.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/


Commands
------------
* Add AUTH option to MIGRATE command #733
* Add MASTER type to KillArgs #760

Fixes
-----
* DefaultEndpoint.QUEUE_SIZE becomes out of sync, preventing command queueing #764 (Thanks to @nivekastoreth)
* Do not retry completed commands through RetryListener #767

Other
-----
* Upgrade to netty 4.1.23.Final #755
* Upgrade to RxJava 1.3.8 #759
* Extend documentation for argument objects #761
* Upgrade to netty 4.1.24.Final #770

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
