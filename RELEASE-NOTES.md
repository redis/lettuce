Lettuce 6.5.0 RELEASE NOTES
==============================

The Redis team is delighted to announce the general availability of Lettuce 6.5.

Great news, everyone! Lettuce 6.5.0 comes with RedisJSON support enabled.
For more on that, please consult with the [RedisJSON documentation](https://redis.io/docs/latest/develop/data-types/json/) and the [Lettuce guide on RedisJSON](https://redis.github.io/lettuce/user-guide/redis-json/). 

Lettuce 6 supports Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires
at least Java 8 and works with Java 21.

Thanks to all contributors who made Lettuce 6.4.0 possible.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://discord.gg/redis and look for the "Help:Tools Lettuce" channel 
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.5.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.5.0.RELEASE/api/

Commands
--------

* Add `CLUSTER MYSHARDID` in #2920 and `CLUSTER LINKS` in #2986
* Add `CLIENT TRACKINGINFO` in #2862

Enhancements
------------

* Default ClientOptions.timeoutOptions to TimeoutOptions.enabled() (#2927)
* Update completeExceptionally on ClusterCommand using super (#2980)

Fixes
-----
* fix(2971): spublish typo fix (#2972)
* Initialize slots with empty BitSet in RedisClusterNode's constructors (#2341)
* Add defensive copy for Futures allOf() method (#2943)
* fix:deadlock when reentrant exclusive lock (#2961)


Other
-----

* Add badges to the README.md file (#2939)
* Convert wiki to markdown docs (#2944)
* Bump org.jacoco:jacoco-maven-plugin from 0.8.9 to 0.8.12 (#2921)
* Bump org.apache.maven.plugins:maven-surefire-plugin from 3.2.5 to 3.3.1 (#2922)
* Bump org.apache.maven.plugins:maven-failsafe-plugin from 3.2.5 to 3.3.1 (#2958)
* Bump org.apache.maven.plugins:maven-javadoc-plugin from 3.7.0 to 3.8.0 (#2957)
* Bump org.apache.maven.plugins:maven-surefire-plugin from 3.3.1 to 3.4.0 (#2968)
* Bump org.hdrhistogram:HdrHistogram from 2.1.12 to 2.2.2 (#2966)
* Bump org.apache.maven.plugins:maven-compiler-plugin from 3.12.1 to 3.13.0 (#2978)
* Bump org.apache.logging.log4j:log4j-bom from 2.17.2 to 2.24.0 (#2988)
* Bump io.netty:netty-bom from 4.1.107.Final to 4.1.113.Final (#2990)
* Suspected change in ubuntu causing CI failures (#2949)
