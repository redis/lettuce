# Lettuce Redis Transactions - Internal Analysis

This document provides a detailed analysis of how Lettuce executes Redis transactions (MULTI/EXEC) across different API layers and cluster configurations.

## Overview

Redis transactions in Lettuce work through the `MULTI`, `EXEC`, `DISCARD`, and `WATCH` commands. The key insight is that commands issued within a transaction are **queued on both the server and client side** and only executed atomically when `EXEC` is called.

## Architecture Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                        User API Layer                            │
│  (RedisCommands / RedisAsyncCommands / RedisReactiveCommands)   │
├─────────────────────────────────────────────────────────────────┤
│                 StatefulRedisConnectionImpl                      │
│    - Manages MultiOutput state                                   │
│    - Wraps commands in TransactionalCommand                     │
├─────────────────────────────────────────────────────────────────┤
│                     DefaultEndpoint                              │
│    - Manages autoFlush mode                                      │
│    - Buffers commands when autoFlush=false                      │
├─────────────────────────────────────────────────────────────────┤
│                     CommandHandler                               │
│    - Netty ChannelDuplexHandler                                 │
│    - Maintains command stack for response correlation           │
├─────────────────────────────────────────────────────────────────┤
│                     CommandEncoder                               │
│    - Encodes RedisCommand to RESP protocol bytes                │
├─────────────────────────────────────────────────────────────────┤
│                     Netty Channel                                │
│    - TCP connection to Redis                                    │
└─────────────────────────────────────────────────────────────────┘
```

## State Management in `StatefulRedisConnectionImpl`

The connection maintains transaction state through a `MultiOutput<K, V> multi` field:

### Key Pre-Processing Logic (`preProcessCommand`)

```java
// When MULTI is received:
if (commandType.equals(MULTI.name())) {
    authHandler.startTransaction();
    multi = (multi == null ? new MultiOutput<>(codec) : multi);
    // Attach completion handler to reset state on failure
}

// For commands within transaction (not MULTI, WATCH, EXEC, DISCARD):
if (multi != null && !commandType.equals(MULTI.name()) && !commandType.equals(WATCH.name())) {
    local = new TransactionalCommand<>(local);  // Wrap command
    multi.add(local);                            // Add to queue
}

// When EXEC is received:
if (commandType.equals(EXEC.name())) {
    MultiOutput<K, V> multiOutput = this.multi;
    this.multi = null;
    local.setOutput((MultiOutput) multiOutput);  // Set output to collect results
}

// When DISCARD is received:
if (commandType.equals(DISCARD.name())) {
    if (multi != null) {
        multi.cancel();  // Complete all queued commands
        multi = null;
    }
}
```

## TransactionalCommand - The Dual Completion Mechanism

The `TransactionalCommand` extends `AsyncCommand` with a **count of 2**:

```java
public class TransactionalCommand<K, V, T> extends AsyncCommand<K, V, T> {
    public TransactionalCommand(RedisCommand<K, V, T> command) {
        super(command, 2);  // Requires 2 completions
    }
}
```

**Why count=2?**

1. **First completion**: When Redis responds with `QUEUED` after receiving the command
2. **Second completion**: When Redis responds with actual result during `EXEC`

The `AsyncCommand.complete()` method decrements the count:

```java
public void complete() {
    if (COUNT_UPDATER.decrementAndGet(this) == 0) {
        completeResult();  // Only completes future when count reaches 0
        command.complete();
    }
}
```

## MultiOutput - Collecting Transaction Results

`MultiOutput` acts as a special output that:

1. Maintains a queue of `RedisCommand` objects
2. Routes response data to the appropriate queued command's output
3. Completes each command as its result arrives during `EXEC` response

```java
public void complete(int depth) {
    if (depth == 1) {
        RedisCommand<K, V, ?> cmd = queue.remove();
        CommandOutput<K, V, ?> o = cmd.getOutput();
        responses.add(!o.hasError() ? o.get() : ExceptionFactory.createExecutionException(o.getError()));
        cmd.complete();  // Second completion for TransactionalCommand
    }
}
```

## API-Specific Behavior

### Blocking API (RedisCommands)

The sync API uses `FutureSyncInvocationHandler` which:

1. Delegates to async API
2. Waits for `RedisFuture` completion
3. **Special handling for transactions**: Returns `null` immediately for commands within a transaction

```java
if (!isTxControlMethod(method.getName(), args) && isTransactionActive(connection)) {
    return null;  // Don't wait - result comes via EXEC
}
```

**User impact**: Sync commands within MULTI return `null`:
```java
redis.multi();
assertThat(redis.set(key, value)).isNull();  // Returns null, not OK
redis.exec();  // TransactionResult contains actual results
```

### Async API (RedisAsyncCommands)

Returns `RedisFuture<T>` for each command:

```java
RedisFuture<String> multi = async.multi();
RedisFuture<String> set = async.set("key", "value");  // Not completed yet
RedisFuture<TransactionResult> exec = async.exec();

exec.get();        // Waits for EXEC
set.get();         // Now completes with "OK"
```

The future for transactional commands completes only after `EXEC` returns.

### Reactive API (RedisReactiveCommands)

Uses `RedisPublisher` to emit results:

```java
Mono<String> multi = reactive.multi();
Mono<String> set = reactive.set("key", "value");  // Cold Mono
Mono<TransactionResult> exec = reactive.exec();
```

**Important**: Reactive requires explicit subscription:
```java
multi.subscribe();
set.subscribe();
exec.block();  // Results available
```

## Netty Integration and Command Flow

### DefaultEndpoint - Command Writing

When `autoFlushCommands=true` (default):
```java
if (autoFlushCommands) {
    Channel channel = this.channel;
    if (isConnected(channel)) {
        writeToChannelAndFlush(channel, command);
    } else {
        writeToDisconnectedBuffer(command);
    }
}
```

When `autoFlushCommands=false` (for pipelining):
```java
} else {
    writeToBuffer(command);  // Commands buffered in commandBuffer
}
// Later: flushCommands() drains buffer and writes to channel
```

### CommandHandler - The Netty Handler

`CommandHandler` extends `ChannelDuplexHandler` and:

1. **On write**: Adds command to response stack, encodes and writes to channel
2. **On read**: Decodes response, correlates with stack, completes command

```java
private void addToStack(RedisCommand<?, ?, ?> command, ChannelPromise promise) {
    if (command.getOutput() == null) {
        complete(command);  // Fire-and-forget
    }
    RedisCommand<?, ?, ?> redisCommand = potentiallyWrapLatencyCommand(command);
    stack.add(redisCommand);  // Add to correlation stack
}
```

### CommandEncoder - RESP Protocol Encoding

Converts `RedisCommand` to RESP bytes:
```java
protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) {
    if (msg instanceof RedisCommand) {
        RedisCommand<?, ?, ?> command = (RedisCommand<?, ?, ?>) msg;
        command.encode(out);  // Write RESP bytes to buffer
    }
}
```

## Sequence Diagram - Transaction Flow

```
User          Connection       Endpoint       CommandHandler      Redis
  |               |                |                |               |
  |--multi()----->|                |                |               |
  |               |--preProcess--->|                |               |
  |               |  (init multi)  |                |               |
  |               |--------------->|--write-------->|--*3 MULTI---->|
  |               |                |                |<--+OK----------|
  |               |<---------------|<---------------|               |
  |<--"OK"--------|                |                |               |
  |               |                |                |               |
  |--set(k,v)---->|                |                |               |
  |               |--preProcess--->|                |               |
  |               | (wrap in       |                |               |
  |               |  Transactional |                |               |
  |               |  Command)      |                |               |
  |               |--------------->|--write-------->|--*3 SET k v-->|
  |               |                |                |<--+QUEUED-----|
  |               |                |                |  (1st complete)|
  |<--null/Future-|                |                |               |
  |               |                |                |               |
  |--exec()------>|                |                |               |
  |               |--preProcess--->|                |               |
  |               | (set MultiOutput)              |               |
  |               |--------------->|--write-------->|--*1 EXEC----->|
  |               |                |                |<--*1 array----|
  |               |                |                |  (results)    |
  |               |                |                |  (2nd complete)|
  |<--TxResult----|<---------------|<---------------|               |
```

## Redis Cluster Transactions

### Limitation: Single-Node Scope

Redis transactions must execute on a **single node**. In cluster mode:

1. `RedisAdvancedClusterCommands` routes commands by key hash slot
2. Transaction commands may route to different nodes = **broken transaction**

### Recommended Approach

Use a direct node connection:
```java
StatefulRedisClusterConnection<String, String> clusterConn = clusterClient.connect();
// Get connection to specific node
RedisClusterCommands<String, String> nodeCmd = clusterConn.getConnection("nodeId").sync();

nodeCmd.multi();
nodeCmd.set("key1", "value1");  // Ensure all keys hash to this node
nodeCmd.set("key2", "value2");
nodeCmd.exec();
```

### ClusterDistributionChannelWriter

The cluster writer routes by key slot:
```java
ByteBuffer encodedKey = args.getFirstEncodedKey();
if (encodedKey != null) {
    int hash = getSlot(encodedKey);  // Calculate slot
    // Route to appropriate node
    CompletableFuture<StatefulRedisConnection<K, V>> connectFuture =
        clusterConnectionProvider.getConnectionAsync(connectionIntent, hash);
}
```

This routing happens **per-command**, not per-transaction, which is why cluster transactions require single-node connections.

## Summary

| Component | Transaction Role |
|-----------|------------------|
| `StatefulRedisConnectionImpl` | Maintains `multi` state, wraps commands |
| `TransactionalCommand` | Dual-completion (QUEUED + EXEC result) |
| `MultiOutput` | Collects and routes EXEC results to commands |
| `FutureSyncInvocationHandler` | Returns null for sync commands in TX |
| `DefaultEndpoint` | Buffers commands if autoFlush=false |
| `CommandHandler` | Correlates responses via stack |
| `CommandEncoder` | Encodes to RESP bytes |

The elegant dual-completion mechanism ensures that:
1. Commands within a transaction are tracked client-side
2. `QUEUED` responses don't prematurely complete futures
3. Actual results from `EXEC` are properly routed to each command's future

