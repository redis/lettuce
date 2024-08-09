# New & Noteworthy

## What’s new in Lettuce 6.3

- [Redis Function support](user-guide/redis-functions.md) (`fcall` and `FUNCTION`
  commands).

- Support for Library Name and Version through `LettuceVersion`.
  Automated registration of the Lettuce library version upon connection
  handshake.

- Support for Micrometer Tracing to trace observations (distributed
  tracing and metrics).

## What’s new in Lettuce 6.2

- [`RedisCredentialsProvider`](user-guide/connecting-redis.md#authentication) abstraction to
  externalize credentials and credentials rotation.

- Retrieval of Redis Cluster node connections using `ConnectionIntent`
  to obtain read-only connections.

- Master/Replica now uses `SENTINEL REPLICAS` to discover replicas
  instead of `SENTINEL SLAVES`.

## What’s new in Lettuce 6.1

- Kotlin Coroutines support for `SCAN`/`HSCAN`/`SSCAN`/`ZSCAN` through
  `ScanFlow`.

- Command Listener API through
  `RedisClient.addListener(CommandListener)`.

- [Micrometer support](advanced-usage.md#micrometer) through
  `MicrometerCommandLatencyRecorder`.

- [Experimental support for `io_uring`](advanced-usage.md#native-transports).

- Configuration of extended Keep-Alive options through
  `KeepAliveOptions` (only available for some transports/Java versions).

- Configuration of netty’s `AddressResolverGroup` through
  `ClientResources`. Uses `DnsAddressResolverGroup` when
  `netty-resolver-dns` is on the classpath.

- Add support for Redis ACL commands.

- [Java Flight Recorder Events](advanced-usage.md#java-flight-recorder-events-since-61)

## What’s new in Lettuce 6.0

- Support for RESP3 usage with Redis 6 along with RESP2/RESP3 handshake
  and protocol version discovery.

- ACL authentication using username and password or password-only
  authentication.

- Cluster topology refresh is now non-blocking.

- [Kotlin Coroutine Extensions](user-guide/kotlin-api.md).

- RxJava 3 support.

- Refined Scripting API accepting the Lua script either as `byte[]` or
  `String`.

- Connection and Queue failures now no longer throw an exception but
  properly associate the failure with the Future handle.

- Removal of deprecated API including timeout methods accepting
  `TimeUnit`. Use methods accepting `Duration` instead.

- Lots of internal refinements.

- `xpending` methods return now `List<PendingMessage>` and
  `PendingMessages`

- Spring support removed. Use Spring Data Redis for a seamless Spring
  integration with Lettuce.

- `AsyncConnectionPoolSupport.createBoundedObjectPool(…)` methods are
  now blocking to await pool initialization.

- `DecodeBufferPolicy` for fine-grained memory reclaim control.

- `RedisURI.toString()` renders masked password.

- `ClientResources.commandLatencyCollector(…)` refactored into
  `ClientResources.commandLatencyRecorder(…)` returning
  `CommandLatencyRecorder`.

## What’s new in Lettuce 5.3

- Improved SSL configuration supporting Cipher suite selection and
  PEM-encoded certificates.

- Fixed method signature for `randomkey()`.

- Un-deprecated `ClientOptions.pingBeforeActivateConnection` to allow
  connection verification during connection handshake.

## What’s new in Lettuce 5.2

- Allow randomization of read candidates using Redis Cluster.

- SSL support for Redis Sentinel.

## What’s new in Lettuce 5.1

- Add support for `ZPOPMIN`, `ZPOPMAX`, `BZPOPMIN`, `BZPOPMAX` commands.

- Add support for Redis Command Tracing through Brave, see [Configuring
  Client resources](advanced-usage.md#configuring-client-resources).

- Add support for [Redis
  Streams](https://redis.io/topics/streams-intro).

- Asynchronous `connect()` for Master/Replica connections.

- [Asynchronous Connection Pooling](advanced-usage.md#asynchronous-connection-pooling)
  through `AsyncConnectionPoolSupport` and `AsyncPool`.

- Dedicated exceptions for Redis `LOADING`, `BUSY`, and `NOSCRIPT`
  responses.

- Commands in at-most-once mode (auto-reconnect disabled) are now
  canceled already on disconnect.

- Global command timeouts (also for reactive and asynchronous API usage)
  configurable through [Client Options](advanced-usage.md#client-options).

- Host and port mappers for Lettuce usage behind connection
  tunnels/proxies through `SocketAddressResolver`, see [Configuring
  Client resources](advanced-usage.md#configuring-client-resources).

- `SCRIPT LOAD` dispatch to all cluster nodes when issued through
  `RedisAdvancedClusterCommands`.

- Reactive `ScanStream` to iterate over the keyspace using `SCAN`
  commands.

- Transactions using Master/Replica connections are bound to the master
  node.

## What’s new in Lettuce 5.0

- New artifact coordinates: `io.lettuce:lettuce-core` and packages moved
  from `com.lambdaworks.redis` to `io.lettuce.core`.

- [Reactive API](user-guide/reactive-api.md) now Reactive Streams-based using
  [Project Reactor](https://projectreactor.io/).

- [Redis Command
  Interfaces](redis-command-interfaces.md) supporting
  dynamic command invocation and Redis Modules.

- Enhanced, immutable Key-Value objects.

- Asynchronous Cluster connect.

- Native transport support for Kqueue on macOS systems.

- Removal of support for Guava.

- Removal of deprecated `RedisConnection` and `RedisAsyncConnection`
  interfaces.

- Java 9 compatibility.

- HTML and PDF reference documentation along with a new project website:
  <https://lettuce.io>.

