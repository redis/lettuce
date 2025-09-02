Lettuce 6.8.1 RELEASE NOTES
==============================

The Redis team is delighted to announce the release of Lettuce 6.8.1

Lettuce 6 supports Redis 2.6+ up to Redis 8.2 In terms of Java runtime, Lettuce requires
at least Java 8 and works with Java 21.

Thanks to all contributors who made Lettuce 6.8.1 possible.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://discord.gg/redis and look for the "Help:Tools Lettuce" channel 
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.8.1.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.8.1.RELEASE/api/

Fixes
-----
* Extend the JSON API to accept values of raw types  in https://github.com/redis/lettuce/issues/3369
* Possible NullPointerException in DelegateJsonObject in https://github.com/redis/lettuce/issues/3417
* Avoid creating a new instance of the ObjectMapper ctd. in https://github.com/redis/lettuce/issues/3412
* JSON implementation has reduced the API surface in https://github.com/redis/lettuce/issues/3368