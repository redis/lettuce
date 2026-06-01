Lettuce 7.6.0 RELEASE NOTES
==============================
The Lettuce team is pleased to announce the Lettuce **7.6.0** minor release!

✨ Highlights
---
Lettuce 7.6.0 introduces support for the new features from [Redis OSS 8.8 release](https://github.com/redis/redis/releases/tag/8.8.0), such as:
- the new Array data structure
- INCREX: a window counter rate limiter combining INCR, INCRBY, INCRBYFLOAT, bounds, and expiration
- XNACK: a new streams command - allow consumers to explicitly release pending messages
- ZUNION, ZINTER, ZUNIONSTORE, ZINTERSTORE: new COUNT aggregator
- JSON.SET: new FPHA argument to specify the FP type for homogeneous FP arrays
---

Lettuce 7.6.0 supports Redis 2.6+ up to Redis 8.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 24. The driver is tested against Redis 8.8, Redis 8.6, Redis 8.4, Redis 8.2, Redis 8.0, Redis 7.4 and Redis 7.2.

Thanks to all contributors who made Lettuce 7.6.0.RELEASE possible.

📗 Links
Reference documentation: https://lettuce.io/core/7.6.0.RELEASE

⭐ New Features
* Add arrays API by @a-TODO-rov in https://github.com/redis/lettuce/pull/3745
* Introduce INCREX command by @a-TODO-rov in https://github.com/redis/lettuce/pull/3746
* Redis 8.8: Add XNACK support by @uglide in https://github.com/redis/lettuce/pull/3728
* Support COUNT aggregator for ZINTER, ZINTERSTORE, ZUNION and ZUNIONSTORE (Redis 8.8) by @atakavci in https://github.com/redis/lettuce/pull/3736
* Add support for FPHA argument with JSON.SET (Redis 8.8)#4478 by @atakavci in https://github.com/redis/lettuce/pull/3710
* Add missing Search languages by @viktoriya-kutsarova in https://github.com/redis/lettuce/pull/3690

🐞 Bug Fixes
* ERR unknown subcommand 'MYID' with Azure Managed Redis #3495 by @tishun in https://github.com/redis/lettuce/pull/3693
* Fix links in README for Streaming API and Native Transports by @a-TODO-rov in https://github.com/redis/lettuce/pull/3707
* Fix JSON.ARRAPPEND root path encoding by @Dgramada in https://github.com/redis/lettuce/pull/3715
* Fix benchmark tests by @atakavci in https://github.com/redis/lettuce/pull/3735
* Fix imports in benchmarks client by @atakavci in https://github.com/redis/lettuce/pull/3737
* Improve CI pipeline stability in https://github.com/redis/lettuce/pull/3740 , https://github.com/redis/lettuce/pull/3724 by @atakavci , https://github.com/redis/lettuce/pull/3725 by @tishun , https://github.com/redis/lettuce/pull/3720 by @viktoriya-kutsarova

⚙️ Maintenance
* ci(integration): cap `GITHUB_TOKEN` to `contents: read` by @arpitjain099 in https://github.com/redis/lettuce/pull/3761
* ci: declare workflow-level `contents: read` on 3 workflows by @arpitjain099 in https://github.com/redis/lettuce/pull/3748
* Bump Netty 4.2.12.Final by @atakavci in https://github.com/redis/lettuce/pull/3723
* Bump Netty 4.2.13.Final by @atakavci in https://github.com/redis/lettuce/pull/3751

💡 Other
* Add 8.8 to test matrix by @a-TODO-rov in https://github.com/redis/lettuce/pull/3764
* Add tests for client auth by @a-TODO-rov in https://github.com/redis/lettuce/pull/3685
* Introduce reusable workflows by @a-TODO-rov in https://github.com/redis/lettuce/pull/3673
* Optimize single-node master/replica reads by @Sean-Kenneth-Doherty in https://github.com/redis/lettuce/pull/3758
* Add subkeyspace notifications integration tests by @a-TODO-rov in https://github.com/redis/lettuce/pull/3734

❤️ New Contributors - Welcome to the Lettuce family!
* @Dgramada made their first contribution in https://github.com/redis/lettuce/pull/3715
* @Sean-Kenneth-Doherty made their first contribution in https://github.com/redis/lettuce/pull/3758
* @arpitjain099 made their first contribution in https://github.com/redis/lettuce/pull/3761

**Full Changelog**: https://github.com/redis/lettuce/compare/7.5.0.RELEASE...7.6.0.RELEASE
