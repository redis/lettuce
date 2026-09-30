Lettuce 7.8.0 RELEASE NOTES
==============================
The Lettuce team is pleased to announce the Lettuce **7.8.0** minor release!

Lettuce 7.8.0 supports Redis 2.6+ up to Redis 8.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 24. The driver is tested against Redis 8.10, Redis 8.8, Redis 8.6, Redis 8.4, Redis 8.2, Redis 8.0, Redis 7.4 and Redis 7.2.

Thanks to all contributors who made Lettuce 7.8.0.RELEASE possible.

📗 Links
Reference documentation: https://lettuce.io/core/7.8.0.RELEASE

⭐ New Features
* [Redis 8.10] Add FT.AGGREGATE REDUCE COLLECT support with complex-value FieldValue by @uglide in https://github.com/redis/lettuce/pull/3878
* Prepare public APIs for optional Reactor support by @a-TODO-rov in https://github.com/redis/lettuce/pull/3915
* Extend ZRANGE with additional arguments by @Dgramada in https://github.com/redis/lettuce/pull/3908
* Rework codecs for redis search module by @viktoriya-kutsarova in https://github.com/redis/lettuce/pull/3884

🐞 Bug Fixes
* Relax timeouts on MOVING rebind without buffered commands by @ggivo in https://github.com/redis/lettuce/pull/3894
* Signal onError instead of hanging when a reactive complex-output command errors by @HwangRock in https://github.com/redis/lettuce/pull/3851
* Fix ObjectOutput handling of top-level scalar responses by @cfcromn in https://github.com/redis/lettuce/pull/3861
* LCS command broken for non String codecs by @Dgramada in https://github.com/redis/lettuce/pull/3919
* fix: XAUTOCLAIM with JUSTID reports deleted PEL entries as claimed messages by @waterWang in https://github.com/redis/lettuce/pull/3901
* Support Sentinel implementations with a partial command surface by @uglide in https://github.com/redis/lettuce/pull/3920

⚙️ Maintenance
* Bump Netty to 4.2.17.Final by @a-TODO-rov in https://github.com/redis/lettuce/pull/3899
* Upgrade Reactor Core to 3.8.7 by @a-TODO-rov in https://github.com/redis/lettuce/pull/3905

💡 Other
* Promote geo failover API to GA by @a-TODO-rov in https://github.com/redis/lettuce/pull/3914
* Deprecate sorted set commands by @Dgramada in https://github.com/redis/lettuce/pull/3913
* Deprecate legacy client-side caching support by @a-TODO-rov in https://github.com/redis/lettuce/pull/3897
* Make partitions and readFrom volatile in PooledClusterConnectionProvider #3777 by @big-cir in https://github.com/redis/lettuce/pull/3783
* Report test metrics from a nightly workflow by @bobymicroby in https://github.com/redis/lettuce/pull/3903
* Add warm up docs by @a-TODO-rov in https://github.com/redis/lettuce/pull/3895
* Update the docs after 7.7 release by @a-TODO-rov in https://github.com/redis/lettuce/pull/3896
* Support running integration tests against externally provisioned Redis Enterprise databases by @uglide in https://github.com/redis/lettuce/pull/3898

❤️ New Contributors - Welcome to the Lettuce family!
* @cfcromn made their first contribution in https://github.com/redis/lettuce/pull/3861
* @waterWang made their first contribution in https://github.com/redis/lettuce/pull/3901
* @bobymicroby made their first contribution in https://github.com/redis/lettuce/pull/3903

**Full Changelog**: https://github.com/redis/lettuce/compare/7.7.0.RELEASE...7.8.0.RELEASE
