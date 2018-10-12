Lettuce 5.1.1 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.1.1 service release! 
This release fixes 9 bugs in total.

Thanks to all contributors who made Lettuce 5.1.1.RELEASE possible.

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.1.1.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.1.1.RELEASE/api/

Fixes
-----
* Unable to reconnect Pub/Sub connection with authorization #868 (Thanks to @maestroua)
* Reduce allocations in topology comparator #870
* Fix recordCommandLatency to work properly #874 (Thanks to @jongyeol)
* Bug: Include hostPortString in the error message #876 (Thanks to @LarryBattle)
* Reference docs CSS prevents HTTPS usage #878
* ReactiveCommandSegmentCommandFactory resolves StreamingOutput for all reactive types #879 (Thanks to @yozhag)

Other
-----
* Migrate tests to JUnit 5 #430
* Remove tempusfugit dependency #871
* Makefile refactor download redis #877 (Thanks to @LarryBattle)
* Upgrade to Reactor Californium SR1 #883
