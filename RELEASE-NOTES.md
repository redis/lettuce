Lettuce 6.0.0 M1 RELEASE NOTES
==============================

The Lettuce team is delighted to announce the availability of the first Lettuce 6 milestone.

Lettuce 6 aligns with Redis 6 in terms of API and protocol changes. Both protocols, RESP and RESP3 are supported side-by-side defaulting to RESP.

Most notable changes that ship with this release are

* RESP3 support
* ACL Authentication with username/password
* Asynchronous Cluster Topology Refresh
* API cleanups/Breaking Changes

We're working towards the next milestone and looking at further Redis 6 features such as client-side caching how these can be incorporated into Lettuce. The release date of Lettuce 6 depends on Redis 6 availability. 

Thanks to all contributors who made Lettuce 6.0.0.M1 possible.
Lettuce requires a minimum of Java 8 to build and run and is compatible with Java 14. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements, and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.0.0.M1/reference/
* Javadoc: https://lettuce.io/core/6.0.0.M1/api/

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

API cleanups/Breaking Changes
-----------------------------

With this release, we took the opportunity to introduce a series of changes that put the API into a cleaner shape. 

* Script Commands: `eval`, `digest`, `scriptLoad` methods now only accept `String` and `byte[]` argument types. Previously `digest` and `scriptLoad` accepted the script contents as Codec value type which caused issues especially when marshalling values using JSON or Java Serialization. The script charset can be configured via `ClientOptions` (`ClientOptions.builder().scriptCharset(StandardCharsets.US_ASCII).build();`), defaulting to UTF-8.
* Connection: Removal of deprecated timeout methods accepting `TimeUnit`. Use methods accepting `Duration` instead.
* Async Commands: `RedisAsyncCommands.select(…)` and `.auth(…)` methods return now futures instead if being blocking methods.
* Asynchronous API Usage: Connection and Queue failures now no longer throw an exception but properly associate the failure with the Future handle.
* Master/Replica API: Move implementation classes from `io.lettuce.core.masterslave` to `io.lettuce.core.masterreplica` package.
* Internal: Removal of the internal `LettuceCharsets` utility class.
* Internal: Reduced visibility of several `protected` fields in `AbstractRedisClient` (`eventLoopGroups`, `genericWorkerPool`, `timer`, `clientResources`, `clientOptions`, `defaultTimeout`).
* Internal: Consolidation of Future synchronization utilities (`LettuceFutures`, `RefreshFutures`, `Futures`).

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
* Support JUSTID flag of XCLAIM command #1233 (Thanks to @christophstrobl)
* Add support for KEEPTTL with SET #1234
* Add support for RxJava 3 #1235
* Retrieve username from URI when RedisURI is built from URL #1242 (Thanks to @gkorland)
* Introduce ThreadFactoryProvider to DefaultEventLoopGroupProvider for easier customization #1243 (Thanks to @apilling6317)

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
