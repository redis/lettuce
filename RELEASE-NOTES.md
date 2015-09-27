# lettuce 3.3.1.Final RELEASE NOTES

lettuce 3.3.1.Final is a maintenance release that fixes several problems.

Fixes
-----
* GEOADD passes long/lat parameters in the wrong order to Redis #134 (thanks to @IdanFridman)
* Strip username from URI userinfo when creating a RedisURI with a username #131 (thanks to @jsiebens)
* GeoArgs not evaluated when calling georadiusbymember(...) #142 (thanks to @codeparity)

lettuce requires a minimum of Java 8 to build and Java 6 run. It is tested
continuously against Redis 3.0 and the unstable branch

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
                or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki
