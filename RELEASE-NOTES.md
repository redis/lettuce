Lettuce 8.0.0.BETA2
==============================
The Lettuce team is pleased to announce the **second beta release of Lettuce 8.0**!

This release adds support for the new commands in **Redis 8.12** and for **Redis TimeSeries**:

- **Redis TimeSeries.** All 21 `TS.*` commands through `RedisTimeSeriesCommands` and its asynchronous, reactive, Kotlin coroutine and node-selection flavors. See [Redis TimeSeries](https://github.com/redis/lettuce/blob/main/docs/user-guide/timeseries.md).
- **Set member expiration (Redis 8.12).** Per-member time to live on sets: `SEXPIRE`, `SPEXPIRE`, `SEXPIREAT`, `SPEXPIREAT`, `STTL`, `SPTTL`, `SEXPIRETIME`, `SPEXPIRETIME`, `SPERSIST` and `SADDEX`.
- **Atomic sorted-set moves (Redis 8.12).** `ZMOVE`, `BZMOVE` and `ZMOVEMEMBER`.
- **`BLESS` (Redis 8.12).** `BLESS SET`, `BLESS CLEAR`, `BLESS GET` and `BLESS SCAN` protect keys from `maxmemory` eviction.
- **`BITRANGEINTARRAY` (Redis 8.12).** Returns the offsets of the set or clear bits in a range of a string or bitmap.
- **Count-min sketch (Redis 8.12).** The `CELL_SIZE` option, the `cell_size` field of `CMS.INFO` and negative `CMS.INCRBY` increments.
- **`CipherCodec` can store the initialization vector with each value.** Opt-in; the existing factory keeps its format.

**Breaking change since 8.0.0.BETA1:** stream commands called on an `AsyncNodeSelection` (for example `xadd`, `xread`, `xrange`) now return `AsyncExecutions<T>` instead of `Executions<T>`. Node selections now also expose the ACL and array commands.

See [What's new in Lettuce 8.0](https://github.com/redis/lettuce/blob/main/docs/new-features.md#whats-new-in-lettuce-80) for details and all breaking changes in 8.0, including the Project Reactor changes from 8.0.0.BETA1.

**Lettuce 8.0.0.BETA2** supports Redis **2.6+** up to Redis **8.x** and requires **Java 8** or newer. The driver is tested against Redis **8.12**, **8.10**, **8.8**, **8.6**, **8.4**, **8.2**, **7.4** and **7.2**.

We encourage you to try out this beta and to provide feedback ahead of the general availability release.

Thanks to all contributors who made Lettuce 8.0.0.BETA2 possible.

🚀 New Features
* Add Redis TimeSeries support by @a-TODO-rov in https://github.com/redis/lettuce/pull/3978
* [8.12] Support set member expiration (Redis 8.12) by @atakavci in https://github.com/redis/lettuce/pull/3991
* [Redis 8.12] Add ZMOVE, BZMOVE and ZMOVEMEMBER sorted-set commands by @a-TODO-rov in https://github.com/redis/lettuce/pull/4018
* [8.12] Support for BLESS command family (Redis 8.12) by @atakavci in https://github.com/redis/lettuce/pull/3936
* [8.12] Add support for the BITRANGEINTARRAY command by @a-TODO-rov in https://github.com/redis/lettuce/pull/3992
* Support CMS `CELL_SIZE`, `cell_size` info field and negative `CMS.INCRBY` (Redis 8.12) by @atakavci in https://github.com/redis/lettuce/pull/3918
* Store initialization vectors alongside CipherCodec values by @urinaner in https://github.com/redis/lettuce/pull/4021

⚠️ Breaking Changes
* [Commands API interface consistency #3] Fix node-selection aggregate interface wiring by @uglide in https://github.com/redis/lettuce/pull/3867

🐞 Bug Fixes
* Fix json command argument handling by @Dgramada in https://github.com/redis/lettuce/pull/3938
* Fail MultiDb connect when building the initial connection throws #4019 by @shinseongsu in https://github.com/redis/lettuce/pull/4020
* Preserve exclusive boundaries for infinite scores #4009 by @ultramancode in https://github.com/redis/lettuce/pull/4010
* Preserve exclusive range boundaries in XPendingArgs #4005 by @ultramancode in https://github.com/redis/lettuce/pull/4006
* Use SORT for streaming sort with arguments #4003 by @ultramancode in https://github.com/redis/lettuce/pull/4004
* Register missing read-only key commands in ReadOnlyCommands #3998 by @RaphaelFakhri in https://github.com/redis/lettuce/pull/3999
* Preserve clientOptions in DatabaseConfig.mutate() #3980 by @kotlakiran in https://github.com/redis/lettuce/pull/3981
* Skip failed read candidates in MasterReplicaConnectionProvider #3502 by @gibkim in https://github.com/redis/lettuce/pull/3975
* Preserve server key order in MapComplexData (#3965) by @princejain756 in https://github.com/redis/lettuce/pull/3966
* fix(output): forward ByteBuffer error to queued command output (#3960) by @ronitgupta138 in https://github.com/redis/lettuce/pull/3963
* fix(output): forward RESP3 callbacks in wrapper CommandOutputs (#3937) by @Lyin-OTTF in https://github.com/redis/lettuce/pull/3959
* Fix CipherCodec value encoding on the ByteBuf path by @liotur in https://github.com/redis/lettuce/pull/3958
* Fix DefaultEndpoint client resources argument validation by @DDINGJOO in https://github.com/redis/lettuce/pull/3952

⚙️ Maintenance
* Update 8.12 to official version and make default by @a-TODO-rov in https://github.com/redis/lettuce/pull/4029
* Bump 8.12 to RC3 by @a-TODO-rov in https://github.com/redis/lettuce/pull/4017
* Bump Redis OSS 8.12 to RC2 by @a-TODO-rov in https://github.com/redis/lettuce/pull/3988
* Test against Redis 8.12-rc1 and tolerate ZSCAN duplicates by @a-TODO-rov in https://github.com/redis/lettuce/pull/3986
* Fix three more flaky integration tests by @a-TODO-rov in https://github.com/redis/lettuce/pull/4028
* Fix and disable more flaky failover tests by @a-TODO-rov in https://github.com/redis/lettuce/pull/4022
* Fix flaky integration tests seen on main and PR CI by @a-TODO-rov in https://github.com/redis/lettuce/pull/4007
* Fix flaky tests that query before the index catches up by @atakavci in https://github.com/redis/lettuce/pull/4000
* Fix ConnectionInterruptionReactiveTest being silently skipped in scenario CI by @uglide in https://github.com/redis/lettuce/pull/4015
* Make draft-pr-description resolve the current branch at run time by @atakavci in https://github.com/redis/lettuce/pull/4001
* Document ProtocolKeyword.toString() as the latency metrics key #3941 by @shariorfarhan07 in https://github.com/redis/lettuce/pull/3961
* Remove references to lettuce.io by @a-TODO-rov in https://github.com/redis/lettuce/pull/3957
* Fix incorrect argument names in assertion messages by @DDINGJOO in https://github.com/redis/lettuce/pull/3955
* Fix typos in Javadoc and comments by @DDINGJOO in https://github.com/redis/lettuce/pull/3954

❤️ New Contributors - Welcome to the Lettuce family!
* @DDINGJOO made their first contribution in https://github.com/redis/lettuce/pull/3952
* @liotur made their first contribution in https://github.com/redis/lettuce/pull/3958
* @Lyin-OTTF made their first contribution in https://github.com/redis/lettuce/pull/3959
* @ronitgupta138 made their first contribution in https://github.com/redis/lettuce/pull/3963
* @princejain756 made their first contribution in https://github.com/redis/lettuce/pull/3966
* @gibkim made their first contribution in https://github.com/redis/lettuce/pull/3975
* @kotlakiran made their first contribution in https://github.com/redis/lettuce/pull/3981
* @shariorfarhan07 made their first contribution in https://github.com/redis/lettuce/pull/3961
* @RaphaelFakhri made their first contribution in https://github.com/redis/lettuce/pull/3999
* @ultramancode made their first contribution in https://github.com/redis/lettuce/pull/4004
* @shinseongsu made their first contribution in https://github.com/redis/lettuce/pull/4020
* @urinaner made their first contribution in https://github.com/redis/lettuce/pull/4021

**Full Changelog**: https://github.com/redis/lettuce/compare/8.0.0.BETA1...8.0.0.BETA2
