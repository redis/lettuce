# lettuce 3.4 RELEASE NOTES

Enhancements
------------
* Adjust logging when running into Exceptions (exceptionCaught()) #140
* Implement an EventBus system to publish events and metrics #124 (Thanks to @pulse00)
* ClientResources for 4.1 enhancement #137
* Provide a reusable client configuration for ThreadPools and other expensive resources #110

Fixes
-----

Other
------

lettuce requires a minimum of Java 8 to build and Java 6 run. It is tested
continuously against Redis 3.0 and the unstable branch

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
                or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki
