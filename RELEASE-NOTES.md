Lettuce 6.6.0 RELEASE NOTES
==============================

The Redis team is delighted to announce the Lettuce 6.6.0 minor release!

This release provides support for the newly introduced `HGETDEL`, `HGETEX` and `HSETEX` commands.
Users of the driver are also now able to [use a command replay filter](https://redis.github.io/lettuce/advanced-usage/#controlling-replay-of-commands-in-at-lease-once-mode),
that allows the user to control which (if any) commands are being retried when the reconnect feature is on. It is also possible to configure the driver to use the
`HashIndexedQueue` as a backing data structure for the command queue, which speeds up  the driver during disconnect / reconnect (needs to be configured using the ClientOptions).
Last but not least the `STRALGO` command was replaced by the `LCS` command.

The driver comes with Microsoft EntraID authentication support.

Lettuce 6 supports Redis 2.6+ up to Redis 8.x. In terms of Java runtime, Lettuce requires at least Java 8 and
works with Java 21. The driver is tested against Redis 8.0, Redis 7.4 and Redis 7.2.

Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 6.6.0.RELEASE possible.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://discord.gg/redis and look for the "Help:Tools Lettuce" channel 
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.6.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.6.0.RELEASE/api/

Commands
--------
* Hash Field Expiration (part II) (#3195) by @ggivo in https://github.com/redis/lettuce/pull/3204
* Deprecate the STRALGO command and implement the LCS in its place by @Dltmd202 in https://github.com/redis/lettuce/pull/3037

Enhancements
------------
* Introduce command replay filter to avoid command replaying after reconnect #1310 by @tishun in https://github.com/redis/lettuce/pull/3118
* fix: prevent blocking event loop thread by replacing ArrayDeque with HashIndexedQueue by @okg-cxf in https://github.com/redis/lettuce/pull/2953
* Token based authentication integration with core extension by @ggivo in https://github.com/redis/lettuce/pull/3063
* Support for DefaultAzureCredential by @ggivo in https://github.com/redis/lettuce/pull/3230
* Add support up to max unsigned integer in Bitfield offset (#2964) by @psw0946 in https://github.com/redis/lettuce/pull/3099
* Improve code by adding some null checks by @tishun in https://github.com/redis/lettuce/pull/3115
* Introduce test matrix based on Redis server versions by @ggivo in https://github.com/redis/lettuce/pull/3145
* Add modules ACL support by @sazzad16 in https://github.com/redis/lettuce/pull/3102
* Test modules CONFIG support by @sazzad16 in https://github.com/redis/lettuce/pull/3103
* report block error when use with reactor mode #3168 by @tishun in https://github.com/redis/lettuce/pull/3169
* Include command type in the timeout message by @arturaz in https://github.com/redis/lettuce/pull/3167
* replace hardcoded GT and LT with CommandKeyword enum by @minwoo1999 in https://github.com/redis/lettuce/pull/3079

Fixes
-----
* Restore API that was accidently deleted when introducing the JSON feature by @tishun in https://github.com/redis/lettuce/pull/3065
* Propagate handshake failures to Handshake future by @mp911de in https://github.com/redis/lettuce/pull/3058
* OpsForGeo producing "READONLY You can't write against a read only replica " on READS...  by @ggivo in https://github.com/redis/lettuce/pull/3032
* Json commands not exposed in AsyncCluster #3048 by @tishun in https://github.com/redis/lettuce/pull/3049
* WATCH during MULTI shouldn't fail transaction #3009 by @tishun in https://github.com/redis/lettuce/pull/3027
* Fix: make sure FIFO order between write and notify channel active by @okg-cxf in https://github.com/redis/lettuce/pull/2597
* UnsupportedOperationException from ListSubscriber during hrandfieldWithvalues #3122 by @tishun in https://github.com/redis/lettuce/pull/3123
* Update CommonsPool2ConfigConverterUnitTests.java by @Rian-Ismael in https://github.com/redis/lettuce/pull/3147
* Fix typo & add withSsl() in  connecting to Entra ID enabled Redis doc by @ggivo in https://github.com/redis/lettuce/pull/3191
* Fix SimpleBatcher apparent deadlock #2196 by @ggivo in https://github.com/redis/lettuce/pull/3148
* jsonArrpop fails with null return value (#3196) by @tishun in https://github.com/redis/lettuce/pull/3206
* json.arrpop forces index=-1 with root path (#3214) by @thachlp in https://github.com/redis/lettuce/pull/3217
* Updates enableAdaptiveRefreshTrigger trigger assertion message by @ymiliaresis in https://github.com/redis/lettuce/pull/3216

Other
-----

* Add example configuration using SNI enabled TLS connection by @ggivo in https://github.com/redis/lettuce/pull/3045
* Disable docker image being used to call compose when running tests by @tishun in https://github.com/redis/lettuce/pull/3046
* Workflow for running benchmarks weekly by @tishun in https://github.com/redis/lettuce/pull/3052
* Fixing benchmark flow by @tishun in https://github.com/redis/lettuce/pull/3056
* Test failures not reported because step is skipped by @tishun in https://github.com/redis/lettuce/pull/3067
* Stale issues action bump by @tishun in https://github.com/redis/lettuce/pull/3182
* Migrate Lettuce test setup to use client-lib-test by @kiryazovi-redis in https://github.com/redis/lettuce/pull/3158
* JSON integration tests now do not use the test-containers framework by @tishun in https://github.com/redis/lettuce/pull/3203
* Test with 8.0-M05-pre by @ggivo in https://github.com/redis/lettuce/pull/3219
* Add sample examples to test redis.io build by @uglide in https://github.com/redis/lettuce/pull/3051
* DOC-4528 async hash examples by @andy-stark-redis in https://github.com/redis/lettuce/pull/3069
* DOC-4531 set data type examples by @andy-stark-redis in https://github.com/redis/lettuce/pull/3076
* DOC-4802 fix string example concurrency by @andy-stark-redis in https://github.com/redis/lettuce/pull/3156
* Fix several typos on the advanced-usage page by @danicheg in https://github.com/redis/lettuce/pull/3174
* docs: update Limitations section to reflect shaded JAR deprecation by @minwoo1999 in https://github.com/redis/lettuce/pull/3095
* Remove extra spaces in words in docs by @enjoy-binbin in https://github.com/redis/lettuce/pull/3120
* Bump to v4 of checkout by @tishun in https://github.com/redis/lettuce/pull/3152
* Fix 'make test' test failures by @ggivo in https://github.com/redis/lettuce/pull/3157
* Readme doc on how to connect to Azure Managed Redis with Entra ID authentication by @ggivo in https://github.com/redis/lettuce/pull/3166
* refactor mget method improved readability and efficiency by @ori0o0p in https://github.com/redis/lettuce/pull/3061
* Migrate JSON tests infra to use client-lilb-test  by @ggivo in https://github.com/redis/lettuce/pull/3128
* Update the base project URLs in pom.xml by @danicheg in https://github.com/redis/lettuce/pull/3151
* Update publish docs action to use latest versions of actions by @tishun in https://github.com/redis/lettuce/pull/3154
* Bump default client-libs-test container version by @ggivo in https://github.com/redis/lettuce/pull/3165
* Bump org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16 by @dependabot in https://github.com/redis/lettuce/pull/2959
* Bump org.testcontainers:testcontainers from 1.20.1 to 1.20.4 by @dependabot in https://github.com/redis/lettuce/pull/3082
* Bump io.micrometer:micrometer-bom from 1.12.4 to 1.14.2 by @dependabot in https://github.com/redis/lettuce/pull/3096
* Bump io.netty.incubator:netty-incubator-transport-native-io_uring from 0.0.25.Final to 0.0.26.Final by @dependabot in https://github.com/redis/lettuce/pull/3106
* Bump netty.version to 4.1.118.Final #3187 by @tishun in https://github.com/redis/lettuce/pull/3189
