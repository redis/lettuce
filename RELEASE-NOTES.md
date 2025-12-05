Lettuce 6.8.2 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.8.2 service release!
This release ships with bugfixes and dependency upgrades.

Lettuce 6 supports Redis 2.6+ up to Redis 8.2. In terms of Java runtime, Lettuce requires
at least Java 8 and works with Java 21.

Thanks to all contributors who made Lettuce 6.8.2 possible.

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://discord.gg/redis and look for the "Help:Tools Lettuce" channel 
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.8.2.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.8.2.RELEASE/api/

Fixes
-----
* [Backport] Reduce CPU cycles spent on setting tracing tags (#3339) by @ggivo in https://github.com/redis/lettuce/pull/3505
* Preserve null values when parsing SearchReplies (#3518) by @a-TODO-rov in https://github.com/redis/lettuce/pull/3551
* SearchArgs.returnField with alias produces malformed redis command #3… by @a-TODO-rov in https://github.com/redis/lettuce/pull/3550
* Bumping Netty to 4.1.125.Final (6.8.x) by @tishun in https://github.com/redis/lettuce/pull/3535
* Fix command queue corruption on encoding failures (#3443) (6.8.x) by @tishun in https://github.com/redis/lettuce/pull/3560

**Full Changelog**: https://github.com/redis/lettuce/compare/6.8.1.RELEASE...6.8.2.RELEASE