Lettuce 6.2.1 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.2.1 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.2.1.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 19. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.2.1.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.2.1.RELEASE/api/

Enhancements
------------

* `PartitionSelectorException` during refresh of `Partitions` #2178
* Make SlotHash utility methods public #2199

Fixes
-----

* INFO response parsing throws on encountering '\n' on NodeTopologyView #2161
* RedisURI.Builder#withSsl(RedisURI) not working with SslVerifyMode#CA #2182
* SMISMEMBER is not marked a readonly command #2197
* Eval lua script expects return integer but null #2200
* `ZRANGESTORE` does not support by Rank comparison #2202
* zrevrangestorebylex/zrevrangestorebyscore range arguments flipped #2203

Other
-----

* Fixes typo in ReadFrom #2213

