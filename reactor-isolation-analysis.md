# Reactor Isolation Analysis for TransactionBuilder

## Objective

Make Project Reactor an optional dependency by isolating all Reactor-specific code to dedicated packages/classes that can be excluded when not using reactive features.

## Current State Analysis

### Files with Reactor Imports Outside `/api/reactive/`

Total: **51 files** import Reactor classes outside the reactive package.

#### Core Package (`io.lettuce.core`)

| File | Reactor Usage | Isolation Difficulty |
|------|---------------|---------------------|
| `TransactionBuilder.java` | `Mono<TransactionResult> executeReactive()` | **Target of this effort** |
| `TransactionBuilderImpl.java` | Implements `executeReactive()` | **Target of this effort** |
| `RedisCredentialsProvider.java` | `Mono<RedisCredentials> resolveCredentials()` | High - core interface |
| `StaticCredentialsProvider.java` | Implements above | High |
| `ScanStream.java` | `Flux<T>` based scanning | Medium - could be in reactive pkg |
| `RedisPublisher.java` | Core Reactor publisher | High - internal impl |
| `Operators.java` | Reactor utilities | High - internal impl |
| `AbstractRedisClient.java` | Connection creation with Mono | High |
| `RedisClient.java` | Connection creation with Mono | High |
| `ConnectionBuilder.java` | Async connection with Mono | High |
| `RedisURI.java` | Mono in builder | Medium |
| `RedisAuthenticationHandler.java` | Flux for credential streaming | High |
| `StatefulRedisConnectionImpl.java` | Creates reactive commands | Medium |
| `AbstractRedisReactiveCommands.java` | Base reactive impl | Expected - reactive impl |
| `RedisReactiveCommandsImpl.java` | Reactive impl | Expected - reactive impl |

#### Cluster Package (`io.lettuce.core.cluster`)

| File | Reactor Usage | Isolation Difficulty |
|------|---------------|---------------------|
| `ClusterTransactionBuilder.java` | `Mono<TransactionResult> executeReactive()` | **Target of this effort** |
| `RedisClusterClient.java` | Connection creation with Mono | High |
| `StatefulRedisClusterConnectionImpl.java` | Creates reactive commands | Medium |
| `ClusterScanSupport.java` | Mono-based scanning | Medium |
| `AbstractClusterNodeConnectionFactory.java` | Mono connections | High |
| `ReactiveExecutionsImpl.java` | Expected - reactive impl | Expected |
| `RedisAdvancedClusterReactiveCommandsImpl.java` | Expected - reactive impl | Expected |
| `RedisClusterPubSubReactiveCommandsImpl.java` | Expected - reactive impl | Expected |

#### Other Packages

| Package | Files with Reactor | Notes |
|---------|-------------------|-------|
| `masterreplica` | 10 files | Connection management uses Mono heavily |
| `protocol` | 3 files | ConnectionWatchdog, ReconnectionHandler |
| `tracing` | 4 files | BraveTracing, MicrometerTracing |
| `event` | 2 files | EventBus uses Flux |
| `dynamic` | 2 files | ReactiveTypeAdapters |
| `pubsub` | 1 file | RedisPubSubReactiveCommandsImpl |
| `sentinel` | 1 file | RedisSentinelReactiveCommandsImpl |
| `authx` | 1 file | TokenBasedRedisCredentialsProvider |

### Public Interfaces with Reactor Types

| Interface | Method | Return Type |
|-----------|--------|-------------|
| `TransactionBuilder<K,V>` | `executeReactive()` | `Mono<TransactionResult>` |
| `RedisCredentialsProvider` | `resolveCredentials()` | `Mono<RedisCredentials>` |
| `RedisCredentialsProvider` | `credentials()` | `Flux<RedisCredentials>` |
| `EventBus` | `get()` | `Flux<Event>` |
| `TraceContextProvider` | `getTraceContextLater()` | `Mono<TraceContext>` |

## TransactionBuilder Isolation Strategy

### Option 1: Interface Inheritance (Recommended)

```
io.lettuce.core/
├── TransactionBuilder<K,V>           # Base interface - NO Reactor
│   ├── commands() → RedisAsyncCommands
│   ├── execute() → TransactionResult
│   └── executeAsync() → RedisFuture<TransactionResult>

io.lettuce.core.api.reactive/
└── ReactiveTransactionBuilder<K,V>   # Extends base, adds reactive
    └── executeReactive() → Mono<TransactionResult>
```

**Connection API:**
```java
// StatefulRedisConnection (no Reactor import needed)
TransactionBuilder<K, V> transaction();

// Access reactive builder via reactive() commands
// OR add to StatefulRedisConnection with return type from reactive package
ReactiveTransactionBuilder<K, V> reactiveTransaction();
```

### Option 2: Separate Factory Methods

```java
// On StatefulRedisConnection - returns base TransactionBuilder
TransactionBuilder<K, V> transaction();

// On RedisReactiveCommands - returns reactive-aware builder
ReactiveTransactionBuilder<K, V> transaction();
```

### Option 3: Functional API with Separate Reactive Extension

```java
// On StatefulRedisConnection (no Reactor)
TransactionResult transact(Consumer<RedisAsyncCommands<K,V>> body);
RedisFuture<TransactionResult> transactAsync(Consumer<RedisAsyncCommands<K,V>> body);

// On RedisReactiveCommands (has Reactor)
Mono<TransactionResult> transactional(Consumer<RedisAsyncCommands<K,V>> body);
```

## Required Changes for TransactionBuilder Isolation

### Phase 1: Interface Changes

| File | Change Required |
|------|-----------------|
| `TransactionBuilder.java` | Remove `executeReactive()`, remove Reactor import |
| `ReactiveTransactionBuilder.java` | **NEW** - extends TransactionBuilder, adds `executeReactive()` |

### Phase 2: Implementation Changes

| File | Change Required |
|------|-----------------|
| `TransactionBuilderImpl.java` | Implement `ReactiveTransactionBuilder` instead of `TransactionBuilder` |
| `ClusterTransactionBuilder.java` | Same - implement `ReactiveTransactionBuilder` |

### Phase 3: Connection API Changes

| File | Change Required |
|------|-----------------|
| `StatefulRedisConnection.java` | Keep `transaction()` returning base type |
| `StatefulRedisConnectionImpl.java` | Internal impl returns `ReactiveTransactionBuilder` (widened to base at interface) |
| `RedisReactiveCommands.java` | Add `ReactiveTransactionBuilder<K,V> transaction()` |
| `AbstractRedisReactiveCommands.java` | Implement reactive transaction factory |

### Phase 4: Functional API (Optional Enhancement)

| File | Change Required |
|------|-----------------|
| `TransactionalOperations.java` | **NEW** - interface with `transact()`, `transactAsync()` |
| `ReactiveTransactionalOperations.java` | **NEW** - extends above, adds `transactional()` |
| `StatefulRedisConnection.java` | Extend `TransactionalOperations` |
| `RedisReactiveCommands.java` | Extend `ReactiveTransactionalOperations` |

## Known Conflicts and Challenges

### 1. `io.lettuce.core.Consumer` Name Collision

Lettuce has its own `Consumer<K>` class for Redis Streams, which conflicts with `java.util.function.Consumer<T>`.

**Solution:** Use fully qualified names in method signatures:
```java
void transact(java.util.function.Consumer<RedisAsyncCommands<K,V>> body);
```

### 2. Multiple Connection Implementations

Classes that implement `StatefulRedisConnection` need to implement new methods:

| Class | Package |
|-------|---------|
| `StatefulRedisConnectionImpl` | `io.lettuce.core` |
| `StatefulRedisClusterConnectionImpl` | `io.lettuce.core.cluster` |
| `StatefulRedisMultiDbConnectionImpl` | `io.lettuce.core.failover` |
| `StatefulRedisMultiDbPubSubConnectionImpl` | `io.lettuce.core.failover` |
| `MasterSlaveConnectionWrapper` | `io.lettuce.core.masterslave` |

### 3. Codec Access in AbstractRedisReactiveCommands

`AbstractRedisReactiveCommands` doesn't expose `codec` field, needed for creating `TransactionBuilderImpl`.

**Solution:** Add protected getter or pass codec via connection.

## Migration Path for Users

### Before (Current API)
```java
TransactionBuilder<String, String> builder = connection.transaction();
builder.commands().set("key", "value");
Mono<TransactionResult> result = builder.executeReactive();
```

### After (Isolated API)
```java
// Option A: Via reactive commands
ReactiveTransactionBuilder<String, String> builder = connection.reactive().transaction();
builder.commands().set("key", "value");
Mono<TransactionResult> result = builder.executeReactive();

// Option B: Via functional API on reactive commands
Mono<TransactionResult> result = connection.reactive().transactional(txn -> {
    txn.set("key", "value");
});

// Option C: Cast (if implementation still supports it)
ReactiveTransactionBuilder<String, String> builder =
    (ReactiveTransactionBuilder<String, String>) connection.transaction();
```

## Broader Reactor Isolation Effort

This analysis focuses on `TransactionBuilder`, but the broader effort needs to address:

1. **RedisCredentialsProvider** - Core interface uses `Mono`/`Flux`
2. **EventBus** - Uses `Flux<Event>`
3. **Connection creation** - `RedisClient.connect*()` uses `Mono` internally
4. **Master-Replica** - Heavy Reactor usage in topology discovery

### Recommended Priority

| Priority | Component | Effort | User Impact |
|----------|-----------|--------|-------------|
| 1 | TransactionBuilder | Low | Direct API |
| 2 | RedisCredentialsProvider | Medium | Auth customization |
| 3 | EventBus | Medium | Monitoring |
| 4 | Connection internals | High | Internal only |

## Recommendations

1. **Start with TransactionBuilder** - isolated change, clear API boundary
2. **Use interface inheritance** - `ReactiveTransactionBuilder extends TransactionBuilder`
3. **Place reactive interface in `api/reactive`** - follows existing pattern
4. **Add functional API** - cleaner for reactive use cases
5. **Document migration** - clear upgrade path for users

## Implementation Status

1. [x] Create `ReactiveTransactionBuilder` interface in `api/reactive`
2. [x] Remove `executeReactive()` from `TransactionBuilder`
3. [x] Update implementations to implement `ReactiveTransactionBuilder`
4. [x] Add factory method to `RedisReactiveCommands`
5. [x] Update tests
6. [ ] Add migration documentation

## Completed Changes

| File | Change |
|------|--------|
| `TransactionBuilder.java` | Removed `executeReactive()`, no Reactor import |
| `ReactiveTransactionBuilder.java` | **NEW** - extends TransactionBuilder, adds `executeReactive()` |
| `TransactionBuilderImpl.java` | Now implements `ReactiveTransactionBuilder` |
| `ClusterTransactionBuilder.java` | Now implements `ReactiveTransactionBuilder` |
| `RedisReactiveCommands.java` | Added `transaction()`, `transaction(K...)`, `transactional()` methods |
| `RedisReactiveCommandsImpl.java` | Implements the new transaction methods |
| `AbstractRedisReactiveCommands.java` | Added `codec` field and `getCodec()` helper |
| Tests | Updated to use new API patterns |

## API Patterns

### Pattern 1: Builder-based (existing, updated)
```java
ReactiveTransactionBuilder<String, String> builder = connection.reactive().transaction();
builder.commands().set("key", "value");
Mono<TransactionResult> result = builder.executeReactive();
```

### Pattern 2: Functional API (NEW - recommended for reactive)
```java
// Simple usage
Mono<TransactionResult> result = connection.reactive().transactional(txn -> {
    txn.set("key", "value");
    txn.incr("counter");
});

// Composing with reactive data sources
Mono<TransactionResult> result = keyMono.flatMap(key ->
    valueMono.flatMap(value ->
        connection.reactive().transactional(txn -> {
            txn.set(key, value);
            txn.incr("counter");
        })
    )
);
```

