---
name: idiomatic-usage
description: Write application code that uses the Lettuce client correctly — thread-safe
  connection reuse, when to pool, and sync vs. async vs. reactive APIs. Use when writing
  or reviewing code that consumes Lettuce.
allowed-tools: Read
---

# Idiomatic Lettuce Usage

## Thread safety & connection reuse (the #1 mistake to avoid)
- A `StatefulRedisConnection` is **thread-safe** — create it ONCE and share it across threads.
- Do NOT open a connection per operation, and do NOT pool connections for non-blocking use.

      RedisClient client = RedisClient.create("redis://localhost");
      StatefulRedisConnection<String, String> conn = client.connect();
      RedisAsyncCommands<String, String> async = conn.async();
      // reuse `conn`/`async` everywhere; close on shutdown
      // conn.close(); client.shutdown();

## When to pool
- Pool ONLY for blocking/synchronous workloads that hold a connection (e.g. `MULTI`/`EXEC`
  transactions, blocking commands, or `WATCH`). Use `ConnectionPoolSupport`.
- For ordinary async/reactive traffic a single shared connection multiplexes fine.

## Choosing an API (don't mix them on the same logical flow)
- **Sync** — `conn.sync()` → `RedisCommands`: blocks the caller; simplest.
- **Async** — `conn.async()` → `RedisAsyncCommands`: returns `RedisFuture`; compose with `CompletionStage`.
- **Reactive** — `conn.reactive()` → `RedisReactiveCommands`: Project Reactor `Mono`/`Flux`; nothing runs until subscribed.

## Topologies
- Standalone: `RedisClient` + `redis://`.
- Sentinel / Master-Replica: `MasterReplica.connect(...)`.
- Cluster: `RedisClusterClient` + `redis://` seed nodes.

## Lifecycle
- Share `RedisClient` across the app; `shutdown()` it once at exit. Connections are cheap to multiplex, expensive to churn.
