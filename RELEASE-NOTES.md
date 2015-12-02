# lettuce 4.1 RELEASE NOTES

Enhancements
------------
* Adjust logging when running into Exceptions (exceptionCaught()) #140
* Implement an EventBus system to publish events and metrics #124 (Thanks to @pulse00)
* ClientResources for 4.1 enhancement #137
* Provide a reusable client configuration for ThreadPools and other expensive resources #110
* Support FLUSHALL [ASYNC]/FLUSHDB [ASYNC]/UNLINK commands #146
* Support DEBUG RESTART/CRASH-AND-RECOVER [delay] commands #145
* Allow control over behavior in disconnected state #121

Fixes
-----
* Do not cache InetSocketAddress/SocketAddress in RedisURI #144
* Cluster API does not implement the Geo commands interface #154 (thanks to @IdanFridman)

Other
------


lettuce requires a minimum of Java 8 to build and run. It is tested continuously against the latest Redis source-build.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
                or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki