## Transactions/Multi

Transactions allow the execution of a group of commands in a single
step. Lettuce provides two approaches for working with transactions:

1. **Bundled Transactions** (recommended): Thread-safe transactions using `TransactionBuilder`
2. **Traditional MULTI/EXEC**: Low-level commands requiring external synchronization

## Bundled Transactions (Since 7.6)

Bundled transactions provide a thread-safe way to execute Redis transactions. Unlike traditional
MULTI/EXEC, bundled transactions encode all commands atomically and dispatch them as a single unit,
preventing command interleaving when sharing connections across threads.

### Basic Usage

``` java
// Create a transaction builder and add commands
TransactionBuilder<String, String> txn = connection.transaction();
txn.commands().set("key1", "value1");
txn.commands().set("key2", "value2");
txn.commands().incr("counter");
TransactionResult result = txn.execute();

// Access results by index
String setResult = result.get(0);  // "OK"
Long counterValue = result.get(2); // new counter value
```

### Async Transactions

``` java
TransactionBuilder<String, String> txn = connection.transaction();
txn.commands().set("key", "value");
txn.commands().get("key");
RedisFuture<TransactionResult> future = txn.executeAsync();

TransactionResult result = future.get();
```

### Reactive Transactions

``` java
TransactionBuilder<String, String> txn = connection.transaction();
txn.commands().set("key", "value");
txn.commands().get("key");
Mono<TransactionResult> mono = txn.executeReactive();

mono.subscribe(result -> {
    String value = result.get(1);
});
```

### Using WATCH

WATCH keys can be specified when creating the transaction builder:

``` java
// Watch keys for optimistic locking
TransactionBuilder<String, String> txn = connection.transaction("watched-key");
txn.commands().set("watched-key", "new-value");
TransactionResult result = txn.execute();

if (result.wasDiscarded()) {
    // Another client modified the watched key - retry logic here
}
```

### Cluster Transactions

When using Redis Cluster, all keys in the transaction must hash to the same slot.
Use hash tags to ensure keys map to the same slot:

``` java
// Keys with same hash tag - will succeed
TransactionBuilder<String, String> txn = connection.transaction();
txn.commands().set("{user123}:name", "John");
txn.commands().set("{user123}:email", "john@example.com");
txn.execute();

// Keys without hash tags may fail with cross-slot error
```

## Traditional MULTI/EXEC

Transactions can also be controlled using `WATCH`, `UNWATCH`, `EXEC`,
`MULTI` and `DISCARD` commands. Synchronous, asynchronous, and reactive
APIs allow the use of transactions.

!!! note
    Transactional use requires external synchronization when a single
    connection is used by multiple threads/processes. This can be achieved
    either by serializing transactions or by providing a dedicated
    connection to each concurrent process. Lettuce itself does not
    synchronize transactional/non-transactional invocations regardless of
    the used API facade.

Redis responds to commands invoked during a transaction with a `QUEUED`
response. The response related to the execution of the command is
received at the moment the `EXEC` command is processed, and the
transaction is executed. The particular APIs behave in different ways:

- Synchronous: Invocations to the commands return `null` while they are
  invoked within a transaction. The `MULTI` command carries the response
  of the particular commands.

- Asynchronous: The futures receive their response at the moment the
  `EXEC` command is processed. This happens while the `EXEC` response is
  received.

- Reactive: An `Obvervable<T>` triggers `onNext`/`onCompleted` at the
  moment the `EXEC` command is processed. This happens while the `EXEC`
  response is received.

As soon as you’re within a transaction, you won’t receive any responses
on triggering the commands

``` java
redis.multi() == "OK"
redis.set(key, value) == null
redis.exec() == list("OK")
```

You’ll receive the transactional response when calling `exec()` on the
end of your transaction.

``` java
redis.multi() == "OK"
redis.set(key1, value) == null
redis.set(key2, value) == null
redis.exec() == list("OK", "OK")
```

### Transactions using the asynchronous API

Asynchronous use of Redis transactions is very similar to
non-transactional use. The asynchronous API returns `RedisFuture`
instances that eventually complete and they are handles to a future
result. Regular commands complete as soon as Redis sends a response.
Transactional commands complete as soon as the `EXEC` result is
received.

Each command is completed individually with its own result so users of
`RedisFuture` will see no difference between transactional and
non-transactional `RedisFuture` completion. That said, transactional
command results are available twice: Once via `RedisFuture` of the
command and once through `List<Object>` (`TransactionResult` since
Lettuce 5) of the `EXEC` command future.

``` java
RedisAsyncCommands<String, String> async = client.connect().async();

RedisFuture<String> multi = async.multi();

RedisFuture<String> set = async.set("key", "value");

RedisFuture<List<Object>> exec = async.exec();

List<Object> objects = exec.get();
String setResult = set.get();

objects.get(0) == setResult
```

### Transactions using the reactive API

The reactive API can be used to execute multiple commands in a single
step. The nature of the reactive API encourages nesting of commands. It
is essential to understand the time at which an `Observable<T>` emits a
value when working with transactions. Redis responds with `QUEUED` to
commands invoked during a transaction. The response related to the
execution of the command is received at the moment the `EXEC` command is
processed, and the transaction is executed. Subsequent calls in the
processing chain are executed after the transactional end. The following
code starts a transaction, executes two commands within the transaction
and finally executes the transaction.

``` java
RedisReactiveCommands<String, String> reactive = client.connect().reactive();
reactive.multi().subscribe(multiResponse -> {
    reactive.set("key", "1").subscribe();
    reactive.incr("key").subscribe();
    reactive.exec().subscribe();
});
```

### Transactions on clustered connections

Clustered connections perform a routing by default. This means, that you
can’t be really sure, on which host your command is executed. So if you
are working in a clustered environment, use rather a regular connection
to your node, since then you’ll bound to that node knowing which hash
slots are handled by it.

### Examples

**Multi with executing multiple commands**

``` java
redis.multi();

redis.set("one", "1");
redis.set("two", "2");
redis.mget("one", "two");
redis.llen(key);

redis.exec(); // result: list("OK", "OK", list("1", "2"), 0L)
```

**Mult executing multiple asynchronous commands**

``` java
redis.multi();

RedisFuture<String> set1 = redis.set("one", "1");
RedisFuture<String> set2 = redis.set("two", "2");
RedisFuture<String> mget = redis.mget("one", "two");
RedisFuture<Long> llen = mgetredis.llen(key);


set1.thenAccept(value -> …); // OK
set2.thenAccept(value -> …); // OK

RedisFuture<List<Object>> exec = redis.exec(); // result: list("OK", "OK", list("1", "2"), 0L)

mget.get(); // list("1", "2")
llen.thenAccept(value -> …); // 0L
```

**Using WATCH**

``` java
redis.watch(key);

RedisConnection<String, String> redis2 = client.connect();
redis2.set(key, value + "X");
redis2.close();

redis.multi();
redis.append(key, "foo");
redis.exec(); // result is an empty list because of the changed key
```

## Migrating to Bundled Transactions

If you're currently using traditional MULTI/EXEC, consider migrating to bundled transactions
for thread-safety benefits. This section provides a comprehensive guide for migration.

### Why Migrate?

**Traditional MULTI/EXEC has threading issues:**

When multiple threads share a connection and use traditional transactions, commands from
different transactions can interleave on the wire:

```
Thread A: MULTI
Thread B: MULTI          <- Interleaved!
Thread A: SET key1 val1
Thread B: SET key2 val2  <- Wrong transaction!
Thread A: EXEC           <- Executes both SETs
Thread B: EXEC           <- Empty or unexpected results
```

**Bundled Transactions solve this** by encoding all commands atomically before dispatch,
ensuring the entire transaction is written to the network as a single unit.

### Migration Scenarios

#### Scenario 1: Basic Synchronous Transaction

**Before:**
``` java
// Traditional - NOT thread-safe without synchronization
RedisCommands<String, String> sync = connection.sync();
sync.multi();
sync.set("key1", "value1");
sync.set("key2", "value2");
sync.incr("counter");
TransactionResult result = sync.exec();
```

**After:**
``` java
// Bundled - thread-safe by design
TransactionBuilder<String, String> txn = connection.transaction();
txn.commands().set("key1", "value1");
txn.commands().set("key2", "value2");
txn.commands().incr("counter");
TransactionResult result = txn.execute();

// Access results by index
String setResult = result.get(0);  // "OK"
Long counterValue = result.get(2); // incremented value
```

#### Scenario 2: Asynchronous Transaction

**Before:**
``` java
RedisAsyncCommands<String, String> async = connection.async();
RedisFuture<String> multi = async.multi();
RedisFuture<String> set1 = async.set("key1", "value1");
RedisFuture<Long> incr = async.incr("counter");
RedisFuture<TransactionResult> exec = async.exec();

// Must wait for exec to get actual results
TransactionResult result = exec.get();
```

**After:**
``` java
TransactionBuilder<String, String> txn = connection.transaction();
txn.commands().set("key1", "value1");
txn.commands().incr("counter");
RedisFuture<TransactionResult> future = txn.executeAsync();

TransactionResult result = future.get();
```

#### Scenario 3: Reactive Transaction

**Before:**
``` java
RedisReactiveCommands<String, String> reactive = connection.reactive();
reactive.multi().subscribe(multiResponse -> {
    reactive.set("key", "value").subscribe();
    reactive.incr("counter").subscribe();
    reactive.exec().subscribe(result -> {
        // Handle result
    });
});
```

**After:**
``` java
TransactionBuilder<String, String> txn = connection.transaction();
txn.commands().set("key", "value");
txn.commands().incr("counter");
Mono<TransactionResult> mono = txn.executeReactive();

mono.subscribe(result -> {
    String setValue = result.get(0);
    Long counterValue = result.get(1);
});
```

#### Scenario 4: Transaction with WATCH (Optimistic Locking)

**Before:**
``` java
// Must handle WATCH separately
sync.watch("balance");
String currentBalance = sync.get("balance");
long balance = Long.parseLong(currentBalance);

if (balance >= 100) {
    sync.multi();
    sync.decrby("balance", 100);
    sync.incrby("purchases", 1);
    TransactionResult result = sync.exec();

    if (result.wasDiscarded()) {
        // Retry - balance was modified by another client
    }
}
```

**After:**
``` java
// WATCH keys passed to transaction() method
String currentBalance = connection.sync().get("balance");
long balance = Long.parseLong(currentBalance);

if (balance >= 100) {
    TransactionBuilder<String, String> txn = connection.transaction("balance");
    txn.commands().decrby("balance", 100);
    txn.commands().incrby("purchases", 1);
    TransactionResult result = txn.execute();

    if (result.wasDiscarded()) {
        // Retry - balance was modified by another client
    }
}
```

!!! note
    With bundled transactions, WATCH, MULTI, commands, and EXEC are sent atomically.
    This means you cannot read watched key values between WATCH and EXEC in the same
    atomic block - read the values before calling `transaction()`.

#### Scenario 5: Using External Synchronization

**Before:**
``` java
// Manual synchronization required
private final Object txnLock = new Object();

public void performTransaction() {
    synchronized (txnLock) {
        redis.multi();
        redis.set("key", "value");
        redis.exec();
    }
}
```

**After:**
``` java
// No synchronization needed - transactions are inherently thread-safe
public void performTransaction() {
    TransactionBuilder<String, String> txn = connection.transaction();
    txn.commands().set("key", "value");
    txn.execute();
}
```

#### Scenario 6: Full Redis Command Access

The `commands()` method provides access to all 400+ Redis commands:

``` java
TransactionBuilder<String, String> txn = connection.transaction();

// String commands
txn.commands().set("str", "value");
txn.commands().append("str", "-suffix");

// Hash commands
txn.commands().hset("hash", "field", "value");
txn.commands().hincrby("hash", "counter", 5);

// List commands
txn.commands().lpush("list", "item1", "item2");

// Set commands
txn.commands().sadd("set", "member1", "member2");

// Sorted set commands
txn.commands().zadd("zset", 1.0, "one");

// HyperLogLog
txn.commands().pfadd("hll", "elem1", "elem2");

// Execute all commands atomically
TransactionResult result = txn.execute();
```

### Key Differences

| Feature | Traditional MULTI/EXEC | Bundled Transactions |
|---------|------------------------|----------------------|
| Thread Safety | Requires external sync | Built-in |
| Command Dispatch | Individual commands | Atomic bundle |
| WATCH Support | Separate command | Builder parameter |
| Cluster Support | Manual node selection | Automatic routing |
| Command Interleaving | Possible | Prevented |
| API | Multiple method calls | Fluent builder |
| Result Access | Individual futures + exec result | Single TransactionResult |

### Checklist for Migration

1. **Replace `multi()`/`exec()` calls** with `connection.transaction()...execute()`
2. **Move WATCH keys** to `transaction(watchKey1, watchKey2, ...)` parameter
3. **Remove synchronization blocks** around transactions
4. **Update result handling** to use `TransactionResult.get(index)`
5. **Use `commands()`** for any Redis command in the transaction
6. **Test thoroughly** - especially multi-threaded scenarios

### When to Keep Traditional MULTI/EXEC

In rare cases, you may need traditional MULTI/EXEC:

- **Interactive transactions**: When you need to read values between MULTI and EXEC
  and make decisions based on QUEUED responses
- **Protocol-level debugging**: When you need to observe individual QUEUED responses
- **Legacy code constraints**: When refactoring is not feasible

For most use cases, bundled transactions are the recommended approach.

## Scripting and Functions

Redis functionality can be extended through many ways, of which [Lua
Scripting](https://redis.io/topics/eval-intro) and
[Functions](https://redis.io/topics/functions-intro) are two approaches
that do not require specific pre-requisites on the server.

