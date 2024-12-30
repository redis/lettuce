Lettuce 6.5.2 RELEASE NOTES
==============================

The Redis team is delighted to announce the general availability of Lettuce 6.5.2
This release ships with bugfixes and dependency upgrades.

Lettuce 6 supports Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires
at least Java 8 and works with Java 21. Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.5.2 possible.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/redis/lettuce/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://discord.gg/redis and look for the "Help:Tools Lettuce" channel 
* GitHub Issues (Bug reports, feature requests): https://github.com/redis/lettuce/issues
* Documentation: https://redis.github.io/lettuce/
* Javadoc: https://www.javadoc.io/doc/io.lettuce/lettuce-core/6.5.2.RELEASE/index.html

Commands
--------
* N/A

Enhancements
------------
* N/A

Fixes
-----
* Public API methods removed by mistake when introducing RedisJSON #3070 by @tishun in https://github.com/redis/lettuce/pull/3108
* Handle UTF-8 characters in command arguments #3071 by @tishun in https://github.com/redis/lettuce/pull/3110

Other
-----
* N/A
