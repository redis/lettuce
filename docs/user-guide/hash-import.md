# Hash import in Lettuce

Lettuce supports the `HIMPORT` command family starting from Lettuce 7.7, for bulk-loading many hashes that
share the same field names.

A normal `HSET` repeats the field names on every command. When you are importing a million records that all
have the same shape, that is a million copies of `name`, `email`, `age` on the wire. `HIMPORT` lets you declare
the field names once and then send only the values, positionally paired to them.

Declare the shared field names as a `HashImport`, then pass it to `himportSet` with the values for each hash:

```java
RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").build();
try (RedisClient client = RedisClient.create(redisURI)) {
    StatefulRedisConnection<String, String> connection = client.connect();
    RedisCommands<String, String> redis = connection.sync();

    try (HashImport<String> fieldset = HashImport.of("name", "email", "age")) {
        redis.himportSet("user:1", fieldset, "Alice", "alice@example.com", "34");
        redis.himportSet("user:2", fieldset, "Bob", "bob@example.com", "41");
    }

    // the imported keys are ordinary hashes
    redis.hget("user:1", "email");   // alice@example.com
    redis.hlen("user:1");            // 3
}
```

The number of values must match the number of fields, in the same order. The keys `HIMPORT` creates are
ordinary hashes — read them back with `HGET`, `HGETALL`, expire them, anything you would normally do.

!!! INFO
    The field names are declared to the server once per connection, the first time you use the fieldset on it.
    Lettuce re-declares them for you after a reconnect, on every connection of a pool, and on each Redis Cluster
    node your keys route to, so a `HashImport` is safe to share across threads and connections without any
    per-connection setup.

## Closing a fieldset

A fieldset holds state on every connection it was used on. Close it when the import is finished — either with
try-with-resources as above, or explicitly:

```java
HashImport<String> fieldset = HashImport.of("sku", "price");
try {
    redis.himportSet("product:1", fieldset, "SKU-1", "9.99");
} finally {
    fieldset.close();
}
```

Closing releases the server-side state and rejects any further use of that fieldset; a later `himportSet` with
it fails with an `IllegalStateException`. Imports you have already issued are not cancelled and still complete,
so the try-with-resources form is safe with the asynchronous and reactive APIs too:

```java
RedisAsyncCommands<String, String> redis = connection.async();
List<RedisFuture<String>> results = new ArrayList<>();

try (HashImport<String> fieldset = HashImport.of("name", "email")) {
    for (User user : users) {
        results.add(redis.himportSet(user.key(), fieldset, user.name(), user.email()));
    }
}

LettuceFutures.awaitAll(5, TimeUnit.SECONDS, results.toArray(new RedisFuture[0]));
```

## Other API flavors

`himportSet` is available on the synchronous, asynchronous, reactive, and Kotlin coroutine APIs, and on cluster
node selections.

```java
// reactive
Flux.fromIterable(users)
        .flatMap(user -> redis.reactive().himportSet(user.key(), fieldset, user.name(), user.email()))
        .then()
        .block();
```

!!! WARNING
    `HIMPORT` is not supported inside a `MULTI` transaction. The field names are per-connection session state
    declared outside the transaction, so calling `himportSet` while a transaction is open fails with an
    `UnsupportedOperationException`.

## Binary keys

For a codec other than `String`, supply a function that names the fieldset in that key type. It must return a
distinct name each time it is called — two live fieldsets sharing a name also share their server-side state:

```java
StatefulRedisConnection<byte[], byte[]> connection = client.connect(ByteArrayCodec.INSTANCE);
RedisCommands<byte[], byte[]> redis = connection.sync();

byte[][] fields = { "name".getBytes(), "email".getBytes() };
try (HashImport<byte[]> fieldset = HashImport.of(seq -> ("fields:" + seq).getBytes(), fields)) {
    redis.himportSet("user:1".getBytes(), fieldset, "Alice".getBytes(), "alice@example.com".getBytes());
}
```
