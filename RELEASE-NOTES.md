Lettuce 7.5.0 RELEASE NOTES
==============================
The Lettuce team is pleased to announce the Lettuce **7.5.0** minor release!

Lettuce 7.5.0 supports Redis 2.6+ up to Redis 8.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 24. The driver is tested against Redis 8.6, Redis 8.4, Redis 8.2, Redis 8.0, Redis 7.4 and Redis 7.2.

Thanks to all contributors who made Lettuce 7.5.0.RELEASE possible.

📗 Links
Reference documentation: https://lettuce.io/core/7.5.0.RELEASE

⭐ New Features
* Hybrid refactor by @a-TODO-rov in https://github.com/redis/lettuce/pull/3667
* Enable keepalive by default by @a-TODO-rov in https://github.com/redis/lettuce/pull/3669
* [automatic failover] Add Dynamic Weight Management for Redis Multi-Database Connections by @atakavci in https://github.com/redis/lettuce/pull/3672

🐞 Bug Fixes
* fix memory leak issue on thread local in sharedLock - PR for main branch by @kandogu in https://github.com/redis/lettuce/pull/3640
* HOTKEYS fixes by @a-TODO-rov in https://github.com/redis/lettuce/pull/3665
* Fix borrowObject timeout wrapping by @JiHongKim98 in https://github.com/redis/lettuce/pull/3666
* Fix memory corruption/JVM crash in RediSearch by copying pooled ByteBuffers by @YoHanKi in https://github.com/redis/lettuce/pull/3664

💡 Other
* update imports in failover docs by @ggivo in https://github.com/redis/lettuce/pull/3658
* Bump org.assertj:assertj-core from 3.25.3 to 3.27.7 in the maven group across 1 directory by @dependabot[bot] in https://github.com/redis/lettuce/pull/3629
* AA scenario tests by @kiryazovi-redis in https://github.com/redis/lettuce/pull/3659
* Bump org.apache.maven.plugins:maven-gpg-plugin from 3.1.0 to 3.2.8 by @dependabot[bot] in https://github.com/redis/lettuce/pull/3668
* Improvements to the Lettuce guide (Advanced usage section) by @tishun in https://github.com/redis/lettuce/pull/3674
* [automatic failover] Docs for dynamic weight management by @atakavci in https://github.com/redis/lettuce/pull/3678

❤️ New Contributors
* @kandogu made their first contribution in https://github.com/redis/lettuce/pull/3640
* @JiHongKim98 made their first contribution in https://github.com/redis/lettuce/pull/3666
* @YoHanKi made their first contribution in https://github.com/redis/lettuce/pull/3664
  Welcome to the Lettuce family!

**Full Changelog**: https://github.com/redis/lettuce/compare/7.4.0.RELEASE...7.5.0.RELEASE
