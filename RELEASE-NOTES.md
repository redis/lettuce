Lettuce 5.1.4 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.1.4 service release! 
This release ships with 14 tickets fixed. This release introduces reactive signal emission on
non-I/O threads so reactive single-connection systems can utilize more threads for
item processing and are not limited to a single thread.

Thanks to all contributors who made Lettuce 5.1.4.RELEASE possible.

Lettuce requires a minimum of Java 8 to build and run and #RunsLikeHeaven on Java 11. 
It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.1.4.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.1.4.RELEASE/api/

Enhancements
------------
* Allow usage of publishOn scheduler for reactive signal emission #905

Fixes
-----
* Chunked Pub/Sub message receive with interleaved command responses leaves commands uncompleted #936 (Thanks to @GavinTianYang)
* Fix typo in log message #970 (Thanks to @twz123)

Other
-----
* Javadoc is missing Javadoc links to Project Reactor types (Flux, Mono) #942
* Extend year range for 2019 in license headers #950
* Streamline communication sections in readme, issue templates and contribution guide #967
* Upgrade to stunnel 5.50 #968
* Replace old reactive API docs #974 (Thanks to @pine)
* Upgrade to Reactor 3.2.6.RELEASE #975
* Upgrade to netty 4.1.33.Final #976
* Upgrade to HdrHistogram 2.1.11 #978
* Upgrade to RxJava 2.2.6 #979
* Use JUnit BOM for dependency management and upgrade to JUnit 5.4.0 #980
* Use logj42 BOM for dependency management and upgrade to 2.11.2 #981
