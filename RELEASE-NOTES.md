Lettuce 6.7.0 RELEASE NOTES
==============================

The Redis team is delighted to announce the release of Lettuce 6.7.0

This release provides support for the newly introduced [Vector Sets](https://redis.io/docs/latest/develop/data-types/vector-sets/) data type which was released as part of Redis 8.0 and [helps Redis users with vector similarity](https://redis.io/blog/announcing-vector-sets-a-new-redis-data-type-for-vector-similarity/).

Starting with 6.7 the `ConnectionPoolSupport` also provides a way to provide custom connection validations. The release comes with a bunch of smaller improvements and bugfixes.

Lettuce 6.7.0 supports Redis 2.6+ up to Redis 8.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 24. The driver is tested against Redis 8.0, Redis 7.4 and Redis 7.2.

Find the full changelog at the end of this document.
Thanks to all contributors who made Lettuce 6.7.0.RELEASE possible.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://discord.gg/redis and look for the "Help:Tools Lettuce" channel 
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.7.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.7.0.RELEASE/api/

Commands
--------
* Support Redis 8 vector sets #3296 by @tishun in https://github.com/redis/lettuce/pull/3295

Enhancements
------------
* Add custom connection validation to ConnectionPoolSupport #3081 by @big-cir in https://github.com/redis/lettuce/pull/3138

Fixes
-----
* Fix deadlock when an invalid URI is presented to DefaultClusterTopologyRefresh by @henry701 in https://github.com/redis/lettuce/pull/3243
* Fix NPE in EntraIdIntegrationTests by @ggivo in https://github.com/redis/lettuce/pull/3254
* Deprecate DnsResolver in favor of AddressResolverGroup(#1572) by @young0264 in https://github.com/redis/lettuce/pull/3291

Other
-----
* Change native library default to epoll over io_uring by @thachlp in https://github.com/redis/lettuce/pull/3278
* Use InfoPatterns enum for ReplicaTopologyProvider pattern management by @ori0o0p in https://github.com/redis/lettuce/pull/3264
* Adjusting disconnectedBehavior Option to Prevent Timeout During Redis Shutdown #2866 by @MagicalLas in https://github.com/redis/lettuce/pull/2894
* Improve the performance of obtaining write connections through double-check locks. by @Chenrujie-85 in https://github.com/redis/lettuce/pull/3228
* Optimize string concatenation in NodeSelectionInvocationHandler.getNodeDescription() by @ori0o0p in https://github.com/redis/lettuce/pull/3262
* DOC-4756 sorted set examples with join() by @andy-stark-redis in https://github.com/redis/lettuce/pull/3184
* DOC-4757 list examples using join() by @andy-stark-redis in https://github.com/redis/lettuce/pull/3185
* docs: add default threads count about NioEventLoopGroup by @brido4125 in https://github.com/redis/lettuce/pull/3221
* Guide on resolving native library conflicts by @thachlp in https://github.com/redis/lettuce/pull/3309