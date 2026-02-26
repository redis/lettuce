Lettuce 7.2.0 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 7.2.0 minor release!

Lettuce 7.2.0 supports Redis 2.6+ up to Redis 8.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 24. The driver is tested against Redis 8.4, Redis 8.2, Redis 8.0, Redis 7.4 and Redis 7.2.

Thanks to all contributors who made Lettuce 7.2.0.RELEASE possible.

📗 Links
Reference documentation: https://lettuce.io/core/7.2.0.RELEASE/reference/
Javadoc: https://lettuce.io/core/7.2.0.RELEASE/api/

⭐ New Features
* Add ftHybrid by @a-TODO-rov in https://github.com/redis/lettuce/pull/3540
* Expose method to add upstream driver libraries to CLIENT SETINFO payload by @viktoriya-kutsarova in https://github.com/redis/lettuce/pull/3542

🐞 Bug Fixes
* SearchArgs.returnField with alias produces malformed redis command #3528 by @tishun in https://github.com/redis/lettuce/pull/3530
* fix consistency with get(int) that returns wrapped DelegateJsonObject/DelegateJsonArray for nested structures by @NeatGuyCoding in https://github.com/redis/lettuce/pull/3464

💡 Other
* Bumping Netty to 4.2.5.Final (main) by @tishun in https://github.com/redis/lettuce/pull/3536

❤️ New Contributors
* @NeatGuyCoding made their first contribution in https://github.com/redis/lettuce/pull/3464
* @viktoriya-kutsarova made their first contribution in https://github.com/redis/lettuce/pull/3542

**Full Changelog**: https://github.com/redis/lettuce/compare/7.1.0.RELEASE...7.2.0.RELEASE
