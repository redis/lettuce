# Connection Pooling

Lettuce connections are designed to be thread-safe so one connection can
be shared amongst multiple threads and Lettuce connections
[auto-reconnection](client-options.md) by default. While connection
pooling is not necessary in most cases it can be helpful in certain use
cases. Lettuce provides generic connection pooling support.

## Is connection pooling necessary?

Lettuce is thread-safe by design which is sufficient for most cases. All
Redis user operations are executed single-threaded. Using multiple
connections does not impact the performance of an application in a
positive way. The use of blocking operations usually goes hand in hand
with worker threads that get their dedicated connection. The use of
Redis Transactions is the typical use case for dynamic connection
pooling as the number of threads requiring a dedicated connection tends
to be dynamic. That said, the requirement for dynamic connection pooling
is limited. Connection pooling always comes with a cost of complexity
and maintenance.

## Execution Models

Lettuce supports two execution models for pooling:

- Synchronous/Blocking via Apache Commons Pool 2

- Asynchronous/Non-Blocking via a Lettuce-specific pool implementation
  (since version 5.1)

## Synchronous Connection Pooling

Using imperative programming models, synchronous connection pooling is
the right choice as it carries out all operations on the thread that is
used to execute the code.

### Prerequisites

Lettuce requires Apache’s
[common-pool2](https://commons.apache.org/proper/commons-pool/)
dependency (at least 2.2) to provide connection pooling. Make sure to
include that dependency on your classpath. Otherwise, you won’t be able
using connection pooling.

If using Maven, add the following dependency to your `pom.xml`:

``` xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
    <version>2.4.3</version>
</dependency>
```

### Connection pool support

Lettuce provides generic connection pool support. It requires a
connection `Supplier` that is used to create connections of any
supported type (Redis Standalone, Pub/Sub, Sentinel, Master/Replica,
Redis Cluster). `ConnectionPoolSupport` will create a
`GenericObjectPool` or `SoftReferenceObjectPool`, depending on your
needs. The pool can allocate either wrapped or direct connections.

- Wrapped instances will return the connection back to the pool when
  called `StatefulConnection.close()`.

- Regular connections need to be returned to the pool with
  `GenericObjectPool.returnObject(…)`.

**Basic usage**

``` java
RedisClient client = RedisClient.create(RedisURI.create(host, port));

GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
               .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig());

// executing work
try (StatefulRedisConnection<String, String> connection = pool.borrowObject()) {

    RedisCommands<String, String> commands = connection.sync();
    commands.multi();
    commands.set("key", "value");
    commands.set("key2", "value2");
    commands.exec();
}

// terminating
pool.close();
client.shutdown();
```

**Cluster usage**

``` java
RedisClusterClient clusterClient = RedisClusterClient.create(RedisURI.create(host, port));

GenericObjectPool<StatefulRedisClusterConnection<String, String>> pool = ConnectionPoolSupport
               .createGenericObjectPool(() -> clusterClient.connect(), new GenericObjectPoolConfig());

// execute work
try (StatefulRedisClusterConnection<String, String> connection = pool.borrowObject()) {
    connection.sync().set("key", "value");
    connection.sync().blpop(10, "list");
}

// terminating
pool.close();
clusterClient.shutdown();
```

## Asynchronous Connection Pooling

Asynchronous/non-blocking programming models require a non-blocking API
to obtain Redis connections. A blocking connection pool can easily lead
to a state that blocks the event loop and prevents your application from
progress in processing.

Lettuce comes with an asynchronous, non-blocking pool implementation to
be used with Lettuces asynchronous connection methods. It does not
require additional dependencies.

### Asynchronous Connection pool support

Lettuce provides asynchronous connection pool support. It requires a
connection `Supplier` that is used to asynchronously connect to any
supported type (Redis Standalone, Pub/Sub, Sentinel, Master/Replica,
Redis Cluster). `AsyncConnectionPoolSupport` will create a
`BoundedAsyncPool`. The pool can allocate either wrapped or direct
connections.

- Wrapped instances will return the connection back to the pool when
  called `StatefulConnection.closeAsync()`.

- Regular connections need to be returned to the pool with
  `AsyncPool.release(…)`.

**Basic usage**

``` java
RedisClient client = RedisClient.create();

CompletionStage<AsyncPool<StatefulRedisConnection<String, String>>> poolFuture = AsyncConnectionPoolSupport.createBoundedObjectPoolAsync(
        () -> client.connectAsync(StringCodec.UTF8, RedisURI.create(host, port)), BoundedPoolConfig.create());

// await poolFuture initialization to avoid NoSuchElementException: Pool exhausted when starting your application

// execute work
CompletableFuture<TransactionResult> transactionResult = pool.acquire().thenCompose(connection -> {

    RedisAsyncCommands<String, String> async = connection.async();

    async.multi();
    async.set("key", "value");
    async.set("key2", "value2");
    return async.exec().whenComplete((s, throwable) -> pool.release(connection));
});

// terminating
pool.closeAsync();

// after pool completion
client.shutdownAsync();
```

**Cluster usage**

``` java
RedisClusterClient clusterClient = RedisClusterClient.create(RedisURI.create(host, port));

CompletionStage<AsyncPool<StatefulRedisClusterConnection<String, String>>> poolFuture = AsyncConnectionPoolSupport.createBoundedObjectPoolAsync(
        () -> clusterClient.connectAsync(StringCodec.UTF8), BoundedPoolConfig.create());

// execute work
CompletableFuture<String> setResult = pool.acquire().thenCompose(connection -> {

    RedisAdvancedClusterAsyncCommands<String, String> async = connection.async();

    async.set("key", "value");
    return async.set("key2", "value2").whenComplete((s, throwable) -> pool.release(connection));
});

// terminating
pool.closeAsync();

// after pool completion
clusterClient.shutdownAsync();
```
