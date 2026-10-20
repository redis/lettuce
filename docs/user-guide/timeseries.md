# Redis TimeSeries support in Lettuce

Lettuce supports [Redis TimeSeries](https://redis.io/docs/latest/develop/data-types/timeseries/) starting from Lettuce 8.0.

A time series stores samples: a timestamp in milliseconds and a `double` value. A series can carry labels (name/value pairs) that other commands use to find it, and compaction rules that aggregate its samples into another series.

The TimeSeries commands are exposed through `RedisTimeSeriesCommands`, with matching asynchronous (`RedisTimeSeriesAsyncCommands`), reactive (`RedisTimeSeriesReactiveCommands`) and Kotlin coroutine (`RedisTimeSeriesCoroutinesCommands`) flavors. Argument types live in `io.lettuce.core.timeseries.arguments`, result and value types in `io.lettuce.core.timeseries`.

## Getting Started

The TimeSeries command interfaces are part of the standard `RedisCommands` aggregate, so they are available directly on a connection:

```java
RedisClient redisClient = RedisClient.create("redis://localhost:6379");
StatefulRedisConnection<String, String> connection = redisClient.connect();

RedisCommands<String, String> commands = connection.sync();
// commands is also a RedisTimeSeriesCommands
```

The examples below use the synchronous API. The asynchronous, reactive and Kotlin coroutine APIs expose the same commands with their respective return types.

Sample values are `double` and labels are `String`, whatever codec the connection uses. Keys use the connection codec.

## Server versions

| Server | Support |
|--------|---------|
| Redis 8.0 and later | Supported and tested. TimeSeries ships with the server. |
| Redis 8.6 and later | NaN sample values, and the `countNaN` / `countAll` aggregations and reducers. |
| Redis 8.8 and later | Several aggregations in one range query (for example `min,avg,max`). |
| Redis 8.10 and later | `TS.NRANGE`, `TS.NREVRANGE`, `TS.QUERYLABELS`, `TS.READ`, and `EXCLUDEEMPTY` on `TS.MRANGE` / `TS.MREVRANGE`. |

Lettuce does not check the server version. A command or option the server does not support is sent anyway, and the server rejects it. The Javadoc of each method names the server version it needs.

Older servers and the standalone RedisTimeSeries module are not blocked, but they are not tested or documented. Redis Software and Redis Cloud mark `TS.NRANGE`, `TS.NREVRANGE`, `TS.READ` and `TS.QUERYLABELS` as not supported.

## Value types

Aggregations (`TsAggregation`), duplicate policies (`TsDuplicatePolicy`) and chunk encodings (`TsEncoding`) are interfaces with an enum of the values this Lettuce version knows (`TsAggregationType`, `TsDuplicatePolicyType`, `TsEncodingType`). Pass an enum constant, or `of(String)` for a value that a newer server supports but Lettuce does not know yet:

```java
TsAggregation avg = TsAggregationType.AVG;
TsAggregation same = TsAggregation.of("avg");          // returns TsAggregationType.AVG
TsAggregation future = TsAggregation.of("median");     // sent as given; the server decides whether it is valid
```

Values the server reports, for example in `TS.INFO`, come back as the enum constant when Lettuce knows them, and as a value that keeps the server's name otherwise. A value added in a newer server version never breaks parsing.

## Series lifecycle

### Create a series

`tsCreate` creates an empty series. Options that are not set use the server's configured defaults:

```java
commands.tsCreate("temperature:room1");

commands.tsCreate("temperature:room2", TsCreateArgs.Builder.retention(Duration.ofDays(7))
        .duplicatePolicy(TsDuplicatePolicyType.LAST)
        .encoding(TsEncodingType.COMPRESSED)
        .chunkSize(4096)
        .label("sensor", "temperature")
        .label("room", "2"));
```

| Option | Setter | Notes |
|--------|--------|-------|
| `RETENTION` | `retention(long millis)`, `retention(Duration)` | Maximum age of samples relative to the newest sample. `0` keeps samples forever. A `Duration` that is not zero but shorter than one millisecond is rejected. |
| `ENCODING` | `encoding(TsEncoding)` | Chunk encoding. Not available on `TS.ALTER`. |
| `CHUNK_SIZE` | `chunkSize(long bytes)` | Memory size of each chunk. The server requires a multiple of 8 in its supported range. |
| `DUPLICATE_POLICY` | `duplicatePolicy(TsDuplicatePolicy)` | What happens when a sample with an existing timestamp is added. |
| `IGNORE` | `ignore(long maxTimeDiff, double maxValDiff)` | Ignore a new sample that is close to the newest sample in time and value. The server applies it only when the duplicate policy of the series is `LAST`; with any other policy the sample is stored. |
| `LABELS` | `label(name, value)`, `labels(Map)` | Labels are sent in insertion order. |

Lettuce always sends `LABELS` last. The server stores every argument that follows `LABELS` as a label pair, so an option after it would silently become a label.

### Change a series

`tsAlter` changes only the options that are set. Labels replace all labels of the series, and an empty map removes them all:

```java
commands.tsAlter("temperature:room1", TsAlterArgs.Builder.retention(Duration.ofDays(30)));

commands.tsAlter("temperature:room1", TsAlterArgs.Builder.labels(Collections.emptyMap())); // remove all labels
```

### Delete samples

`tsDel` deletes the samples in a range of timestamps and returns how many were deleted. The range is a `Range<Long>`:

```java
Long deleted = commands.tsDel("temperature:room1", Range.create(1000L, 2000L));   // 1000..2000, inclusive

commands.tsDel("temperature:room1", Range.unbounded());                           // all samples, sent as - +
commands.tsDel("temperature:room1", Range.from(Range.Boundary.excluding(1000L),
        Range.Boundary.including(2000L)));                                         // sent as 1001 2000
```

Timestamps are integer milliseconds, so an exclusive bound is sent as its inclusive neighbor: an exclusive lower bound `t` becomes `t + 1` and an exclusive upper bound `t` becomes `t - 1`. An exclusive lower bound at `Long.MAX_VALUE` or an exclusive upper bound at `Long.MIN_VALUE` has no neighbor and throws `IllegalArgumentException`. The same rules apply to every TimeSeries command that takes a `Range<Long>`.

To delete the whole series, delete its key with `del`.

### Inspect a series

`tsInfo` returns a `TsInfoValue` with the series' sample count, memory use, first and last timestamp, retention, chunk settings, duplicate policy, labels, source key and compaction rules:

```java
TsInfoValue<String> info = commands.tsInfo("temperature:room1");

long samples = info.getTotalSamples();
Map<String, String> labels = info.getLabels();
for (TsInfoValue.Rule<String> rule : info.getRules()) {
    System.out.println(rule.getDestinationKey() + " " + rule.getAggregation() + " " + rule.getBucketDuration());
}
```

`tsInfo(key, true)` sends `DEBUG` and also fills in the data chunks (`getChunks()`) and the key name (`getKeySelfName()`); without it, both are `null`.

RESP2 and RESP3 replies produce equal `TsInfoValue` instances. Fields that this Lettuce version does not model are available through `getRawInfo()`.

## Compaction rules

A compaction rule aggregates the samples of a source series into buckets and writes one sample per bucket to a destination series. Both series must exist before the rule is created:

```java
commands.tsCreate("{sensor1}:raw");
commands.tsCreate("{sensor1}:avg-1m");

commands.tsCreateRule("{sensor1}:raw", "{sensor1}:avg-1m", TsAggregationType.AVG, 60_000);

// buckets aligned to a timestamp instead of the epoch
commands.tsCreateRule("{sensor1}:raw", "{sensor1}:max-1h", TsAggregationType.MAX, 3_600_000, 1_800_000);

commands.tsDeleteRule("{sensor1}:raw", "{sensor1}:avg-1m");   // keeps both series
```

- The alignment timestamp must not be negative; a negative value throws `IllegalArgumentException` before the command is sent.
- A rule takes one aggregation. `countNaN` and `countAll` require Redis 8.6 or later. For an aggregation that this Lettuce version does not know yet, pass `TsAggregation.of("name")`.
- Only samples added after the rule is created are aggregated. Each compacted sample has the start of its bucket as timestamp, and a bucket with no source samples gets no compacted sample.
- A destination series cannot be the source or destination of another rule. Deleting the destination key also deletes the rule.
- `tsInfo` on the source lists its rules (`getRules()`, with destination, bucket duration, aggregation and alignment); `tsInfo` on the destination reports the source (`getSourceKey()`).

!!! INFO
    In Redis Cluster, the source and destination keys must hash to the same slot. Use a shared hash tag such as `{sensor1}`. For keys in different slots, Redis 8.4 and later reply `CROSSSLOT`. Redis 8.0 and 8.2 reply `TSDB: the key does not exist` (`tsCreateRule`) or `TSDB: compaction rule does not exist` (`tsDeleteRule`) instead, even though both keys exist. Lettuce passes the server's reply through.

## Writes

### Add a sample

`tsAdd` adds one sample and returns its timestamp. It creates the series if it does not exist; the series options in `TsAddArgs` apply only then:

```java
Long ts = commands.tsAdd("temperature:room1", 1700000000000L, 21.5);    // explicit timestamp in milliseconds
Long now = commands.tsAdd("temperature:room1", 21.7);                   // timestamp * : the server clock sets it

commands.tsAdd("temperature:room3", 1700000000000L, 19.0, TsAddArgs.Builder.retention(Duration.ofDays(7))
        .label("sensor", "temperature")
        .onDuplicate(TsDuplicatePolicyType.LAST));
```

!!! WARNING
    `tsAdd(key, value)` sends the timestamp `*`. Java converts a `long` to `double` without a cast, so `tsAdd(key, timestampMillis)` compiles and stores the timestamp as the **value**. To set the timestamp, use `tsAdd(key, timestamp, value)`.

`onDuplicate` overrides the duplicate policy of the series for this call only. It has no effect when the call creates the series.

### Ignore near-duplicates

With `ignore(maxTimeDiff, maxValDiff)` on the series, a new sample that is at most `maxTimeDiff` milliseconds after the newest sample and differs from its value by at most `maxValDiff` is not stored, and the command returns the newest existing timestamp. The server applies this only when the duplicate policy of the series is `LAST`; with any other policy the sample is stored:

```java
commands.tsCreate("power:meter1", TsCreateArgs.Builder.duplicatePolicy(TsDuplicatePolicyType.LAST).ignore(1000, 0.5));
commands.tsAdd("power:meter1", 1000, 10.0);   // 1000
commands.tsAdd("power:meter1", 1500, 10.2);   // 1000: ignored, within 1000 ms and 0.5
```

### Add several samples

`tsMAdd` adds samples to existing series; unlike `tsAdd`, it does not create series. Each sample is a `TsMAddValue`:

```java
List<Long> result = commands.tsMAdd(Arrays.asList(
        TsMAddValue.of("temperature:room1", 1700000001000L, 21.6),
        TsMAddValue.of("temperature:room2", 1700000001000L, 22.1),
        TsMAddValue.of("temperature:room1", 21.8)));                   // timestamp * : the server clock sets it
```

The server handles each sample on its own. A rejected sample (for example a duplicate under policy `BLOCK`, or a key that does not exist) gives a `null` element at its position, and the other samples are still added. The reactive API emits `Value.empty()` for a rejected sample, and the Kotlin API returns `List<Long?>`. To add a single sample with options, use `tsAdd`.

In Redis Cluster, `tsMAdd` with keys in several slots is sent as one `TS.MADD` per slot, in parallel, and the results come back in the order of the samples. The split is **not atomic** across slots: each slot's command is applied on its own. If one slot's command fails as a whole (for example a connection error), the call fails, although other slots may already have added their samples. Use hash tags (`{room}:temperature`) to keep related series in one slot.

### Increment and decrement

`tsIncrBy` and `tsDecrBy` change the value of the latest sample and create the series if it does not exist:

```java
commands.tsIncrBy("requests:api", 1);                                                      // server clock
commands.tsIncrBy("requests:api", 5, TsIncrByArgs.Builder.timestamp(1700000060000L));     // explicit timestamp
commands.tsDecrBy("stock:item42", 2, TsIncrByArgs.Builder.label("warehouse", "east"));
```

If the timestamp equals the newest timestamp of the series, the latest sample is updated in place; a higher timestamp adds a new sample with the changed value. A timestamp lower than the newest one is rejected by the server.
