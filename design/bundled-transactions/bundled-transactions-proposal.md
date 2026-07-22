# Bundled Transactions Proposal

**Related Issue**: [#3084 - Batch multiple commands together](https://github.com/redis/lettuce/issues/3084)

## Problem Statement

The current Lettuce transaction implementation has fundamental thread-safety issues:

1. **Command Interleaving**: When multiple threads share a connection, transaction commands (MULTI, commands, EXEC) can be interleaved, breaking atomicity
2. **State Leakage**: The `multi` field in `StatefulRedisConnectionImpl` is connection-scoped, not thread-scoped
3. **No Shared Connection Support**: Transactions on shared connections are not supported, forcing users to use connection pools

### Current Flow (Problematic)

```
Thread A: MULTI ─────────────────────────────────────────> Redis
Thread B: SET x 1 ──────────────────────────────────────-> Redis  (OUTSIDE transaction!)
Thread A: SET key value ────────────────────────────────-> Redis
Thread A: EXEC ─────────────────────────────────────────-> Redis
```

Thread B's command gets interleaved between Thread A's transaction commands.

## Proposed Solution: Transaction Bundles

Instead of sending transaction commands individually, bundle the **entire transaction** as a single atomic unit that is dispatched atomically to the Netty layer.

### Core Concept

```
┌─────────────────────────────────────────────────────────────────┐
│                    TransactionBundle                             │
│  ┌─────────┬──────────────┬──────────────┬─────────┐            │
│  │  MULTI  │  Command 1   │  Command N   │  EXEC   │            │
│  └─────────┴──────────────┴──────────────┴─────────┘            │
│         Prepared & dispatched as single atomic unit             │
└─────────────────────────────────────────────────────────────────┘
```

## API Design

### 1. Transaction Builder Pattern

```java
public interface TransactionBuilder<K, V> {
    
    // All standard Redis commands return the builder for chaining
    TransactionBuilder<K, V> set(K key, V value);
    TransactionBuilder<K, V> get(K key);
    TransactionBuilder<K, V> hset(K key, K field, V value);
    TransactionBuilder<K, V> lpush(K key, V... values);
    // ... all other Redis commands
    
    // Execute the transaction, returns results
    TransactionResult execute();
    
    // Async variant
    RedisFuture<TransactionResult> executeAsync();
    
    // Reactive variant
    Mono<TransactionResult> executeReactive();
}
```

### 2. Factory Method on Connection

```java
public interface StatefulRedisConnection<K, V> {
    
    /**
     * Creates a new transaction builder.
     * The transaction is NOT started until execute() is called.
     * Thread-safe: multiple threads can create independent builders.
     */
    TransactionBuilder<K, V> transaction();
    
    /**
     * Creates a transaction with WATCH on specified keys.
     */
    TransactionBuilder<K, V> transaction(K... watchKeys);
}
```

### 3. Usage Examples

**Sync API**:
```java
TransactionResult result = connection.transaction()
    .set("key1", "value1")
    .set("key2", "value2")
    .incr("counter")
    .execute();

String setResult1 = result.get(0);  // "OK"
String setResult2 = result.get(1);  // "OK"  
Long incrResult = result.get(2);    // 1L
```

**Async API**:
```java
RedisFuture<TransactionResult> future = connection.transaction()
    .set("key1", "value1")
    .get("key1")
    .executeAsync();

future.thenAccept(result -> {
    // Process results
});
```

**Reactive API**:
```java
Mono<TransactionResult> mono = connection.transaction()
    .set("key1", "value1")
    .get("key1")
    .executeReactive();

mono.subscribe(result -> {
    // Process results
});
```

**With WATCH**:
```java
TransactionResult result = connection.transaction("watchedKey")
    .set("watchedKey", "newValue")
    .execute();

if (result.wasDiscarded()) {
    // WATCH detected modification, retry
}
```

## Internal Architecture

### New Components

#### 1. `TransactionBundle` - The Atomic Command Container

```java
public class TransactionBundle<K, V> implements RedisCommand<K, V, TransactionResult> {
    
    private final List<RedisCommand<K, V, ?>> commands;
    private final K[] watchKeys;
    private final MultiOutput<K, V> output;
    
    // Encodes: WATCH (optional) + MULTI + all commands + EXEC
    @Override
    public void encode(ByteBuf buf) {
        if (watchKeys != null) {
            encodeWatch(buf);
        }
        encodeMulti(buf);
        for (RedisCommand<K, V, ?> cmd : commands) {
            cmd.encode(buf);
        }
        encodeExec(buf);
    }
    
    // Returns count of expected responses
    public int getExpectedResponseCount() {
        return (watchKeys != null ? 1 : 0)  // WATCH response
             + 1                             // MULTI response
             + commands.size()               // QUEUED responses
             + 1;                            // EXEC response (array)
    }
}
```

#### 2. `TransactionBuilderImpl` - Command Collection

```java
public class TransactionBuilderImpl<K, V> implements TransactionBuilder<K, V> {
    
    private final StatefulRedisConnection<K, V> connection;
    private final RedisCodec<K, V> codec;
    private final List<RedisCommand<K, V, ?>> commands = new ArrayList<>();
    private final K[] watchKeys;
    
    @Override
    public TransactionBuilder<K, V> set(K key, V value) {
        commands.add(commandBuilder.set(key, value));
        return this;
    }
    
    @Override
    public TransactionResult execute() {
        TransactionBundle<K, V> bundle = new TransactionBundle<>(commands, watchKeys, codec);
        return connection.dispatch(bundle).get();  // Atomic dispatch
    }
}
```

### Dispatch Path Changes

#### Current Flow vs Proposed Flow

**Current (Per-Command)**:
```
User        Connection       Endpoint       CommandHandler      Netty
  |             |                |                |                |
  |--multi()--->|--dispatch(MULTI)-->           -->--MULTI------->|
  |--set()----->|--dispatch(SET)--->            -->--SET--------->|
  |--exec()---->|--dispatch(EXEC)-->            -->--EXEC-------->|
  |             |                |                |                |
  (3 separate dispatch operations - can be interleaved by other threads)
```

**Proposed (Bundle)**:
```
User        Connection       Endpoint       CommandHandler      Netty
  |             |                |                |                |
  |--execute()->|                |                |                |
  |             |--dispatch(TransactionBundle)------------------>|
  |             |                |                |  MULTI        |
  |             |                |                |  SET          |
  |             |                |                |  EXEC         |
  |             |                |                |  (atomic write)
  (Single dispatch = atomic, no interleaving possible)
```

### Key Implementation Details

#### 1. Atomic Write in DefaultEndpoint

The `TransactionBundle` is dispatched as a **single command**, ensuring atomic queue insertion:

```java
// In DefaultEndpoint.write()
public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {
    // TransactionBundle is a single command
    // Lock acquired once, bundle written atomically
    sharedLock.incrementWriters();
    try {
        if (autoFlushCommands) {
            writeToChannelAndFlush(channel, command);  // Atomic
        }
    } finally {
        sharedLock.decrementWriters();
    }
}
```

#### 2. Response Handling in CommandHandler

The `TransactionBundle` handles multi-response correlation internally:

```java
// In CommandHandler - TransactionBundle is ONE command on the stack
stack.add(bundle);  // Single entry

// When responses arrive:
// Response 1: +OK (MULTI)        -> bundle.consumeMultiResponse()
// Response 2: +QUEUED (cmd 1)    -> bundle.consumeQueuedResponse()
// Response N: +QUEUED (cmd N-1)  -> bundle.consumeQueuedResponse()
// Response N+1: *array (EXEC)    -> bundle.consumeExecResponse() -> complete()
```

The bundle tracks which response phase it's in:

```java
public class TransactionBundle<K, V> {

    private enum Phase { WATCH, MULTI, QUEUED, EXEC }
    private Phase currentPhase = Phase.MULTI;  // or WATCH if watching
    private int queuedCount = 0;

    public boolean decode(ByteBuf buffer, CommandOutput<K, V, ?> output) {
        switch (currentPhase) {
            case WATCH:
                // Decode +OK, transition to MULTI
                break;
            case MULTI:
                // Decode +OK, transition to QUEUED
                break;
            case QUEUED:
                // Decode +QUEUED, count++
                if (queuedCount == commands.size()) {
                    currentPhase = Phase.EXEC;
                }
                break;
            case EXEC:
                // Decode array, distribute results to individual commands
                return decodeExecArray(buffer);
        }
        return false;  // More data expected
    }
}
```

#### 3. Result Distribution

When EXEC array is received, results are distributed to individual command outputs:

```java
private void distributeExecResults(List<Object> execResults) {
    for (int i = 0; i < commands.size(); i++) {
        RedisCommand<K, V, ?> cmd = commands.get(i);
        Object result = execResults.get(i);
        // Route result to command's output
        routeResultToOutput(cmd.getOutput(), result);
        cmd.complete();
    }
}
```

## Cluster Mode Handling

### Known Limitation

Redis Cluster transactions require all keys to be on the **same slot**. The `TransactionBundle` approach cannot automatically guarantee this.

### Proposed Strategy: Fail-Fast Validation

```java
public class ClusterTransactionBuilder<K, V> implements TransactionBuilder<K, V> {

    private final RedisCodec<K, V> codec;
    private Integer targetSlot = null;

    @Override
    public TransactionBuilder<K, V> set(K key, V value) {
        validateSlot(key);
        commands.add(commandBuilder.set(key, value));
        return this;
    }

    private void validateSlot(K key) {
        int slot = SlotHash.getSlot(codec.encodeKey(key));
        if (targetSlot == null) {
            targetSlot = slot;
        } else if (targetSlot != slot) {
            throw new RedisException(
                "All keys in a cluster transaction must belong to the same slot. " +
                "Expected slot " + targetSlot + ", but key hashes to slot " + slot);
        }
    }

    @Override
    public TransactionResult execute() {
        if (targetSlot == null) {
            throw new RedisException("Transaction contains no commands");
        }
        // Route entire bundle to the node owning targetSlot
        StatefulRedisConnection<K, V> nodeConnection =
            clusterConnection.getConnection(targetSlot);
        return nodeConnection.dispatch(bundle).get();
    }
}
```

### Alternative: Disable for Cluster (Simple Approach)

```java
// In RedisClusterClient
public StatefulRedisClusterConnection<K, V> connect() {
    StatefulRedisClusterConnectionImpl<K, V> conn = ...;
    conn.setTransactionBundlesEnabled(false);  // Disabled by default
    return conn;
}

// In StatefulRedisClusterConnectionImpl
@Override
public TransactionBuilder<K, V> transaction() {
    if (!transactionBundlesEnabled) {
        throw new UnsupportedOperationException(
            "Transaction bundles are not supported on cluster connections. " +
            "Use getConnection(nodeId).transaction() for single-node transactions.");
    }
    return new ClusterTransactionBuilder<>(this, codec);
}
```

## Migration Strategy

### Phase 1: New API (Non-Breaking)

- Add `TransactionBuilder` interface
- Add `transaction()` factory method to `StatefulRedisConnection`
- Existing MULTI/EXEC API remains unchanged
- Users can opt-in to new bundle API

### Phase 2: Deprecation (Next Major Version)

- Mark existing `multi()`, `exec()`, `discard()` methods as `@Deprecated`
- Document migration path to bundle API

### Phase 3: Removal (Future Major Version)

- Remove deprecated transaction methods
- Bundle API becomes the only supported approach

## Implementation Plan

### Step 1: Core Infrastructure

| Task | Description | Complexity |
|------|-------------|------------|
| `TransactionBundle` class | Command container with multi-response decoding | High |
| `BundleOutput` | Output class that collects MULTI/QUEUED/EXEC responses | Medium |
| `TransactionBuilder` interface | API contract with all Redis commands | Medium |
| `TransactionBuilderImpl` | Builder implementation | Medium |

### Step 2: Connection Integration

| Task | Description | Complexity |
|------|-------------|------------|
| Add `transaction()` to `StatefulRedisConnection` | Factory method | Low |
| Implement in `StatefulRedisConnectionImpl` | Wire up builder creation | Low |
| Update `CommandHandler` | Handle `TransactionBundle` response sequence | High |

### Step 3: Cluster Support

| Task | Description | Complexity |
|------|-------------|------------|
| `ClusterTransactionBuilder` | Slot validation and routing | Medium |
| Fail-fast validation | Check all keys hash to same slot | Low |
| Single-node routing | Route bundle to correct node | Medium |

### Step 4: Testing & Documentation

| Task | Description | Complexity |
|------|-------------|------------|
| Unit tests for bundle encoding/decoding | Verify RESP protocol | Medium |
| Integration tests for thread safety | Multi-threaded scenarios | High |
| Cluster integration tests | Slot validation and routing | Medium |
| Documentation | User guide and migration docs | Low |

## Testing Strategy

### Thread Safety Tests

```java
@Test
void transactionsDoNotInterleaveAcrossThreads() throws Exception {
    int threadCount = 10;
    int iterationsPerThread = 100;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    List<Future<Boolean>> futures = new ArrayList<>();

    for (int t = 0; t < threadCount; t++) {
        int threadId = t;
        futures.add(executor.submit(() -> {
            for (int i = 0; i < iterationsPerThread; i++) {
                String key = "thread-" + threadId + "-iter-" + i;
                TransactionResult result = connection.transaction()
                    .set(key, "start")
                    .get(key)
                    .set(key, "end")
                    .execute();

                // Within transaction, GET should see "start" (set in same TX)
                // or nothing (if key didn't exist)
                String getValue = result.get(1);
                if (!"start".equals(getValue) && getValue != null) {
                    return false;  // Another thread interfered!
                }
            }
            return true;
        }));
    }

    for (Future<Boolean> future : futures) {
        assertThat(future.get()).isTrue();
    }
}
```

### WATCH Semantics Test

```java
@Test
void watchedKeyModificationDiscardsTransaction() {
    // Thread 1: Start transaction with WATCH
    TransactionResult result = connection.transaction("watchedKey")
        .set("watchedKey", "value1")
        .executeAsync();

    // Thread 2: Modify the watched key before EXEC
    connection.sync().set("watchedKey", "modified");

    TransactionResult txResult = result.get();
    assertThat(txResult.wasDiscarded()).isTrue();
}
```

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Bundle size limits | Very large transactions may exceed buffer sizes | Add configurable max command count; warn at thresholds |
| Response desync | Bug in response handling breaks connection state | Comprehensive tests; connection reset on desync |
| Memory pressure | Buffering many commands before dispatch | Lazy command encoding; streaming if needed |
| Cluster slot changes | Target node may change during transaction | Handle MOVED/ASK in bundle; retry entire transaction |
| WATCH timeout | Long delay between transaction() and execute() | Document best practices; optional timeout |

## Comparison: Current vs Proposed

| Aspect | Current MULTI/EXEC | Proposed Bundles |
|--------|-------------------|------------------|
| Thread safety | No (commands interleave) | Yes (atomic dispatch) |
| Shared connections | Not supported | Fully supported |
| API ergonomics | Imperative, stateful | Fluent builder, stateless |
| Result access | Via individual futures | Via indexed TransactionResult |
| Cluster support | Manual node selection | Automatic slot validation |
| Memory overhead | Per-command dispatch | Buffered until execute() |
| Backward compatibility | N/A | Existing API preserved |

## Open Questions

1. **Should WATCH be part of the bundle or separate?**
   - Including WATCH means atomic dispatch but limits flexibility
   - Separate WATCH allows checking conditions before building transaction

2. **How to handle command-specific options?**
   - Some commands have complex argument builders (e.g., SET with NX/XX/EX)
   - Option: Mirror existing command signatures in builder

3. **Should we support nested/conditional commands?**
   - E.g., "if GET returns X, then SET Y"
   - This is Lua scripting territory; likely out of scope

4. **Result type safety?**
   - `result.get(0)` returns `Object`
   - Option: Typed result handles like `result.get(setHandle)` where `setHandle` was returned by `.set()`

## Conclusion

The bundled transaction approach solves the fundamental thread-safety issues with the current implementation by ensuring the entire transaction is dispatched atomically. The builder pattern provides a clean, fluent API that is thread-safe by design.

Key benefits:
- **Thread-safe**: No interleaving possible
- **Shared connection support**: Multiple threads can use transactions on same connection
- **Clean API**: Builder pattern is intuitive and prevents invalid states
- **Backward compatible**: Existing API remains, new API is opt-in
- **Cluster-aware**: Validates slot consistency, routes appropriately

