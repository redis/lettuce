# Architecture: how a command flows through Lettuce

This page explains how a Redis command travels from a public API call down to the
wire and back. Read it before adding commands, touching the protocol layer, or
changing the connection stack. For the step-by-step recipe to add a command, see
the `adding-a-redis-command` skill in `.agents/skills/`.

Lettuce is a thread-safe Redis client for the JVM built on **netty** (transport)
and **Project Reactor** (reactive API). A single connection multiplexes many
concurrent commands over one channel.

## Contents

- The three API shapes
- One command catalog, hand-maintained flavors, test-enforced consistency
- Sync is a proxy; reactive is a separate implementation
- Command lifecycle (end to end)
- The command model
- The codec
- Wire protocol (RESP2 / RESP3)
- Netty pipeline
- Cluster routing
- Connections, threading, and the error model
- Common misconceptions

## The three API shapes

The public surface exists in three parallel flavors, all under
`src/main/java/io/lettuce/core/api/`:

| Package | Top interface | Return type |
|---------|---------------|-------------|
| `api/sync/` | `RedisCommands` | `T` (blocking) |
| `api/async/` | `RedisAsyncCommands` | `RedisFuture<T>` |
| `api/reactive/` | `RedisReactiveCommands` | `Mono<T>` / `Flux<T>` |

Each top interface is a **composition of per-command-group interfaces**
(`RedisStringCommands`, `RedisHashCommands`, `RedisKeyCommands`, …). The three
flavors are **not subtypes of one another** — they are three independently
generated shapes of the same command catalog, differing only in return type.

Cluster variants live under `src/main/java/io/lettuce/core/cluster/api/{sync,async,reactive}/`
(`RedisAdvancedClusterCommands` etc.) and add topology-aware, multi-node methods.

## One command catalog, hand-maintained flavors, test-enforced consistency

Every command group (STRING, HASH, …) exists in up to six parallel interfaces —
sync, async, reactive, Kotlin coroutines, and the two cluster node-selection
flavors. All of them are **ordinary hand-edited source files**; there is no code
generation. What keeps them in lockstep is the **API consistency test suite** in
`src/test/java/io/lettuce/core/api/consistency/` (plus a Kotlin test), which runs
with the regular unit tests and fails the build when a flavor is missing a method
or maps a return type incorrectly.

- **The sync interface is the reference** — the other flavors are derived from it
  by mechanical return-type mapping: async wraps in `RedisFuture<…>`; reactive
  maps `T → Mono<T>` and `List<T>/Set<T> → Flux<T>`; Kotlin uses
  `suspend fun`/`Flow`; node-selection wraps in `Executions`/`AsyncExecutions`.
- **Known deviations** live in two registries, split by flavor ownership: the
  Java flavors (forced `Flux`, `Value<…>`-wrapped elements, kept sync types, …)
  in `src/test/java/io/lettuce/core/api/consistency/KnownApiDeviations.java`;
  the Kotlin coroutine flavor (`Flow` methods, non-suspendable methods, …) in
  `src/test/kotlin/io/lettuce/core/api/consistency/KnownKotlinApiDeviations.kt`.
- **The group catalog** is the `CommandInterfaces` enum in the same package —
  register a new command group there and the suite covers it.

The rules, the editing workflow, and how to add a deviation are owned by
[api-consistency.md](api-consistency.md).

## Sync is a proxy; reactive is a separate implementation

There are effectively **two real implementations**, not three:

- **Async** — `AbstractRedisAsyncCommands` (concrete impl `RedisAsyncCommandsImpl`)
  in `src/main/java/io/lettuce/core/`. Each method delegates to the command builder
  and calls `dispatch(...)`.
- **Reactive** — `AbstractRedisReactiveCommands` in `src/main/java/io/lettuce/core/`.
  A **fully separate hand-written implementation** that builds the *same* `Command`
  objects via its own builder and dispatches them through a `RedisPublisher` on
  subscription (`createMono` / `createDissolvingFlux`).
- **Sync** — has **no implementation class**. `sync()` returns a **JDK dynamic
  proxy** (`java.lang.reflect.Proxy`) backed by
  `FutureSyncInvocationHandler` (`src/main/java/io/lettuce/core/`). The handler maps
  each sync call to the matching async method and blocks on the future
  (`Futures.awaitOrCancel`). There is no `RedisCommandsImpl`.

## Command lifecycle (end to end)

Taking `RedisCommands.get(key)`:

```
RedisCommands (dynamic proxy)
  → FutureSyncInvocationHandler         (maps to async method, blocks on future)
  → AbstractRedisAsyncCommands.get()    (delegates to builder, dispatch())
  → RedisCommandBuilder.get()           (builds Command + CommandArgs + Output)
  → AsyncCommand (wraps Command, is a CompletableFuture)
  → StatefulRedisConnectionImpl.dispatch()
  → RedisChannelHandler.dispatch()      (channelWriter.write(cmd))
  → [CommandListenerWriter → CommandExpiryWriter →] DefaultEndpoint.write()
  → CommandHandler.write()              (enqueue on `stack`, then ctx.write)
  → CommandEncoder.encode()             (command.encode(out) → RESP bytes)
─────────────────────────── WIRE ───────────────────────────
  → CommandHandler.decode()             (peek head of `stack`)
  → RedisStateMachine.decode()          (parse RESP reply markers)
  → CommandOutput (codec.decode…)       (bytes → K/V/T)
  → AsyncCommand.complete()             (resolve the future)
  → value returned to the blocked sync caller
```

`SET` is identical but uses `StatusOutput`; the 2-arg overload builds `CommandArgs`
via `SetArgs.build(args)`. `HGETALL` uses `MapOutput` (or `KeyValueStreamingOutput`
for the streaming overload); the multi-element reply drives repeated `set()` calls
in the state machine rather than one scalar.

Key files, in reading order:

1. `api/sync/RedisCommands.java`, `api/async/RedisAsyncCommands.java` — the surface.
2. `src/test/java/io/lettuce/core/api/consistency/` — the consistency suite that
   keeps the API flavors in lockstep.
3. `FutureSyncInvocationHandler.java`, `RedisChannelHandler.java` — sync proxy + dispatch.
4. `AbstractRedisAsyncCommands.java`, `AbstractRedisReactiveCommands.java` — the two impls.
5. `RedisCommandBuilder.java`, `protocol/BaseRedisCommandBuilder.java` — command construction.
6. `protocol/{Command,AsyncCommand,CommandArgs,CommandType,CommandKeyword}.java` — the command model.
7. `output/{CommandOutput,ValueOutput,StatusOutput,MapOutput}.java`, `codec/RedisCodec.java`.
8. `protocol/{DefaultEndpoint,CommandHandler,CommandEncoder,RedisStateMachine}.java` — the wire layer.
9. `ConnectionBuilder.java`, `RedisClient.java` — pipeline assembly and writer chain.

## The command model

- **`Command<K,V,T>`** (`protocol/Command.java`) holds a `ProtocolKeyword type`, a
  `CommandArgs<K,V>`, and a `CommandOutput<K,V,T>`; `encode(ByteBuf)` writes RESP.
- **`ProtocolKeyword`** is implemented by two enums: **`CommandType`** (command names
  like `GET`, `SET`, `HGETALL`) and **`CommandKeyword`** (argument modifiers like
  `MATCH`, `REPLACE`, `WITHVALUES`). The builder statically imports both.
- **`CommandArgs<K,V>`** (`protocol/CommandArgs.java`) is an **ordered** list of typed
  arguments. `addKey`/`addValue` capture the value *plus the codec*, so encoding to
  bytes happens lazily at `encode()` time. **Builder order is the wire order.**
- **`CommandOutput<K,V,T>`** (`output/CommandOutput.java`) is abstract and holds the
  codec; subclasses (`ValueOutput`, `StatusOutput`, `IntegerOutput`, `BooleanOutput`,
  `MapOutput`, `KeyValueStreamingOutput`, …) override `set` / `multiArray` / `complete`.
- **`AsyncCommand`** decorates a `Command` and *is* a `CompletableFuture<T>` — this is
  what `RedisFuture` resolves to.

## The codec

`RedisCodec<K,V>` (`codec/`) has four methods: `encodeKey`, `encodeValue`
(K/V → `ByteBuffer`) and `decodeKey`, `decodeValue` (`ByteBuffer` → K/V). Default is
`StringCodec` (UTF-8). It plugs in at **two** points:

1. **Encode** — passed into `CommandArgs` when keys/values are added.
2. **Decode** — passed into every `CommandOutput` at construction.

One codec instance flows from `RedisClient.connect(codec)` through the builder and
into every output — one codec per connection.

## Wire protocol (RESP2 / RESP3)

- **`CommandEncoder`** (`MessageToByteEncoder`) is **protocol-version agnostic** for
  requests — it just writes the unified request protocol (a RESP array of bulk strings).
- **`RedisStateMachine`** parses replies and **auto-discovers RESP2 vs RESP3** from
  reply-type marker bytes (a `MAP` or `HELLO_V3` marker flips it to RESP3). Only the
  *decoder* is version-aware.
- **`CommandHandler`** is the correlation engine. RESP has **no request IDs**: each
  written command is enqueued on an in-flight FIFO queue (`stack`), and each inbound
  reply is matched to the head of that queue **by send order**. The exception is RESP3
  out-of-band **push** messages, detected by marker and routed to `PushListener`s
  without consuming a stack entry.

## Netty pipeline

Handlers are built in `ConnectionBuilder.buildHandlers()` and added in
`PlainChannelInitializer.initChannel()`, in order:

```
ChannelGroupListener → CommandEncoder → RedisHandshakeHandler (HELLO/negotiation)
  → CommandHandler → ConnectionEventTrigger → [ConnectionWatchdog (auto-reconnect)]
```

The writer seen by the connection is a **decorator chain**
(`CommandListenerWriter → CommandExpiryWriter → DefaultEndpoint`), assembled in
`RedisClient.connectStandaloneAsync`.

## Cluster routing

In cluster mode the same builder/command/output model is reused. There are two
routing paths:

**1. Single-key routing (the default).** The connection's `channelWriter` is a
`ClusterDistributionChannelWriter` (`cluster/ClusterDistributionChannelWriter.java`).
Its `write()` extracts the first key, computes the slot via `SlotHash.getSlot(...)`,
and routes the command to the connection owning that slot *before* it reaches a
`DefaultEndpoint`/`CommandHandler`. Any single-key command flows through this
automatically — no cluster-specific code needed.

**2. Broadcast + aggregation (fan-out to all shards).** Commands that must run on
every master (e.g. `FLUSHALL`, `DBSIZE`, `KEYS`, and any command whose server tips
say `request_policy: all_shards`) are **hand-coded overrides** in
`RedisAdvancedClusterAsyncCommandsImpl` and its reactive sibling
`RedisAdvancedClusterReactiveCommandsImpl`. Lettuce does **not** auto-fan-out from
`COMMAND DOCS` tips — you override the method explicitly. The shape is always:

```java
// fan out to all upstream (master) nodes, then aggregate the per-node replies
return MultiNodeExecution.firstOfAsync(executeOnUpstream(RedisServerAsyncCommands::flushall));
```

- **`executeOnUpstream(fn)`** runs `fn` on every master node's connection and returns
  `Map<nodeId, CompletableFuture<T>>` (masters only — replicas never need it).
- **`MultiNodeExecution`** (`cluster/MultiNodeExecution.java`) holds the aggregators
  that fold those per-node replies into one: `aggregateAsync` (sums `Long`s, e.g.
  `dbsize`), `firstOfAsync` (returns the first / all-succeeded, e.g. `flushall`
  status). A command with a different response policy — logical-AND over booleans, a
  max, etc. — needs a **new aggregator helper added here**.
- Override in **both** the async and reactive advanced-cluster impls. The sync
  advanced-cluster and the `NodeSelection` APIs are dynamic proxies over async, so
  they inherit the fan-out for free.

Choosing the aggregator to match the command's response policy is the crux — pick
`firstOfAsync` for all-succeeded status, `aggregateAsync` for a sum, or add a helper
for anything else.

## Connections, threading, and the error model

### Connection types and entry points

A connection is a `Stateful*Connection` object; you get the command API from it via
`sync()`, `async()`, or `reactive()`. Each topology has its own connection type and
entry point:

| Topology | Connection interface | Entry point |
|----------|----------------------|-------------|
| Standalone | `StatefulRedisConnection` (`api/`) | `RedisClient.connect()` |
| Pub/Sub | `StatefulRedisPubSubConnection` (`pubsub/`) | `RedisClient.connectPubSub()` |
| Sentinel (management) | `StatefulRedisSentinelConnection` (`sentinel/api/`) | `RedisClient.connectSentinel()` |
| Master/Replica | `StatefulRedisMasterReplicaConnection` (`masterreplica/`) | `MasterReplica.connect(client, codec, uri)` |
| Cluster | `StatefulRedisClusterConnection` (`cluster/api/`) | `RedisClusterClient.connect()` |
| Cluster Pub/Sub | `StatefulRedisClusterPubSubConnection` (`cluster/pubsub/`) | `RedisClusterClient.connectPubSub()` |

All share the `StatefulConnection` base (`api/StatefulConnection.java`). The
`masterslave/` package is the **deprecated** predecessor of `masterreplica/`; a newer
multi-database failover API lives under `failover/` (in active development).

### Threading and multiplexing

- A `Stateful*Connection` is **thread-safe** and **multiplexes** many concurrent
  commands over a single channel (that is what `CommandHandler.stack` correlates).
  For ordinary non-blocking commands you need **one shared connection**, not one per
  thread.
- **A dedicated connection is required** for operations that hold or change
  connection state: **blocking** commands (`BLPOP`, `XREAD BLOCK`, …), **transactions**
  (`MULTI`/`EXEC`), and connection-state changes (`SELECT`, entering pub/sub). Use a
  pool for these.
- **Batching:** `setAutoFlushCommands(false)` (`StatefulConnection`) lets you queue
  many commands and then `flushCommands()` to write them in one batch for throughput.
  Re-enable/flush carefully — the flag is connection-wide, so it is awkward to share
  across threads.
- **Pooling:** connection pools live in `core/support/` — `ConnectionPoolSupport`
  (blocking) and `AsyncConnectionPoolSupport` + `BoundedAsyncPool` (async). Reach for
  a pool when you need blocking/transactional isolation, not to speed up normal
  commands.

### Error and timeout model

`RedisException` (`core/RedisException.java`) is the unchecked root. Key subtypes:

| Exception | Means |
|-----------|-------|
| `RedisConnectionException` | connection could not be established/kept |
| `RedisCommandExecutionException` | server returned an error reply (e.g. `WRONGTYPE`) |
| `RedisCommandTimeoutException` | command did not complete within the timeout |
| `RedisCommandInterruptedException` | thread interrupted while awaiting a result |

- **Timeouts** are enforced by `CommandExpiryWriter` in the writer chain, configured
  via `TimeoutOptions` (on `ClientOptions`) and/or the `RedisURI`/default timeout. A
  blocking `sync()` call waits up to the timeout, then throws
  `RedisCommandTimeoutException`.
- **Reconnection** is handled by `ConnectionWatchdog` (last handler in the pipeline),
  which transparently re-establishes dropped channels. Behavior on disconnect —
  whether commands are buffered and replayed, cancelled, or rejected — is governed by
  `ClientOptions` (`DisconnectedBehavior`, auto-reconnect, request-queue limits).
  While disconnected, `DefaultEndpoint` buffers writes up to those limits.

## Common misconceptions (read this)

1. **Sync commands do not have their own impl class.** `sync()` is a dynamic proxy
   over the async impl. The sync interface is only a declaration — its
   implementation is the async-backed proxy. Do not invent a `RedisCommandsImpl`.
2. **The interfaces are not generated.** All API flavors under `api/**` are
   hand-edited source files; when adding a command you edit every flavor and the
   consistency tests (`src/test/java/io/lettuce/core/api/consistency/`) verify you
   didn't miss one.
3. **Reactive is not derived from async.** Sync is a proxy over async, but reactive is
   a separate hand-written implementation (`AbstractRedisReactiveCommands`). There are
   two real impls to keep in sync (async + reactive), plus the Kotlin `*Impl.kt`.
4. **Replies are matched by FIFO order, not an ID.** RESP carries no correlation token;
   ordering in `CommandHandler.stack` is the correlation.
5. **The codec and RESP version live in more than one place.** The codec plugs in at
   both encode (`CommandArgs`) and decode (`CommandOutput`); RESP2/RESP3 is decided by
   the decoder auto-detecting reply markers, not a flag on the encoder. And "the writer"
   is a decorator chain (with a cluster router in front for cluster connections).
