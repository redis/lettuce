Lettuce 6.2.2 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.2.2 service release!
This release ships with bugfixes and dependency upgrades.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.2.2.RELEASE possible. Lettuce 6 supports
Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 19. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.2.2.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.2.2.RELEASE/api/

Fixes
-----
* The hostname and password cannot parse even if escaping with RedisURI redis-sentinel the password include '@' and '#' #2254
* Fix password parsing error when redis-sentinel URI contains @ #2255
* XTrimArgs Should Allow Limit = 0 #2250
* NullPointerException if INFO command on redis cluster fails #2243
* Own `RedisCredentialsProvider` causes issue with protocol handshake on Redis 5 #2234
* * Proper creation of `AttributeKey` #2111

Other
-----
* Improve Document on dynamicRefreshSources #2139
* Improve Document on pingBeforeActivateConnection #2138
