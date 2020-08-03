Lettuce 6.0.0 RC1 RELEASE NOTES
==============================

The Lettuce team is delighted to announce the availability of the first Lettuce 6 release candidate.

Most notable changes that ship with this release are

* Client-side caching support
* Registration of push message listeners
* Configuration files for GraalVM Native Image compilation
* API cleanups/Breaking Changes

Server-assisted Client-side caching support
-------------------------------------------

Redis can notify clients about cache invalidations when you use Redis as a read-through cache.
That is when applications keep a local copy of the cached value and use Redis to back up the local cache before.
When a cache values gets changed (that were fetched from Redis), then Redis notifies interested clients so they can invalidate their near-cache and potentially fetch the changed value.

Redis 6 allows for tracking clients and sending push messages using RESP3 push messages.
Lettuce provides a `CacheFrontend` that can be used to interact with a cache.

Client-side caching assumes a near cache that can be queried, updated and evicted for individual keys.
Cache values are represented as strings and the entire functionality is exposed through a `CacheFrontend`.

See the following example that uses a `ConcurrentHashMap` as near cache and outlines the interaction between involved parties:

```java
Map<String, String> clientCache = new ConcurrentHashMap<>();

RedisCommands<String, String> otherParty = redisClient.connect().sync();

// creates a key
otherParty.set(key, value);

StatefulRedisConnection<String, String> myself = redisClient.connect();
CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), myself,
        TrackingArgs.Builder.enabled().noloop());

// Read-through into Redis
String cachedValue = frontend.get(key);
assertThat(cachedValue).isNotNull();

// client-side cache holds the same value
assertThat(clientCache).hasSize(1);

// now, the key expires
commands.pexpire(key, 1);

// a while later
Thread.sleep(200);

// the expiration reflects in the client-side cache
assertThat(clientCache).isEmpty();

assertThat(frontend.get(key)).isNull()
```

Lettuce ships out of the box with a `CacheAccessor` for the `Map` interface.
You can implement a `CacheAccessor` for your cache if it doesn't implement the `Map` interface.
Client-side caching support is only supported when using RESP3. The Pub/Sub mode isn't supported through `ClientSideCaching` and we don't support Pub/Sub redirection.
Since push messages are node-local, client-side caching is supported only on Redis Standalone setups. 
Master/Replica or Clustering operating modes are not supported as multi-node operations and connection failover impose severe complexity onto key tracking.

Read more: https://redis.io/topics/client-side-caching

Registration of push message listeners
--------------------------------------

Registration of push message listeners completes Lettuce's RESP3 support. 
You can register `PushMessage` listeners on Standalone and Redis Cluster connections by implementing `PushListener` respective `RedisClusterPushListener`:

```java 
connection.addListener(message -> {

    if (message.getType().equals("invalidate")) {
        invalidations.addAll((List) message.getContent(StringCodec.UTF8::decodeKey).get(1));
    }
});
```

Using push messages with Redis Cluster are subject to node-locality, therefore `RedisClusterPushListener` provides access to the `RedisClusterNode` from which the push message originated.

The content of push messages may consist of arbitrary data structures and vary across various push message types. 
`PushMessage` exposes the message type and access to its content as `List<Object>`. Bulk content can be decoded into more specific data types.    

We're working towards a GA release in late September.
We're considering resolving package cycles at this stage, but we're not sure whether we will ship these changes with Lettuce 6 or postpone these to Lettuce 7.  
Specifically, `RedisFuture`, argument types (all `CompositeArgument` subtypes), `Value` including subtypes, and API  types such as `ScanCursor` would be migrated to their own packages.
Your application would be impacted as you would be required to review and adjust your imports.   

Thanks to all contributors who made Lettuce 6.0.0.RC1 possible.
Lettuce requires a minimum of Java 8 to build and run and is compatible with Java 15. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements, and releases): https://groups.google.com/g/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.0.0.RC1/reference/
* Javadoc: https://lettuce.io/core/6.0.0.RC1/api/


API cleanups/Breaking Changes
-----------------------------

With this release, we took the opportunity to introduce a series of changes that put the API into a cleaner shape.

* Remove JavaRuntime class and move LettuceStrings to internal package #1329
* Remove Spring support classes #1358
* Replace io.lettuce.core.resource.Futures utility with Netty's PromiseCombiner #1283
* XGROUP DELCONSUMER should return pending message count #1377 (xgroupDelconsumer(…) now returns `Long`)

Commands
-----------------------------

* Add support for STRALGO #1280
* Add support for LPOS #1320

Enhancements
------------

* Use domain specific value object as return type for xpending. #1229 (Thanks to @christophstrobl)
* Add template method for EventLoopGroup creation #1273 (Thanks to @konstantin-grits)
* Add support for Client-side caching #1281
* Registration of push message listeners  #1284
* Add charset option to ScanArgs.match(…) #1285 (Thanks to @gejun123456)
* Allow for more customisation of the tracing span #1303 (Thanks to @JaidenAshmore)
* Support for GraalVM Native Images #1316 (Thanks to @ilopmar)
* Consider topology updates for default Cluster connections #1317 (Thanks to @be-hase)
* SSL handshake doesn't respect timeouts #1326 (Thanks to @feliperuiz)
* Reduce RedisStateMachine bytecode size #1332 (Thanks to @hellyguo)
* Feature request: add a cluster-capable version of `flushallAsync` #1359 (Thanks to @jchambers)
* BoundedAsyncPool object is ready to be manipulated with even though a connection is not created yet #1363 (Thanks to @little-fish)
* Introduce DecodeBufferPolicy to reduce memory usage #1314 (Thanks to @Shaphan)

Fixes
-----

* Write race condition while migrating/importing a slot #1218 (Thanks to @phyok)
* PauseDetector acquisition hang in DefaultCommandLatencyCollector #1300 (Thanks to @ackerL)
* NullPointerException thrown during AbstractRedisAsyncCommands.flushCommands #1301 (Thanks to @mruki)
* xpending(K, Consumer, Range, Limit) fails with ERR syntax error using Limit.unlimited() #1302 (Thanks to @nagaran1)
* Remove duplicated command on asking #1304 (Thanks to @koisyu)
* ArrayOutput stops response parsing on empty nested arrays #1327 (Thanks to @TheCycoONE)
* Synchronous dispatch of MULTI returns null #1335 (Thanks to @tzxyz)
* RedisAdvancedClusterAsyncCommandsImpl scriptKill is incorrectly calling scriptFlush #1340 (Thanks to @azhukayak)
* RedisAdvancedClusterAsyncCommands.scriptKill now calls scriptKill instead of scriptFlush #1341 (Thanks to @dengliming)
* Lingering topology refresh connections when using dynamic refresh sources #1342 (Thanks to @tpf1994)
* Wrong cast in StringCodec may lead to IndexOutOfBoundsException #1367 (Thanks to @dmandalidis)
* xpending(key, group) fails without pending messages #1378

Other
-----

* Upgrade dependencies #1305
* Add FAQ section to reference docs #1307
* Rename master branch to main #1308
* Consistently use Javadoc wording in BoundedPoolConfig.Builder #1337 (Thanks to @maestroua)
* Upgrade to Reactor Core 3.3.8.RELEASE #1353
* Upgrade to netty 4.1.51.Final #1354
* Consistently translate execution exceptions #1370
* Upgrade to RxJava 3.0.5 #1374
* Upgrade to Commons Pool 2.8.1 #1375
