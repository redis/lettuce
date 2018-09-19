Lettuce 4.5.0 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 4.5.0 release! 
This release contains new features, bugfixes, and enhancements. 

The most notable change in this release are:

* Exception types for Redis Error Responses
* Support for Redis Streams

Find the full change log at the end of this document that lists all 78 tickets.

Thanks to all contributors who made Lettuce 4.5.0.Final possible.
Lettuce 4.5.0.Final requires Java 8 up to Java 11.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/

New Exceptions for Redis Responses
----------------------------------

This release introduces new Exception types for the following Redis responses:

* `LOADING`: `RedisLoadingException`
* `NOSCRIPT`: `RedisNoScriptException`
* `BUSY`: `RedisBusyException`

All exception types derive from `RedisCommandExecutionException` and do not 
require changes in application code.

Redis Streams
-----------------------

Redis 5.0 is going to ship with support for a Stream data structure. 
A stream is a log of events that can be consumed sequentially. A Stream 
message consists of an id and a body represented as hash (or `Map<K, V>`).

Lettuce provides access to Stream commands through `RedisStreamCommands` supporting
synchronous, asynchronous, and reactive execution models. All Stream commands
are prefixed with `X` (`XADD`, `XREAD`, `XRANGE`).

Stream messages are required to be polled. Polling can return either in a non-blocking
way without a message if no message is available, or, in a blocking way.
`XREAD` allows to specify a blocking duration in which the connection is blocked
until either the timeout is exceeded or a Stream message arrives.

The following example shows how to append and read messages from a Redis Stream:

```java
// Append a message to the stream
String messageId = redis.xadd("my-stream", Collections.singletonMap("key", "value"));

// Read a message
List<StreamMessage<String, String>> messages = redis.xread(StreamOffset.from("my-stream", messageId));


redis.xadd("my-stream", Collections.singletonMap("key", "value"));

// Blocking read
List<StreamMessage<String, String>> messages = redis.xread(XReadArgs.Builder.block(Duration.ofSeconds(2)), 
                                                           StreamOffset.latest("my-stream"));
```

Redis Streams support the notion of consumer groups. A consumer group is a group of 
one or more consumers that tracks the last consumed Stream message and allows 
explicit acknowledgment of consumed messages. 

```java
// Setup stream, see https://github.com/antirez/redis/issues/4824
redis.xadd("my-stream", Collections.singletonMap("key", "value"));

// Create consumer group
redis.xgroupCreate("my-stream", "my-group", "$");
redis.xadd("my-stream", Collections.singletonMap("key", "value"));

// Read stream messages in the context of a consumer
List<StreamMessage<String, String>> messages = redis.xreadgroup(Consumer.from("my-stream", "consumer1"),
                XReadArgs.Builder.noack(),
                StreamOffset.lastConsumed(key));

// process message

…

// Acknowledge message
redis.xack(key, "group", messages.get(0).getId());
```


Commands
--------
* Add AUTH option to MIGRATE command #733
* Add MASTER type to KillArgs #760
* Add support for ZPOPMIN, ZPOPMAX, BZPOPMIN, BZPOPMAX commands #778
* Add REPLACE option to RESTORE. #783 (Thanks to @christophstrobl)
* Add XGROUP DESTROY #789
* Add support for CLIENT UNBLOCK #812
* Support for approximate trimming in XAddArgs #846

Enhancements
------------
* Cancel commands after disconnect in at-most-once mode #547
* Execute scriptLoad(…) on all nodes via cluster connection #590
* Add support for Redis streams #606
* Reduce default shutdown timeout #613
* Introduce dedicated exceptions for NOSCRIPT and BUSY responses #620 (Thanks to @DaichiUeura)
* Add SocketAddressOutput to directly parse SENTINEL get-master-addr-by-name output #644
* Read from random slave preferred #676 (Thanks to @petetanton)
* Introduce exception to represent Redis LOADING response #682
* Do not fail if COMMAND command fails on startup #685 (Thanks to @pujian1984)
* CommandHandler.write() is O(N^2) #709 (Thanks to @gszpak)
* Cluster topology lookup should not replaces self-node details with host and port from RedisURI when RedisURI is load balancer #712 (Thanks to @warrenzhu25)
* Optimize Partitions/RedisClusterNode representation #715
* Unnecessary copying of byteBuf in CommandHandler.decode() #725 (Thanks to @gszpak)
* Add unknown node as trigger for adaptive refresh #732
* Remove MULTI from read-only commands #779
* Improve Javadoc of QUIT method #781

Fixes
-----
* PING responses are not decoded properly if Pub/Sub connection is subscribed #579
* Fix IllegalArgumentException in RedisClient.connectSentinel #588 (Thanks to @andrewsensus)
* UnsupportedOperationException (List#add) in NestedMultiOutput #589 (Thanks to @zapl)
* GEOPOS fails with a single member in the var args #591 (Thanks to @FerhatSavci)
* Reduce logging of native transport state to INFO #596
* Lettuce doesn't fail early & cleanly with a host in protected mode #608 (Thanks to @exercitussolus)
* Fix encapsulated default method lookup on interfaces #614
* CommandHandler.rebuildQueue() causes long locks #615 (Thanks to @nikolayspb)
* Request queue size is not cleared on reconnect #616 (Thanks to @nikolayspb)
* BITPOS should allow to just specify start. #623 (Thanks to @christophstrobl)
* Command.isDone() not consistent with CompletableFuture.isDone() #629
* Provide Javadoc path for Project Reactor #641
* Debug logging of ConnectionWatchdog has wrong prefix after reconnect. #645 (Thanks to @mlex)
* Weights param should be ignored if it is empty #657 (Thanks to @garfeildma)
* MasterSlave getNodeSpecificViews NPE with sync API #659 (Thanks to @boughtonp)
* RandomServerHandler can respond zero bytes #660
* ConcurrentModificationException when connecting a RedisClusterClient #663 (Thanks to @blahblahasdf)
* Recovered Sentinels in Master/Slave not reconnected #668
* Handling dead Sentinel slaves #669 (Thanks to @vleushin)
* Support SLAVE_PREFERRED at valueOf method #671 (Thanks to @be-hase)
* RedisCommandTimeoutException after two subsequent MULTI calls without executing the transaction #673 (Thanks to @destitutus)
* Fix ConnectionWatchDog won't reconnect problem in edge case #679 (Thanks to @kojilin)
* At least once mode keeps requeueing commands on non-recoverable errors #680 (Thanks to @mrvisser)
* Retain ssl/tls config from seed uris in Master/Slave context #684 (Thanks to @acmcelwee)
* NOAUTH after full queue and reconnect #691
* RedisURI.create("localhost") causes NPE #694
* RuntimeExceptions thrown by implementations of RedisCodec do not fail TransactionCommands #719 (Thanks to @blahblahasdf)
* RedisPubSubAdapter.message() being called with wrong channel #724 (Thanks to @adimarco)
* firstResponseLatency is always negative #740 (Thanks to @nickvollmar)
* EXEC does not fail on EXECABORT #743 (Thanks to @dmandalidis)
* DefaultEndpoint.QUEUE_SIZE becomes out of sync, preventing command queueing #764 (Thanks to @nivekastoreth)
* Do not retry completed commands through RetryListener #767
* Encode ScanArgs.matches using UTF-8 #792
* Fix Delay.exponential() Javadoc #799
* Reset coordinate state of GeoWithinListOutput after completing an item #805
* Remove superfluous call to command.get() in FutureSyncInvocationHandler #809
* Fall back to initial seed nodes on topology refresh when dynamicRefreshSources is enabled #822
* Retain response order for Set responses #823
* Change ZINCRBY member to value type #826
* Lua script execution containing non-ascii characters fails #844 (Thanks to @wenzuowei110)
* RedisState fails to resolve CommandType for known commands #851

Other
-----
* Cleanups #604
* Update LICENSE text and add NOTICE file #612
* Upgrade to Commons Pool 2.4.3 #650
* Upgrade to HdrHistogram 2.1.10 #653
* Upgrade Redis versions on TravisCI #655
* Readme 5.x maven details #681 (Thanks to @flurdy)
* Upgrade to Log4j 2.11.0 #749
* Upgrade to RxJava 1.3.8 #759
* Extend documentation for argument objects #761
* Upgrade to JavaParser 3.6.3 #769
* Upgrade to netty 4.1.29.Final #836
* Upgrade to Commons Pool 2.6 #854
* Upgrade to Mockito 2.22 #855
* Upgrade to commons-lang3 3.8 #857
* Upgrade to Spring Framework 4.3.19 #858
