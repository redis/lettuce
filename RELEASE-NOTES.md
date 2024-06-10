Lettuce 6.4.0 RELEASE NOTES
==============================

The Redis team is delighted to announce general availability of Lettuce 6.4.

This Lettuce driver is now going to be shipped under the MIT licensing scheme. The `CLIENT SETINFO`
is now working in a fire-and-forget mode to allow better compatibility with Redis servers that do
not support this command.

Lettuce 6 supports Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires
at least Java 8 and works with Java 21.

Thanks to all contributors who made Lettuce 6.4.0 possible.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://discord.gg/redis for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.4.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.4.0.RELEASE/api/

Commands
--------

* Add `PUBSUB` shard channel commands `SHARDCHANNELS` #2756, `SHARDNUMSUB` #2776
* Add `PUBSUB` shard channel commands `SPUBLISH` #2757, `SSUBSCRIBE` #2758 and `SUNSUBSCRIBE` #2758
* Add support for `CLIENT KILL [MAXAGE]` #2782
* Hash field expiration commands `HEXPIRE`, `HEXPIREAT`, `HEXPIRETIME` and `HPERSIST` #2836
* Hash field expiration commands `HPEXPIRE`, `HPEXPIREAT`, `HPEXPIRETIME`, `HTTL` and `HPTTL` #2857

Enhancements
------------

* Add support for `HSCAN NOVALUES` #2763
* Send the `CLIENT SETINFO` command in a fire-and-forget way #2082
* Change the license to more permissive MIT #2173
* Add a evalReadOnly overload that accepts the script as a String #2868
* `XREAD` support for reading last message from stream #2863
* Mark dnsResolver(DnsResolver) as deprecated  #2855
* Remove connection-related methods from commands API #2027
* Move connection-related commands from BaseRedisCommands to RedisConnectionCommands #2031

Fixes
-----

* None

Other
-----

* Bump `org.apache.commons:commons-pool2` from 2.11.1 to 2.12.0 #2877 
* Bump `org.openjdk.jmh:jmh-generator-annprocess` from 1.21 to 1.37 #2876
* Bump `org.apache.maven.plugins:maven-jar-plugin` from 3.3.0 to 3.4.1 #2875 
* Bump `org.codehaus.mojo:flatten-maven-plugin from` 1.5.0 to 1.6.0 #2874
* Bump `org.apache.maven.plugins:maven-javadoc-plugin` from 3.6.3 to 3.7.0 #2873
* Applying code formatter each time we run a Maven build #2841 
* Bump `setup-java` to v4 #2807 
