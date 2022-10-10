Lettuce 6.1.10 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.1.10 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.1.10.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 19. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.1.10.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.1.10.RELEASE/api/

Fixes
-----

* zrevrangestorebylex/zrevrangestorebyscore range arguments flipped #2203
* SMISMEMBER is not marked a readonly command #2197
* RedisURI.Builder#withSsl(RedisURI) not working with SslVerifyMode#CA #2182
* INFO response parsing throws on encountering '\n' on NodeTopologyView #2161
