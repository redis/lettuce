Lettuce 7.3.0 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 7.3.0 minor release!

Lettuce 7.3.0 supports Redis 2.6+ up to Redis 8.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 24. The driver is tested against Redis 8.6, Redis 8.4, Redis 8.2, Redis 8.0, Redis 7.4 and Redis 7.2.

Thanks to all contributors who made Lettuce 7.3.0.RELEASE possible.

📗 Links
Reference documentation: https://lettuce.io/core/7.3.0.RELEASE

⭐ New Features
* Add idempotent mechanism to streams by @a-TODO-rov in https://github.com/redis/lettuce/pull/3637
* Add support for INT vector types by @a-TODO-rov in https://github.com/redis/lettuce/pull/3616

🐞 Bug Fixes
* Fix command queue corruption on encoding failures by @yangy0000 in https://github.com/redis/lettuce/pull/3443
* Update NIO event loop creation to use Netty 4.2 API #3584 by @jruaux in https://github.com/redis/lettuce/pull/3585
* Fix epoll with iouring scenario by @a-TODO-rov in https://github.com/redis/lettuce/pull/3601

💡 Other
* Bump the maven group with 2 updates by @dependabot[bot] in https://github.com/redis/lettuce/pull/3390
* DOC-4423 list command examples by @andy-stark-redis in https://github.com/redis/lettuce/pull/3433
* DOC-5375 reactive hash examples by @andy-stark-redis in https://github.com/redis/lettuce/pull/3336
* DOC-5376 added reactive sets examples by @andy-stark-redis in https://github.com/redis/lettuce/pull/3337
* DOC-5399  set cmd examples by @andy-stark-redis in https://github.com/redis/lettuce/pull/3342
* Bump org.awaitility:awaitility from 4.2.2 to 4.3.0 by @dependabot[bot] in https://github.com/redis/lettuce/pull/3626
* Add Redis 8.6 to test matrix by @a-TODO-rov in https://github.com/redis/lettuce/pull/3617 https://github.com/redis/lettuce/pull/3635
* Re-enable SentinelAclIntegrationTests #3274 by @yuripbong in https://github.com/redis/lettuce/pull/3625

❤️ New Contributors
* @yangy0000 made their first contribution in https://github.com/redis/lettuce/pull/3443
* @yuripbong made their first contribution in https://github.com/redis/lettuce/pull/3625

**Full Changelog**: https://github.com/redis/lettuce/compare/7.2.1.RELEASE...7.3.0.RELEASE
