Lettuce 6.3.1 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.3.1 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.3.1.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires
at least Java 8 and works with Java 21.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.3.1.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.3.1.RELEASE/api/

Enhancements
------------

* Update `RedisVersion` parser to accept version numbers with non-numeric suffix #2557
* `ClusterTopologyRefreshOptions.Builder.enableAdaptiveRefreshTrigger (…)` without
  options should throw `IllegalArgumentException` #2575
* GraalVM -
  io.lettuce.core.metrics.DefaultCommandLatencyCollector$DefaultPauseDetectorWrapper was
  found in the image heap #2579

Fixes
-----

* Geosearch and FCALL_RO commands go to the master node #2568

Other
-----

* Extend copyright license years to 2024 #2577
* Upgrade to Reactor 3.6.2 #2586
