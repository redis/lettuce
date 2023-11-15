Lettuce 6.2.7 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.2.7 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.2.7.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 21. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.2.7.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.2.7.RELEASE/api/

Enhancements
------------

* Add support for cluster-announced hostname #2487
* Add support for disconnect on timeout to recover early from no `RST` packet failures
  #2082

Fixes
-----

* StatefulRedisClusterPubSubConnectionImpl's activated() method will report exception
  after resubscribe() was call. #2534

Other
-----

* Refine command outputs to capture whether a segment has been received instead of relying
  on the deserialized value state #2498
* Docs on metrics (wiki) are misleading #2538
* Upgrade to netty 4.1.101.Final #2550
* Upgrade to Micrometer 1.9.17 #2551
