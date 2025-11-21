Lettuce 7.1.0 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 7.1.0 minor release!

This release provides support for new features that are going to be part of the [Redis 8.4 release](https://redis.io/docs/latest/operate/oss_and_stack/stack-with-enterprise/release-notes/redisce/redisos-8.4-release-notes/), such as using the `CLAIM` parameter in the `XREADGROUP` command; atomically set multiple string keys and update their expiration with`MSETEX` and atomic compare-and-set and compare-and-delete for string keys using the extensions of the `DIGEST`, `DELEX` and `SET` commands.

Lettuce 7.1.0 supports Redis 2.6+ up to Redis 8.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 24. The driver is tested against Redis 8.4, Redis 8.2, Redis 8.0, Redis 7.4 and Redis 7.2.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 7.1.0.RELEASE possible.

📗 Links
Reference documentation: https://lettuce.io/core/7.1.0.RELEASE/reference/
Javadoc: https://lettuce.io/core/7.1.0.RELEASE/api/

⭐ New Features
* Add support for XREADGROUP CLAIM arg by @a-TODO-rov in https://github.com/redis/lettuce/pull/3486
* Add support CAS/CAD by @a-TODO-rov in https://github.com/redis/lettuce/pull/3512
* Implement msetex command by @a-TODO-rov in https://github.com/redis/lettuce/pull/3510

🐞 Bug Fixes
* Preserve null values when parsing SearchReplies by @mhyllander in https://github.com/redis/lettuce/pull/3518
* Add official 8.4 to test matrix and make it default by @a-TODO-rov in https://github.com/redis/lettuce/pull/3520
* Fix io_uring class name by @a-TODO-rov in https://github.com/redis/lettuce/pull/3509
* Reduce CPU cycles spent on setting tracing tags by @RohanNagar in https://github.com/redis/lettuce/pull/3339

💡 Other
* N/A

❤️ New Contributors
* @RohanNagar made their first contribution in https://github.com/redis/lettuce/pull/3339
* @mhyllander made their first contribution in https://github.com/redis/lettuce/pull/3518

**Full Changelog**: https://github.com/redis/lettuce/compare/7.0.0.RELEASE...7.1.0.RELEASE
