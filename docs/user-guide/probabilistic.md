# Probabilistic Data Structures support in Lettuce

Lettuce supports [Redis probabilistic data structures](https://redis.io/docs/latest/develop/data-types/probabilistic/) (formerly the RedisBloom module) starting from [Lettuce 7.7.0.RELEASE](https://github.com/redis/lettuce/releases/tag/7.7.0.RELEASE).

Probabilistic data structures trade a small, bounded error for large savings in memory and time. They answer set-membership, frequency, and rank queries over very large data sets using a fraction of the space an exact structure would need.

Lettuce exposes five structures, each through its own command interface with matching synchronous, asynchronous, reactive and Kotlin coroutine flavors:

| Structure | Command interface | Redis commands | Answers |
|-----------|-------------------|----------------|---------|
| [Bloom Filter](https://redis.io/docs/latest/develop/data-types/probabilistic/bloom-filter/) | `RedisBloomFilterCommands` | `BF.*` | "Have I probably seen this item?" (no false negatives) |
| [Cuckoo Filter](https://redis.io/docs/latest/develop/data-types/probabilistic/cuckoo-filter/) | `RedisCuckooFilterCommands` | `CF.*` | Membership with deletion support |
| [Top-K](https://redis.io/docs/latest/develop/data-types/probabilistic/top-k/) | `RedisTopKCommands` | `TOPK.*` | "What are the most frequent items?" |
| [Count-Min Sketch](https://redis.io/docs/latest/develop/data-types/probabilistic/count-min-sketch/) | `RedisCMSCommands` | `CMS.*` | Approximate per-item frequency |
| [T-Digest](https://redis.io/docs/latest/develop/data-types/probabilistic/t-digest/) | `RedisTDigestCommands` | `TDIGEST.*` | Quantiles and ranks over a stream of values |

## Getting Started

The probabilistic command interfaces are part of the standard `RedisCommands` aggregate, so they are available directly on a connection — no extra module or factory is required:

```java
RedisURI redisURI = RedisURI.Builder.redis("localhost").withPort(6379).build();
RedisClient redisClient = RedisClient.create(redisURI);
StatefulRedisConnection<String, String> connection = redisClient.connect();

RedisCommands<String, String> commands = connection.sync();
// commands is also a RedisBloomFilterCommands, RedisCuckooFilterCommands,
// RedisTopKCommands, RedisCMSCommands and RedisTDigestCommands
```

The examples below use the synchronous API. The asynchronous (`connection.async()`), reactive (`connection.reactive()`) and Kotlin coroutine (`connection.coroutines()`) APIs expose the same commands with their respective return types.

!!! INFO
    These structures require a Redis deployment that provides the probabilistic data-structure commands (Redis 8+ / Redis Stack). Against a server without them, the commands fail with an "unknown command" error.

## Bloom Filter

A Bloom filter is a space-efficient set that answers membership queries. A negative answer is always correct; a positive answer is correct within a configurable error rate.

```java
// Create a filter sized for 10,000 items with a 1% false-positive rate
commands.bfReserve("visitors", 0.01, 10_000);

Boolean added = commands.bfAdd("visitors", "alice");      // true — newly added
Boolean exists = commands.bfExists("visitors", "alice");  // true
Boolean missing = commands.bfExists("visitors", "bob");   // false (guaranteed correct)

// Add and test many items at once
List<Boolean> addedMany = commands.bfMAdd("visitors", "carol", "dave");
List<Boolean> existMany = commands.bfMExists("visitors", "alice", "bob");

Long count = commands.bfCard("visitors");                 // approximate item count
BfInfoValue info = commands.bfInfo("visitors");           // capacity, size, filters, ...
```

`bfReserve` can be tuned further with `BfReserveArgs`, and `bfInsert` combines reserve-and-add in a single call:

```java
commands.bfReserve("events", 0.001, 1_000_000, BfReserveArgs.expansion(4).nonScaling());

commands.bfInsert("events", BfInsertArgs.capacity(1_000_000).error(0.001).noCreate(), "e1", "e2");
```

## Cuckoo Filter

A Cuckoo filter answers the same membership question as a Bloom filter but additionally supports deletion and approximate per-item counts.

```java
commands.cfReserve("seen", 10_000);

Boolean added = commands.cfAdd("seen", "item-1");
Boolean addedNx = commands.cfAddNx("seen", "item-1");     // false — already present
Boolean exists = commands.cfExists("seen", "item-1");     // true
Long count = commands.cfCount("seen", "item-1");
Boolean deleted = commands.cfDel("seen", "item-1");       // Cuckoo filters support deletes

List<Boolean> inserted = commands.cfInsertNx("seen", "a", "b", "c");
CfInfoValue info = commands.cfInfo("seen");
```

Sizing can be tuned with `CfReserveArgs`:

```java
commands.cfReserve("seen", 100_000, CfReserveArgs.bucketSize(4).maxIterations(20).expansion(2));
```

## Top-K

Top-K keeps an approximate list of the `k` most frequent items in a stream.

```java
// Track the top 3 items; width, depth and decay control accuracy
commands.topKReserve("trending", 3);

List<String> dropped = commands.topKAdd("trending", "apple", "apple", "banana", "cherry", "date");
// Any item evicted from the top-K by these additions is returned; nulls otherwise

List<String> incremented = commands.topKIncrBy("trending", "apple", 5);

TopKInfoValue info = commands.topKInfo("trending");
```

`topKReserve` accepts `TopKReserveArgs` to tune the underlying sketch:

```java
commands.topKReserve("trending", 10, TopKReserveArgs.width(2000).depth(7).decay(0.9));
```

## Count-Min Sketch

A Count-Min Sketch estimates the frequency of items using a fixed amount of memory. Initialize it either by dimensions or by target error/probability, then merge sketches as needed.

```java
// Initialize by target error (0.1%) and probability of exceeding it (0.01%)
commands.cmsInitByProb("hits", 0.001, 0.0001);
// ... or by explicit width and depth
// commands.cmsInitByDim("hits", 2000, 5);

CMSInfoValue info = commands.cmsInfo("hits");

// Merge several source sketches into a destination (all created with the same dimensions)
commands.cmsInitByDim("merged", 2000, 5);
commands.cmsMerge("merged", "hits");
```

## T-Digest

A T-Digest maintains a compact summary of a distribution, answering quantile, rank and trimmed-mean queries over a stream of numeric values.

```java
commands.tdigestCreate("latencies");                        // optionally pass a compression value

commands.tdigestAdd("latencies", 12.0, 8.5, 34.2, 21.7, 5.0);

List<Double> quantiles = commands.tdigestQuantile("latencies", 0.5, 0.95, 0.99);
List<Double> cdf = commands.tdigestCDF("latencies", 20.0);
Double min = commands.tdigestMin("latencies");
Double max = commands.tdigestMax("latencies");
Double trimmedMean = commands.tdigestTrimmedMean("latencies", 0.1, 0.9);

TDigestInfoValue info = commands.tdigestInfo("latencies");
```

Digests can be merged, optionally overriding the destination and setting a compression level:

```java
commands.tdigestCreate("combined");
commands.tdigestMerge("combined", TDigestMergeArgs.compression(200).override(), "latencies");
```

## Asynchronous, Reactive and Kotlin usage

The same commands are available on the other API flavors. For example, with the reactive API:

```java
RedisReactiveCommands<String, String> reactive = connection.reactive();

reactive.bfReserve("visitors", 0.01, 10_000)
        .then(reactive.bfAdd("visitors", "alice"))
        .flatMap(added -> reactive.bfExists("visitors", "alice"))
        .subscribe(exists -> System.out.println("exists: " + exists));
```

See the [Asynchronous API](async-api.md), [Reactive API](reactive-api.md) and [Kotlin API](kotlin-api.md) guides for the general usage patterns of each flavor.

## See Also

- [Redis probabilistic data structures](https://redis.io/docs/latest/develop/data-types/probabilistic/) — upstream documentation
- [Compatibility and Roadmap](../compatibility-roadmap.md) — module support status
- [Redis Command Interfaces](../redis-command-interfaces.md) — custom command support for modules not yet natively supported
