Lettuce 6.6.0 RELEASE NOTES
==============================

The Redis team is delighted to announce the general availability of Lettuce 6.6.0

Great news, everyone! Lettuce 6.6.0 comes with RedisJSON support enabled.
For more on that, please consult with the [RedisJSON documentation](https://redis.io/docs/latest/develop/data-types/json/) and the [Lettuce guide on RedisJSON](https://redis.github.io/lettuce/user-guide/redis-json/). 

Lettuce 6 supports Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires
at least Java 8 and works with Java 21.

Thanks to all contributors who made Lettuce 6.6.0 possible.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://discord.gg/redis and look for the "Help:Tools Lettuce" channel 
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.6.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.6.0.RELEASE/api/

Commands
--------

* Add `CLUSTER MYSHARDID` in #2920 and `CLUSTER LINKS` in #2986
* Add `CLIENT TRACKINGINFO` in #2862

Enhancements
------------
* Token based authentication integration with core extension by @ggivo in https://github.com/redis/lettuce/pull/3063
* Deprecate the STRALGO command and implement the LCS in its place by @Dltmd202 in https://github.com/redis/lettuce/pull/3037

Fixes
-----
* fix: prevent blocking event loop thread by replacing ArrayDeque with HashIndexedQueue by @okg-cxf in https://github.com/redis/lettuce/pull/2953
* Fix: make sure FIFO order between write and notify channel active by @okg-cxf in https://github.com/redis/lettuce/pull/2597

Other
-----
* Add example configuration using SNI enabled TLS connection by @ggivo in https://github.com/redis/lettuce/pull/3045
* Disable docker image being used to call compose when running tests by @tishun in https://github.com/redis/lettuce/pull/3046
* Workflow for running benchmarks weekly by @tishun in https://github.com/redis/lettuce/pull/3052
* Fixing benchmark flow by @tishun in https://github.com/redis/lettuce/pull/3056
* Using the right name for the file this time by @tishun in https://github.com/redis/lettuce/pull/3057
* Test failures not reported because step is skipped by @tishun in https://github.com/redis/lettuce/pull/3067
* DOC-4528 async hash examples by @andy-stark-redis in https://github.com/redis/lettuce/pull/3069
* Bump org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16 by @dependabot in https://github.com/redis/lettuce/pull/2959
* Bump org.testcontainers:testcontainers from 1.20.1 to 1.20.4 by @dependabot in https://github.com/redis/lettuce/pull/3082
