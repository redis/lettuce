Lettuce 6.7.0 RELEASE NOTES
==============================

The Redis team is delighted to announce the general availability of Lettuce 6.7.

Great news, everyone! Lettuce 6.7.0 comes with RedisJSON support enabled.
For more on that, please consult with the [RedisJSON documentation](https://redis.io/docs/latest/develop/data-types/json/) and the [Lettuce guide on RedisJSON](https://redis.github.io/lettuce/user-guide/redis-json/). 

Lettuce 6 supports Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires
at least Java 8 and works with Java 21.

Thanks to all contributors who made Lettuce 6.7.0 possible.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://discord.gg/redis and look for the "Help:Tools Lettuce" channel 
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.7.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.7.0.RELEASE/api/

Commands
--------
* Add `CLUSTER MYSHARDID` in #2920 and `CLUSTER LINKS` in #2986


Enhancements
------------
* Default ClientOptions.timeoutOptions to TimeoutOptions.enabled() (#2927)


Fixes
-----
* fix(2971): spublish typo fix (#2972)


Other
-----
* Add badges to the README.md file (#2939)

