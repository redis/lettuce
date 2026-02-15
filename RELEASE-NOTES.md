Lettuce 7.4.0 RELEASE NOTES
==============================
The Lettuce team is pleased to announce the Lettuce **7.4.0** minor release!

✨ New: MultiDbClient
Lettuce introduces MultiDbClient, providing client-side failover and failback across multiple Redis databases.

MultiDbClient is designed to support client-side geographic failover, improving availability by monitoring the health of configured Redis endpoints and automatically switching connections when a database becomes unavailable. 
See the [docs](https://github.com/redis/lettuce/blob/main/docs/failover.md) for details and more examples.

Lettuce 7.4.0 supports Redis 2.6+ up to Redis 8.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 24. The driver is tested against Redis 8.6, Redis 8.4, Redis 8.2, Redis 8.0, Redis 7.4 and Redis 7.2.

Thanks to all contributors who made Lettuce 7.4.0.RELEASE possible.

📗 Links
Reference documentation: https://lettuce.io/core/7.4.0.RELEASE

⭐ New Features
* [automatic failover] Support for client-side geographic failover by @ggivo in https://github.com/redis/lettuce/pull/3576
* Implement hotkeys commands by @a-TODO-rov in https://github.com/redis/lettuce/pull/3638

🐞 Bug Fixes
* Remove noisy INFO log for unsupported maintenance events by @ggivo in https://github.com/redis/lettuce/pull/3652

💡 Other
* Improvements to the Lettuce guide by @tishun in https://github.com/redis/lettuce/pull/3655

❤️ Contributors
We'd like to thank all the contributors who worked on this release!
@a-TODO-rov, @ggivo, @tishun, @atakavci  and github-action-benchmark

**Full Changelog**: https://github.com/redis/lettuce/compare/7.3.0.RELEASE...7.4.0.RELEASE
