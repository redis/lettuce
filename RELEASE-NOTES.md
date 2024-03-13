Lettuce 6.3.2 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.3.2 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.3.2.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires
at least Java 8 and works with Java 22.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.3.2.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.3.2.RELEASE/api/

Enhancements
------------

* Performance: Encoding of keys/values in CommandArgs when using a codec that implements
  ToByteBufEncoder #2610
* Switch to `ConcurrentLinkedQueue` to avoid expensive `size` calls #2602
* Use `HashedWheelTimer` for command expiration management to reduce thread context
  switches and improve performance #2773

Fixes
-----

* Connection reconnect suspended after reconnect to Redis with protected mode #2770
* Can't PUBLISH when subscribed with RESP3 #2594

Other
-----

* Upgrade dependencies #2780
