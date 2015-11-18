# lettuce 4.0.1.Final RELEASE NOTES

This is a bugfix release for lettuce 4.0.Final to fix a bug in the Redis Cluster API
when using Geo commands.

Fixes
-----
* Cluster API does not implement the Geo commands interface #154 (thanks to @IdanFridman)

lettuce requires a minimum of Java 8 to build and run. It is tested continuously against the latest Redis source-build.

For complete information on lettuce see the websites:

* http://github.com/mp911de/lettuce
* http://redis.paluch.biz
