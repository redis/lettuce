Lettuce 7.1.1.RELEASE NOTES
==============================

The Lettuce team is pleased to announce the release of Lettuce 7.1.1!

Lettuce 7.1.1.RELEASE supports Redis 2.6+ up to Redis 8.x and requires Java 8 or newer. The driver is tested against Redis 8.4, 8.2, 8.0, 7.4, and 7.2.

Thanks to all contributors who made Lettuce 7.1.1.RELEASE possible!

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://discord.gg/redis and look for the "Help:Tools Lettuce" channel 
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/7.1.1.RELEASE/reference/
* Javadoc: https://lettuce.io/core/7.1.1.RELEASE/api/

# Changes

## 🐛 Bug Fixes
* SearchArgs.returnField with alias produces malformed redis command #3528 (7.1.x) by @tishun in https://github.com/redis/lettuce/pull/3534
* Fix command queue corruption on encoding failures (#3443) (7.1.x) by @tishun in https://github.com/redis/lettuce/pull/3562

## 🧰 Maintenance
* Bumping Netty to 4.2.5.Final (7.1.x) by @tishun in https://github.com/redis/lettuce/pull/3537

---

**Full Changelog**: https://github.com/redis/lettuce/compare/7.1.0.RELEASE...7.1.1.RELEASE
