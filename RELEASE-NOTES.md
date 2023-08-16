Lettuce 6.2.6 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.2.6 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.2.6.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 21. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.2.6.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.2.6.RELEASE/api/

Enhancements
------------
* Add fallback to RESP2 upon `NOPROTO` response #2455
* Propagate initialization failures from `ChannelInitializer` #2475

Fixes
-----
* `CommandListener` notified twice on error #2457
* `flushCommands` leads to random inbound command order when using large argument values with SSL #2456
* `RoleParser` does not define `none`, `handshake`, and `unknown` replica states #2482

Other
-----
* Use enum for no-op `PauseDetectorWrapper` #2474
* Upgrade build to Redis 7.2 #2481
