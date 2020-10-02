Lettuce 6.0.0 GA RELEASE NOTES
==============================

The Lettuce team is delighted to announce the availability of Lettuce 6.

Lettuce 6 aligns with Redis 6 in terms of API and protocol changes. Both protocols, RESP and RESP3 are supported side-by-side defaulting to RESP.

Most notable changes that ship with this release are:

* RESP3 support
* ACL Authentication with username/password
* Asynchronous Cluster Topology Refresh
* Client-side caching support
* Registration of push message listeners
* Configuration files for GraalVM Native Image compilation
* Kotlin Coroutine API
* Redesign command latency metrics publishing
* API cleanups/Breaking Changes

Lettuce 6 supports Redis 2.6+ up to Redis 6.x. In terms of Java runtime, Lettuce requires at least Java 8 and works with Java 15.

Thanks to all contributors who made Lettuce 6.0.0.RELEASE possible.
Lettuce requires a minimum of Java 8 to build and run and is compatible with Java 15. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements, and releases): https://groups.google.com/g/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.0.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.0.0.RELEASE/api/

RESP3 Support
-------------

Redis 6 ships with support for a new protocol version. RESP3 brings support for additional data types to distinguish better between responses. The following response types were introduced with RESP3:

* Null: a single `null` value replacing RESP v2 `*-1` and `$-1` null values.
* Double: a floating-point number.
* Boolean: `true` or `false`.
* Blob error: binary-safe error code and message.
* Verbatim string: a binary-safe string that is typically used as user message without any escaping or filtering.
* Map: an ordered collection of key-value pairs. Keys and values can be any other RESP3 type.
* Set: an unordered collection of N other types.
* Attribute: Like the Map type, but the client should keep reading the reply ignoring the attribute type, and return it to the client as additional information.
* Push: Out-of-band data.
* Streamed strings: A large response using chunked transfer.
* Hello: Like the Map type, but is sent only when the connection between the client and the server is established, in order to welcome the client with different information like the name of the server, its version, and so forth.
* Big number: a large number non-representable by the Number type

Lettuce supports all response types except attributes. Push messages are only supported for Pub/Sub messages.

The protocol version can be changed through `ClientOptions` which disables protocol discovery:

```java
ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build();
```

Future versions are going to discover the protocol version as part of the connection handshake and use the newest available protocol version.

ACL Authentication
------------------

Redis 6 supports authentication using username and password. Lettuce's `RedisURI` adapts to this change by allowing to specify a username:

`redis://username:password@host:port/database`

Using RESP3 or PING on connect authenticates the connection during the handshake phase. Already connected connections may switch the user context by issuing an `AUTH` command with username and password:

```java
StatefulRedisConnection<String, String> connection = client.connect();
RedisCommands<String, String> commands = connection.sync();
commands.auth("username", "password");
```

Asynchronous Cluster Topology Refresh
-------------------------------------

Cluster Topology Refresh was in Lettuce 4 and 5 a blocking and fully synchronous task that required a worker thread. A side-effect of the topology refresh was that command timeouts could be delayed as the worker thread pool was used for timeout tasks and the topology refresh. Lettuce 6 ships with a fully non-blocking topology refresh mechanism which is basically a reimplementation of the previous refresh mechanism but using non-blocking components instead.

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

Kotlin Coroutine API
--------------------

Kotlin users can now use a Coroutine API that exposes suspended methods and uses `Flow` where appropriate.
The API can be obtained through an extension function for Standalone, Cluster, and Sentinel connections exposing the appropriate API.  

```kotlin
val api: RedisCoroutinesCommands<String, String> = connection.coroutines()

val foo1 = api.set("foo", "bar")
val foo2 = api.keys("fo*")
```

Additionally, we ship two extensions that simplify transactional usage by providing a `multi` closure:

```kotlin
val result: TransactionResult = connection.async().multi {
    set("foo", "bar")
    get("foo")
}
```

The API is marked experimental and requires opt-in through `@ExperimentalLettuceCoroutinesApi` to avoid Compiler warnings. 
We expect further evolution of the API towards a more `Flow`-oriented API where now `List` is returned to enable streaming of large responses.

Redesign command latency metrics publishing
-------------------------------------------

This is a mostly internal change that switches from `CommandLatencyCollector` to the newly introduced `CommandLatencyRecorder` interface. 
Unless you're implementing or configuring `CommandLatencyCollector` yourself, you should not see any changes.

This change is motivated by support for libraries that provide latency observability without publishing metrics to the `EventBus`.


API cleanups/Breaking Changes
-----------------------------

With this release, we took the opportunity to introduce a series of changes that put the API into a cleaner shape. 

* Redesign command latency metrics publishing #1409
* Remove JavaRuntime class and move LettuceStrings to internal package #1329
* Remove Spring support classes #1358
* Replace io.lettuce.core.resource.Futures utility with Netty's PromiseCombiner #1283
* XGROUP DELCONSUMER should return pending message count #1377 (xgroupDelconsumer(…) now returns `Long`)
* Change hgetall return type from Mono<Map> to Flux<KeyValue> #1434
* Script Commands: `eval`, `digest`, `scriptLoad` methods now only accept `String` and `byte[]` argument types. Previously `digest` and `scriptLoad` accepted the script contents as Codec value type which caused issues especially when marshalling values using JSON or Java Serialization. The script charset can be configured via `ClientOptions` (`ClientOptions.builder().scriptCharset(StandardCharsets.US_ASCII).build();`), defaulting to UTF-8.
* Connection: Removal of deprecated timeout methods accepting `TimeUnit`. Use methods accepting `Duration` instead.
* Async Commands: `RedisAsyncCommands.select(…)` and `.auth(…)` methods return now futures instead if being blocking methods.
* Asynchronous API Usage: Connection and Queue failures now no longer throw an exception but properly associate the failure with the Future handle.
* Master/Replica API: Move implementation classes from `io.lettuce.core.masterslave` to `io.lettuce.core.masterreplica` package.
* Internal: Removal of the internal `LettuceCharsets` utility class.
* Internal: Reduced visibility of several `protected` fields in `AbstractRedisClient` (`eventLoopGroups`, `genericWorkerPool`, `timer`, `clientResources`, `clientOptions`, `defaultTimeout`).
* Internal: Consolidation of Future synchronization utilities (`LettuceFutures`, `RefreshFutures`, `Futures`).
* Deprecate reactive StreamChannel methods #1434
* Rename StatefulRedisConnection.suspendable() to .coroutines() and command interfaces accordingly #1436


Commands
-----------------------------
* Add support for STRALGO #1280
* Add support for LPOS #1320

Enhancements
------------
* Use channel thread to enqueue commands #617
* Redesign connection activation #697
* Add support for RESP3 #964
* Consolidate Future utils #1039
* Make RedisAsyncCommands.select() and auth() async #1118 (Thanks to @ikkyuland)
* Allow client to pick a specific TLS version and introduce PEM-based configuration #1167 (Thanks to @amohtashami12307)
* Optimization of BITFIELD args generation #1175 (Thanks to @ianpojman)
* Add mutate() to SocketOptions #1193
* Add CLIENT ID command #1197
* Lettuce not able to reconnect automatically to SSL+authenticated ElastiCache node #1201 (Thanks to @chadlwilson)
* Add support for AUTH with user + password introduced in Redis 6 #1202 (Thanks to @tgrall)
* HMSET deprecated in version 4.0.0 #1217 (Thanks to @hodur)
* Allow selection of Heap or Direct buffers for CommandHandler.buffer #1223 (Thanks to @dantheperson)
* Use domain specific value object as return type for xpending. #1229 (Thanks to @christophstrobl)
* Support JUSTID flag of XCLAIM command #1233 (Thanks to @christophstrobl)
* Add support for KEEPTTL with SET #1234
* Add support for RxJava 3 #1235
* Retrieve username from URI when RedisURI is built from URL #1242 (Thanks to @gkorland)
* Introduce ThreadFactoryProvider to DefaultEventLoopGroupProvider for easier customization #1243 (Thanks to @apilling6317)
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
* Kotlin Coroutine API #1387 (Thanks to @SokoMishaLov)
* Add support for aarch64 #1396 (Thanks to @odidev)

Fixes
-----
* Commands Timeout ignored/not working during refresh #1107 (Thanks to @pendula95)
* StackOverflowError in RedisPublisher #1140 (Thanks to @csunwold)
* Incorrect access on io.lettuce.core.ReadFrom.isOrderSensitive() #1145 (Thanks to @orclev)
* Consider ReadFrom.isOrderSensitive() in cluster scan command #1146
* Improve log message for nodes that cannot be reached during reconnect/topology refresh #1152 (Thanks to @drewcsillag)
* BoundedAsyncPool doesn't work with a negative maxTotal #1181 (Thanks to @sguillope)
* TLS setup fails to a master reported by sentinel #1209 (Thanks to @ae6rt)
* Lettuce metrics creates lots of long arrays, and gives out of memory error.  #1210 (Thanks to @omjego)
* CommandSegments.StringCommandType does not implement hashCode()/equals() #1211
* Unclear documentation about quiet time for RedisClient#shutdown  #1212 (Thanks to @LychakGalina)
* StreamReadOutput in Lettuce 6 creates body entries containing the stream id #1216
* Write race condition while migrating/importing a slot #1218 (Thanks to @phyok)
* randomkey return V not K #1240 (Thanks to @hosunrise)
* ConcurrentModificationException iterating over partitions #1252 (Thanks to @johnny-costanzo)
* Replayed activation commands may fail because of their execution sequence #1255 (Thanks to @robertvazan)
* Fix infinite command timeout #1260
* Connection leak using pingBeforeActivateConnection when PING fails #1262 (Thanks to @johnny-costanzo)
* Lettuce blocked when connecting to Redis #1269 (Thanks to @jbyjby1)
* Stream commands are not considered for ReadOnly routing #1271 (Thanks to @redviper)
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
* Sentinel authentication failed when using the pingBeforeActivateConnection parameter #1401 (Thanks to @viniciusxyz)
* RedisURI.toString() should not reveal password #1405
* MasterReplica.connect(…) doesn't consider username with Redis 6 #1406
* LPOS command sends FIRST instead of RANK #1410 (Thanks to @christophstrobl)
* Fix **/ for closing comments #1416
* Correctly report isDone if Command completed with completeExceptionally #1433

Other
-----
* Refactor script content argument types to String and byte[] instead of V (value type) #1010 (Thanks to @danielsomekh)
* Render Redis.toString() to a Redis URI #1040
* Pass Allocator as RedisStateMachine constructor argument #1053
* Simplify condition to invoke "resolveCodec" method in AnnotationRedisCodecResolver #1149 (Thanks to @machi1990)
* Encode database in RedisURI in path when possible #1155
* Remove LettuceCharsets #1156
* Move SocketAddress resolution from RedisURI to SocketAddressResolver #1157
* Remove deprecated timeout methods accepting TimeUnit #1158
* Upgrade to RxJava 2.2.13 #1162
* Add ByteBuf.touch(…) to aid buffer leak investigation #1164
* Add warning log if MasterReplica(…, Iterable<RedisURI>) contains multiple Sentinel URIs #1165
* Adapt GEOHASH tests to 10 chars #1196
* Migrate Master/Replica support to the appropriate package #1199
* Disable RedisURIBuilderUnitTests failing on Windows OS #1204 (Thanks to @kshchepanovskyi)
* Provide a default port(DEFAULT_REDIS_PORT) to RedisURI's Builder #1205 (Thanks to @hepin1989)
* Update code for pub/sub to listen on the stateful connection object. #1207 (Thanks to @judepereira)
* Un-deprecate ClientOptions.pingBeforeActivateConnection #1208
* Use consistently a shutdown timeout of 2 seconds in all AbstractRedisClient.shutdown methods #1214
* Upgrade dependencies (netty to 4.1.49.Final) #1161, #1224, #1225, #1239, #1259
* RedisURI class does not parse password when using redis-sentinel #1232 (Thanks to @kyrogue)
* Reduce log level to DEBUG for native library logging #1238 (Thanks to @DevJoey)
* Reduce visibility of fields in AbstractRedisClient #1241
* Upgrade to stunnel 5.56 #1246
* Add build profiles for multiple Java versions #1247
* Replace outdated Sonatype parent POM with plugin definitions #1258
* Upgrade to RxJava 3.0.2 #1261
* Enable Sentinel tests after Redis fixes RESP3 handshake #1266
* Consolidate exception translation and bubbling #1275
* Reduce min thread count to 2 #1278
* Upgrade dependencies #1305
* Add FAQ section to reference docs #1307
* Rename master branch to main #1308
* Consistently use Javadoc wording in BoundedPoolConfig.Builder #1337 (Thanks to @maestroua)
* Upgrade to Reactor Core 3.3.8.RELEASE #1353
* Upgrade to netty 4.1.51.Final #1354
* Consistently translate execution exceptions #1370
* Upgrade to RxJava 3.0.5 #1374
* Upgrade to Commons Pool 2.8.1 #1375
* Upgrade to Reactor 3.3.9.RELEASE #1384
* Upgrade to Project Reactor 3.3.10.RELEASE #1411
* Upgrade to netty 4.1.52.Final #1412
* Upgrade test/optional dependencies #1413
* Explicit AWS and AZURE compatibility on README #1425 (Thanks to @raphaelauv)
* Revisit synchronized blocks #1429
* Upgrade dependencies #1431
