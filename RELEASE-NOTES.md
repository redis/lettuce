Lettuce 7.7.0 RELEASE NOTES
==============================
The Lettuce team is pleased to announce the Lettuce **7.7.0** minor release!

✨ Highlights
---
Lettuce 7.7.0 introduces support for the new features from [Redis OSS 8.10 release](https://github.com/redis/redis/releases/tag/8.10.0), such as:

- New command: [HIMPORT](https://redis.io/docs/latest/commands/himport/) - high-throughput compact hash bulk insertion - [user-guide](https://github.com/redis/lettuce/blob/main/docs/user-guide/hash-import.md)
- New commands: [LMOVEM](https://redis.io/docs/latest/commands/lmovem/), [BLMOVEM](https://redis.io/docs/latest/commands/blmovem/) - move multiple elements between lists
- New command: [SUNIONCARD](https://redis.io/docs/latest/commands/sunioncard/) - get the cardinality of the union of multiple sets
- New command: [SDIFFCARD](https://redis.io/docs/latest/commands/sdiffcard/) - get the cardinality of the difference between sets
- [XREAD](https://redis.io/docs/latest/commands/xread/#optional-arguments), [XREADGROUP](https://redis.io/docs/latest/commands/xreadgroup/#optional-arguments) - new MAXCOUNT and MAXSIZE arguments to cap the cumulative reply entries and size
- New command: [FT.ALIASLIST](https://redis.io/docs/latest/commands/ft.aliaslist/) - get all aliases for the index
- Stemmer support for Malay and Tagalog languages

Feature support for Probabilistic data structures in Redis:

- [Bloom filter](https://redis.io/docs/latest/develop/data-types/probabilistic/bloom-filter/)
- [Count-min sketch](https://redis.io/docs/latest/develop/data-types/probabilistic/count-min-sketch/)
- [Cuckoo filter](https://redis.io/docs/latest/develop/data-types/probabilistic/cuckoo-filter/)
- [t-digest](https://redis.io/docs/latest/develop/data-types/probabilistic/t-digest/)
- [Top-K](https://redis.io/docs/latest/develop/data-types/probabilistic/top-k/)
---

Lettuce 7.7.0 supports Redis 2.6+ up to Redis 8.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 24. The driver is tested against Redis 8.10, Redis 8.8, Redis 8.6, Redis 8.4, Redis 8.2, Redis 8.0, Redis 7.4 and Redis 7.2.

Thanks to all contributors who made Lettuce 7.7.0.RELEASE possible.

📗 Links
Reference documentation: https://lettuce.io/core/7.7.0.RELEASE

⭐ New Features
* feature: Add Bloom Filter (BF.*) support to Lettuce #3759 by @Dgramada in https://github.com/redis/lettuce/pull/3760
* Add Cuckoo Filter (CF.*) support by @HwangRock in https://github.com/redis/lettuce/pull/3774
* Feature/top k by @Dgramada in https://github.com/redis/lettuce/pull/3780
* Feature/cms by @Dgramada in https://github.com/redis/lettuce/pull/3821
* Feature/t digest by @Dgramada in https://github.com/redis/lettuce/pull/3823
* Add support for CLIENT NO-TOUCH #3775 by @big-cir in https://github.com/redis/lettuce/pull/3776
* Add VISMEMBER support for Vector Sets by @njm1250 in https://github.com/redis/lettuce/pull/3822
* [Redis 8.10] add malay and tagalog lang support by @uglide in https://github.com/redis/lettuce/pull/3792
* [Redis 8.10]Add MAXCOUNT and MAXSIZE options to XREAD and XREADGROUP by @uglide in https://github.com/redis/lettuce/pull/3859
* [Redis 8.10] Add FT.ALIASLIST search command support by @uglide in https://github.com/redis/lettuce/pull/3844
* [Redis 8.10] Add LMOVEM and BLMOVEM list commands by @a-TODO-rov in https://github.com/redis/lettuce/pull/3858
* [Redis 8.10] Add SUNIONCARD and SDIFFCARD set cardinality commands by @uglide in https://github.com/redis/lettuce/pull/3874
* [Redis 8.10] Add HIMPORT (Hinted Hash Templates) with lazy per-connection prepare by @a-TODO-rov in https://github.com/redis/lettuce/pull/3879

🐞 Bug Fixes
* Avoid Mono.block() in RedisURI masking path by @PreAgile in https://github.com/redis/lettuce/pull/3762
* Publish ClusterTopologyChangedEvent after applying the new topology #2860 by @shun-lee in https://github.com/redis/lettuce/pull/3791
* Deprecate commands by @thachlp in https://github.com/redis/lettuce/pull/3682
* Fix VADD command for single-element vectors #3802 by @njm1250 in https://github.com/redis/lettuce/pull/3806
* Fix streams response parsing by @a-TODO-rov in https://github.com/redis/lettuce/pull/3801
* Fix JFR event recorder unit tests #3066 by @lsh1215 in https://github.com/redis/lettuce/pull/3809
* Reset topology-refresh in-progress flag on synchronous exception by @karunsehwag in https://github.com/redis/lettuce/pull/3817
* Fix quadratic key-order restoration in cluster reactive MGET by @karunsehwag in https://github.com/redis/lettuce/pull/3819
* Align native transport option selection with default order by @karunsehwag in https://github.com/redis/lettuce/pull/3818
* Count YIELD_SCORE_AS tokens in FT.HYBRID COMBINE clause #3811 by @apoorva-01 in https://github.com/redis/lettuce/pull/3832
* Fix FT.AGGREGATE ADDSCORES argument position by @uglide in https://github.com/redis/lettuce/pull/3833
* Fall back to plain SO_KEEPALIVE when jdk.net module is absent by @meteaksoyy in https://github.com/redis/lettuce/pull/3864

⚙️ Maintenance
* Upgrade GitHub Actions versions by @uglide in https://github.com/redis/lettuce/pull/3773
* Upgrade GitHub Actions versions (#3773) - part 2 by @tishun in https://github.com/redis/lettuce/pull/3784
* ci: run full Redis version matrix only nightly by @uglide in https://github.com/redis/lettuce/pull/3836
* Fix top flaky integration tests by @uglide in https://github.com/redis/lettuce/pull/3870
* Disable broken FT.HYBRID tests by @uglide in https://github.com/redis/lettuce/pull/3877
* [Commands API interface consistency #1] tests and non-breaking fixes by @uglide in https://github.com/redis/lettuce/pull/3865
* [Commands API interface consistency #2] Remove generators by @uglide in https://github.com/redis/lettuce/pull/3866
* [Commands API interface consistency] Replace adding-a-redis-command with extend-commands-api skill by @uglide in https://github.com/redis/lettuce/pull/3872
* Update security policy by @uglide in https://github.com/redis/lettuce/pull/3883

💡 Other
* Add option to skip unit tests in reusable workflow by @a-TODO-rov in https://github.com/redis/lettuce/pull/3768
* ci: add Spring Data Redis integration test workflow by @tishun in https://github.com/redis/lettuce/pull/3732
* Add deprecation annotation to coroutine multi DSL #2371 by @young0264 in https://github.com/redis/lettuce/pull/3771
* Add more tests around SCH by @uglide in https://github.com/redis/lettuce/pull/3793
* Add integration testing infrastructure documentation by @uglide in https://github.com/redis/lettuce/pull/3813
* Add Vector Set commands to read-only command list by @njm1250 in https://github.com/redis/lettuce/pull/3825
* Add AI-agent operating manual, reference docs, skills, rules, and guardrails by @a-TODO-rov in https://github.com/redis/lettuce/pull/3850
* [Redis 8.10] Add integration test for search-on-timeout config by @uglide in https://github.com/redis/lettuce/pull/3828
* Fix/cluster reject commands pending future by @Vaibhav026 in https://github.com/redis/lettuce/pull/3782=

❤️ New Contributors - Welcome to the Lettuce family!
* @PreAgile made their first contribution in https://github.com/redis/lettuce/pull/3762
* @shun-lee made their first contribution in https://github.com/redis/lettuce/pull/3791
* @njm1250 made their first contribution in https://github.com/redis/lettuce/pull/3806
* @lsh1215 made their first contribution in https://github.com/redis/lettuce/pull/3809
* @HwangRock made their first contribution in https://github.com/redis/lettuce/pull/3774
* @karunsehwag made their first contribution in https://github.com/redis/lettuce/pull/3817
* @apoorva-01 made their first contribution in https://github.com/redis/lettuce/pull/3832
* @meteaksoyy made their first contribution in https://github.com/redis/lettuce/pull/3864
* @Vaibhav026 made their first contribution in https://github.com/redis/lettuce/pull/3782

**Full Changelog**: https://github.com/redis/lettuce/compare/7.6.0.RELEASE...7.7.0.RELEASE
