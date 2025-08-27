Lettuce 6.8.0 RELEASE NOTES
==============================

The Redis team is delighted to announce the release of Lettuce 6.8.0

Lettuce 6 supports Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires
at least Java 8 and works with Java 21.

Thanks to all contributors who made Lettuce 6.8.0 possible.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://discord.gg/redis and look for the "Help:Tools Lettuce" channel 
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.8.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.8.0.RELEASE/api/

Commands
--------
* Introduce RediSearch by @tishun in https://github.com/redis/lettuce/pull/3375
* Add support for new operations of BITOP command in Redis Community Edition 8.2 by @atakavci in https://github.com/redis/lettuce/pull/3334
* Add support for 8.2 stream commands by @uglide in https://github.com/redis/lettuce/pull/3374

Enhancements
------------
* N/A

Fixes
-----
* NoClassDefFoundError in Lettuce 6.7.0 #3317 by @tishun in https://github.com/redis/lettuce/pull/3318
  
Other
-----
* The instance of the `ObjectMapper` can now be reused in the `DefaultJsonParser` by @thachlp in https://github.com/redis/lettuce/pull/3372
* Added basic connection interruption tests by @uglide in https://github.com/redis/lettuce/pull/3292
* DOC-4758 async JSON doc examples by @andy-stark-redis in https://github.com/redis/lettuce/pull/3335
* Fixed SocketOptions.Builder validation messages by @hubertchylik in https://github.com/redis/lettuce/pull/3366
* Refactor tests for clarity and maintainability by @Rian-Ismael in https://github.com/redis/lettuce/pull/3363
* Provide support for the SVS-VMANA index (#3385) by @tishun in https://github.com/redis/lettuce/pull/3386
