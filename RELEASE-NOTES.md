Lettuce 6.2.3 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.2.3 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.2.3.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 19. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.2.3.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.2.3.RELEASE/api/

Fixes
-----
* Fallback to RESP2 hides potential authentication configuration problems #2313
* Accept slots as String using `CLUSTER SHARDS` #2325
* Handle unknown endpoints in MOVED response #2290
* `RedisURI.applySsl(…)` does not retain `SslVerifyMode` #2328
* Apply `SslVerifyMode` in `RedisURI.applySsl(…)` #2329

Other
-----
* Avoid using port 7443 in Lettuce tests #2326
* Update netty.version to 4.1.89.Final #2311
* Fix duplicate word occurrences #2307

