Lettuce 6.2.5 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.2.5 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.2.5.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 19. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.2.5.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.2.5.RELEASE/api/

Enhancements
------------
* ReplicaTopologyProvider can't parse replicas from INFO #2375
* Support for module-based read-only commands #2401
* Accept Double and Boolean in `MapOutput` #2429
* Array lists with set capacities in SimpleBatcher #2445

Fixes
-----
* Reactive Cluster `MGET` is not running in parallel #2395
* `memory usage` command passes key as `String` instead of using the codec #2424
* Fix NPE when manually flushing a batch #2444

Other
-----
* Upgrade to Netty 4.1.94.Final #2431
* Update SetArgs.java builder method param comment #2441
