lettuce 4.2.2 RELEASE NOTES
===========================

This is a bugfix release that fixes several issues. Updating is recommended for
setups with frequent reconnects or frequent cluster topology changes during runtime.

Thanks to all who made this release possible.

lettuce 4.2.2 requires Java 8 and cannot be used with Java 6 or 7.

Commands
--------
* Deprecate SYNC command #319

Fixes
-----
* Fix sync/async/reactive methods for thread-safe #302 (Thanks to @jongyeol)
* Allow coordinated cross-slot execution using Iterable #303 (Thanks to @agodet)
* Allow MasterSlave connection using Sentinel if some Sentinels are not available #304 (Thanks to @RahulBabbar)
* Don't call AUTH in topology refresh #313
* Record sent-time on queueing #314 (Thanks to @HaloFour)
* CommandHandler notifications called out of order #315
* Fix ask redirection #321 (Thanks to @kaibaemon)
* Check for isUnsubscribed() before calling subscriber methods #323 (Thanks to @vleushin)
* Replace synchronized setters with volatile fields #326 (Thanks to @guperrot)
* Store error at output-level when using NestedMultiOutput #328 (Thanks to @jongyeol)
* Fix master and slave address parsing for IPv6 addresses #329 (Thanks to @maksimlikharev)
* Guard command completion against exceptions #331 (Thanks to @vleushin)
* Use a read-view for consistent Partition usage during Partitions updates #333 (Thanks to @OutOfBrain)

Other
------
* Improve test synchronization in PubSubRxTest #216
* Change MethodTranslator's loadfactor to 1.0 for sync APIs performance #305 (Thanks to @jongyeol)
* Use at least 3 Threads when configuring default thread count #309
* Replace own partition host and port only if the reported connection point is empty #312
* Add workaround for IPv6 parsing #332
* Provide multi-key-routing for exists and unlink commands using Redis Cluster #334
* Add ConnectionWatchdog as last handler #335

lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki
