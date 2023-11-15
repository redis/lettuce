Lettuce 6.3.0 RELEASE NOTES
==============================

The Lettuce team is delighted to announce general availability of Lettuce 6.3.

This release ships with support for Observability through Micrometer Tracing that has been
ported from Spring Data into Lettuce directly. Another infrastructure change is to report
the driver version and driver name to Redis through `CLIENT SETINFO` if the Redis version
is 7.2 or later using RESP3.

This release ships also with refinements around Cluster Topology refresh to suspend
periodic refresh. This is useful for JVM Checkpoint-Restore arrangements (Project CRaC)
to ensure that no background activity opens connections.

This release also upgrades to Kotlin 1.7 as baseline.

Lettuce 6 supports Redis 2.6+ up to Redis 7.x. In terms of Java runtime, Lettuce requires
at least Java 8 and works with Java 21.

Thanks to all contributors who made Lettuce 6.3.0 possible.

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature
  requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.3.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.3.0.RELEASE/api/

Commands
--------

* Add `WITHSCORE` option to `ZRANK` and `ZREVRANK` commands #2410
* Add support for `CLIENT SETINFO`, `CLIENT INFO`, and enhanced `CLIENT LIST` #2439
* ZMPOP and BZMPOP commands #2435
* Support 'FCALL' commands to Call Lua-scripts that are loaded as Function in redis #2185

Enhancements
------------

* Add support for disconnect on timeout to recover early from no `RST` packet failures
  #2082
* Avoid buffer copies in `RedisStateMachine` #2173
* Make SlotHash utility methods public #2199
* Improve `AdaptiveRefreshTriggeredEvent` to provide the cause and contextual details
  #2338
* Refine `RedisException` instantiation to avoid exception instances if they are not used
  #2353
* Add capability of FailOver with takeOver option #2358
* Add option to disable tracing for individual commands #2373
* ReplicaTopologyProvider can't parse replicas from INFO #2375
* Add support for Micrometer Tracing #2391
* Add Command filter to `MicrometerCommandLatencyRecorder` #2406
* Expose methods to suspend periodic topology refresh and to check whether a topology
  refresh is running #2428
* Accept Double and Boolean in `MapOutput` #2429
* Array lists with set capacities in SimpleBatcher #2445
* Add fallback to RESP2 upon `NOPROTO` response #2455
* Introduce generic Object output #2467
* Propagate initialization failures from `ChannelInitializer` #2475
* Register library name and library version on Redis 7.2 or greater #2483
* Add support for cluster-announced hostname #2487

Fixes
-----

* Proper creation of `AttributeKey` #2111
* INFO response parsing throws on encountering '\' on NodeTopologyView #2161
* `PartitionSelectorException` during refresh of `Partitions` #2178
* RedisURI.Builder#withSsl(RedisURI) not working with SslVerifyMode#CA #2182
* SMISMEMBER is not marked a readonly command #2197
* Eval lua script expects return integer but null #2200
* `ZRANGESTORE` does not support by Rank comparison #2202
* zrevrangestorebylex/zrevrangestorebyscore range arguments flipped #2203
* Own `RedisCredentialsProvider` causes issue with protocol handshake on Redis 5 #2234
* NullPointerException if INFO command on redis cluster fails #2243
* XTrimArgs Should Allow Limit = 0 #2250
* The hostname and password cannot parse even if escaping with RedisURI redis-sentinel the
  password include '@' and '#' #2254
* Fix password parsing error when redis-sentinel URI contains @ #2255
* Handle unknown endpoints in MOVED response #2290
* Fallback to RESP2 hides potential authentication configuration problems #2313
* Accept slots as String using `CLUSTER SHARDS` #2325
* `RedisURI.applySsl(…)` does not retain `SslVerifyMode` #2328
* Apply `SslVerifyMode` in `RedisURI.applySsl(…)` #2329
* Fix long overflow in `RedisSubscription#potentiallyReadMore` #2383
* Consistently implement CompositeArgument in arg types #2387
* Reactive Cluster `MGET` is not running in parallel #2395
* Polish RedisObservation name & javadoc #2404
* `memory usage` command passes key as `String` instead of using the codec #2424
* Fix NPE when manually flushing a batch #2444
* `flushCommands` leads to random inbound command order when using large argument values
  with SSL #2456
* `CommandListener` notified twice on error #2457
* `RoleParser` does not define `none`, `handshake`, and `unknown` replica states #2482
* StatefulRedisClusterPubSubConnectionImpl's activated() method will report exception
  after resubscribe() was call. #2534

Other
-----

* Improve Document on pingBeforeActivateConnection #2138
* Improve Document on dynamicRefreshSources #2139
* Fixes typo in ReadFrom #2213
* Fix duplicate word occurrences #2307
* Update netty.version to 4.1.89.Final #2311
* Avoid using port 7443 in Lettuce tests #2326
* Upgrade to Reactor 3.4.27 #2330
* Fix Set unit test sscanMultiple fail in redis7 #2349
* README.md demo has a error #2377
* Upgrade to Kotlin 1.7 #2392
* Upgrade to Netty 4.1.94.Final #2431
* Update SetArgs.java builder method param comment #2441
* Use enum for no-op `PauseDetectorWrapper` #2474
* Upgrade build to Redis 7.2 #2481
* Refine command outputs to capture whether a segment has been received instead of relying
  on the deserialized value state #2498
* Upgrade to Reactor 3.6.0 #2517
* Docs on metrics (wiki) are misleading #2538
* Upgrade to Micrometer 1.12.0 #2549
* Upgrade to netty 4.1.101.Final #2550

