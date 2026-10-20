/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api.reactive;

import java.util.List;

import io.lettuce.core.Range;
import io.lettuce.core.Value;
import io.lettuce.core.timeseries.TsAggregation;
import io.lettuce.core.timeseries.TsFilter;
import io.lettuce.core.timeseries.TsInfoValue;
import io.lettuce.core.timeseries.TsMAddValue;
import io.lettuce.core.timeseries.TsMGetValue;
import io.lettuce.core.timeseries.TsReadOffset;
import io.lettuce.core.timeseries.TsSample;
import io.lettuce.core.timeseries.TsSeries;
import io.lettuce.core.timeseries.arguments.TsAddArgs;
import io.lettuce.core.timeseries.arguments.TsAlterArgs;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.core.timeseries.arguments.TsIncrByArgs;
import io.lettuce.core.timeseries.arguments.TsMGetArgs;
import io.lettuce.core.timeseries.arguments.TsMRangeArgs;
import io.lettuce.core.timeseries.arguments.TsRangeArgs;
import io.lettuce.core.timeseries.arguments.TsReadArgs;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive executed commands for Redis TimeSeries.
 * <p>
 * Sample values are {@code double} and labels are {@code String}, so the value type {@code V} is not used by these commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @see <a href="https://redis.io/docs/latest/develop/data-types/timeseries/">Redis TimeSeries</a>
 * @since 8.0
 */
public interface RedisTimeSeriesReactiveCommands<K, V> {

    /**
     * Create a new time series with the server's default options.
     *
     * @param key the key, must not be {@code null}.
     * @return String simple-string-reply {@code OK} if the series was created.
     * @throws IllegalArgumentException if {@code key} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.create/">TS.CREATE</a>
     */
    Mono<String> tsCreate(K key);

    /**
     * Create a new time series with the given options. Options that are not set use the server's defaults.
     *
     * @param key the key, must not be {@code null}.
     * @param args the series options, must not be {@code null}.
     * @return String simple-string-reply {@code OK} if the series was created.
     * @throws IllegalArgumentException if {@code key} or {@code args} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.create/">TS.CREATE</a>
     */
    Mono<String> tsCreate(K key, TsCreateArgs args);

    /**
     * Update the options of an existing time series. Only the options set in {@code args} change. Labels set in {@code args}
     * replace all labels of the series; an empty label map removes all labels.
     *
     * @param key the key, must not be {@code null}.
     * @param args the options to change, must not be {@code null}.
     * @return String simple-string-reply {@code OK} if the series was updated.
     * @throws IllegalArgumentException if {@code key} or {@code args} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.alter/">TS.ALTER</a>
     */
    Mono<String> tsAlter(K key, TsAlterArgs args);

    /**
     * Delete all samples of a time series within a range of timestamps.
     * <p>
     * An inclusive bound sends that timestamp, and an unbounded side sends {@code -} (earliest) or {@code +} (latest). Because
     * timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor: an exclusive lower bound
     * {@code t} sends {@code t + 1}, an exclusive upper bound {@code t} sends {@code t - 1}.
     *
     * @param key the key, must not be {@code null}.
     * @param range the range of timestamps in milliseconds, must not be {@code null}.
     * @return Long integer-reply the number of deleted samples.
     * @throws IllegalArgumentException if {@code key} or {@code range} is {@code null}, or if {@code range} has an exclusive
     *         lower bound at {@link Long#MAX_VALUE} or an exclusive upper bound at {@link Long#MIN_VALUE}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.del/">TS.DEL</a>
     */
    Mono<Long> tsDel(K key, Range<Long> range);

    /**
     * Get information about a time series.
     *
     * @param key the key, must not be {@code null}.
     * @return TsInfoValue the information about the series; {@link TsInfoValue#getChunks()} and
     *         {@link TsInfoValue#getKeySelfName()} are {@code null}.
     * @throws IllegalArgumentException if {@code key} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.info/">TS.INFO</a>
     */
    Mono<TsInfoValue<K>> tsInfo(K key);

    /**
     * Get information about a time series, optionally including debug information. With {@code debug} set, the command sends
     * {@code DEBUG} and the result includes the data chunks ({@link TsInfoValue#getChunks()}) and the key name
     * ({@link TsInfoValue#getKeySelfName()}).
     *
     * @param key the key, must not be {@code null}.
     * @param debug {@code true} to request debug information.
     * @return TsInfoValue the information about the series.
     * @throws IllegalArgumentException if {@code key} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.info/">TS.INFO</a>
     */
    Mono<TsInfoValue<K>> tsInfo(K key, boolean debug);

    /**
     * Create a compaction rule that aggregates the samples of {@code sourceKey} into {@code destKey}. Both series must exist,
     * and {@code destKey} must not be the source or destination of another rule. Only samples added after the rule is created
     * are aggregated. Each compacted sample has the start of its bucket as timestamp; buckets start at multiples of
     * {@code bucketDuration} from the epoch.
     * <p>
     * Both keys must hash to the same slot, for example by sharing a hash tag such as {@code {sensor1}}. In Redis Cluster, keys
     * in different slots are rejected with {@code CROSSSLOT} on Redis 8.4 and later; Redis 8.0 and 8.2 reply
     * {@code TSDB: the key does not exist} instead, even though both keys exist.
     *
     * @param sourceKey the key of the series to aggregate, must not be {@code null}.
     * @param destKey the key of the series that receives the compacted samples, must not be {@code null}.
     * @param aggregation the aggregation, must not be {@code null}. {@code countNaN} and {@code countAll} require Redis 8.6 or
     *        later.
     * @param bucketDuration the duration of each bucket in milliseconds, must be positive.
     * @return String simple-string-reply {@code OK} if the rule was created.
     * @throws IllegalArgumentException if {@code sourceKey}, {@code destKey} or {@code aggregation} is {@code null}, or if
     *         {@code bucketDuration} is not positive.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.createrule/">TS.CREATERULE</a>
     */
    Mono<String> tsCreateRule(K sourceKey, K destKey, TsAggregation aggregation, long bucketDuration);

    /**
     * Create a compaction rule with aligned buckets that aggregates the samples of {@code sourceKey} into {@code destKey}. Like
     * {@link #tsCreateRule(Object, Object, TsAggregation, long)}, but a bucket starts exactly at {@code alignTimestamp}.
     * <p>
     * Both keys must hash to the same slot, for example by sharing a hash tag such as {@code {sensor1}}. In Redis Cluster, keys
     * in different slots are rejected with {@code CROSSSLOT} on Redis 8.4 and later; Redis 8.0 and 8.2 reply
     * {@code TSDB: the key does not exist} instead, even though both keys exist.
     *
     * @param sourceKey the key of the series to aggregate, must not be {@code null}.
     * @param destKey the key of the series that receives the compacted samples, must not be {@code null}.
     * @param aggregation the aggregation, must not be {@code null}. {@code countNaN} and {@code countAll} require Redis 8.6 or
     *        later.
     * @param bucketDuration the duration of each bucket in milliseconds, must be positive.
     * @param alignTimestamp the timestamp in milliseconds at which a bucket starts, must not be negative.
     * @return String simple-string-reply {@code OK} if the rule was created.
     * @throws IllegalArgumentException if {@code sourceKey}, {@code destKey} or {@code aggregation} is {@code null}, if
     *         {@code bucketDuration} is not positive, or if {@code alignTimestamp} is negative.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.createrule/">TS.CREATERULE</a>
     */
    Mono<String> tsCreateRule(K sourceKey, K destKey, TsAggregation aggregation, long bucketDuration, long alignTimestamp);

    /**
     * Delete the compaction rule from {@code sourceKey} to {@code destKey}. Both series are kept.
     * <p>
     * Both keys must hash to the same slot, for example by sharing a hash tag such as {@code {sensor1}}. In Redis Cluster, keys
     * in different slots are rejected with {@code CROSSSLOT} on Redis 8.4 and later; Redis 8.0 and 8.2 reply
     * {@code TSDB: compaction rule does not exist} instead, even though both keys exist.
     *
     * @param sourceKey the key of the source series, must not be {@code null}.
     * @param destKey the key of the destination series, must not be {@code null}.
     * @return String simple-string-reply {@code OK} if the rule was deleted.
     * @throws IllegalArgumentException if {@code sourceKey} or {@code destKey} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.deleterule/">TS.DELETERULE</a>
     */
    Mono<String> tsDeleteRule(K sourceKey, K destKey);

    /**
     * Add a sample with the current server time to a time series, creating the series if it does not exist.
     * <p>
     * The sample gets timestamp {@code *}, so the server clock sets it. Java converts a {@code long} to {@code double} without
     * a cast, so {@code tsAdd(key, timestampMillis)} compiles and stores the timestamp as the value. To set the timestamp, use
     * {@link #tsAdd(Object, long, double)}.
     *
     * @param key the key, must not be {@code null}.
     * @param value the value.
     * @return Long integer-reply the timestamp of the added sample, or the highest timestamp of the series if the sample was
     *         ignored as a duplicate ({@code IGNORE}).
     * @throws IllegalArgumentException if {@code key} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.add/">TS.ADD</a>
     */
    Mono<Long> tsAdd(K key, double value);

    /**
     * Add a sample with the current server time to a time series, with options.
     * <p>
     * The sample gets timestamp {@code *}, so the server clock sets it. Java converts a {@code long} to {@code double} without
     * a cast, so {@code tsAdd(key, timestampMillis)} compiles and stores the timestamp as the value. To set the timestamp, use
     * {@link #tsAdd(Object, long, double)}.
     * <p>
     * If the series does not exist, it is created. The series options in {@code args} apply only when the series is created;
     * for an existing series the server ignores them.
     *
     * @param key the key, must not be {@code null}.
     * @param value the value.
     * @param args the options, must not be {@code null}.
     * @return Long integer-reply the timestamp of the added sample, or the highest timestamp of the series if the sample was
     *         ignored as a duplicate ({@code IGNORE}).
     * @throws IllegalArgumentException if {@code key} or {@code args} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.add/">TS.ADD</a>
     */
    Mono<Long> tsAdd(K key, double value, TsAddArgs args);

    /**
     * Add a sample to a time series, creating the series if it does not exist.
     *
     * @param key the key, must not be {@code null}.
     * @param timestamp the timestamp in milliseconds.
     * @param value the value. NaN requires Redis 8.6 or later.
     * @return Long integer-reply the timestamp of the added sample, or the highest timestamp of the series if the sample was
     *         ignored as a duplicate ({@code IGNORE}).
     * @throws IllegalArgumentException if {@code key} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.add/">TS.ADD</a>
     */
    Mono<Long> tsAdd(K key, long timestamp, double value);

    /**
     * Add a sample to a time series, with options.
     * <p>
     * If the series does not exist, it is created. The series options in {@code args} apply only when the series is created;
     * for an existing series the server ignores them.
     *
     * @param key the key, must not be {@code null}.
     * @param timestamp the timestamp in milliseconds.
     * @param value the value. NaN requires Redis 8.6 or later.
     * @param args the options, must not be {@code null}.
     * @return Long integer-reply the timestamp of the added sample, or the highest timestamp of the series if the sample was
     *         ignored as a duplicate ({@code IGNORE}).
     * @throws IllegalArgumentException if {@code key} or {@code args} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.add/">TS.ADD</a>
     */
    Mono<Long> tsAdd(K key, long timestamp, double value, TsAddArgs args);

    /**
     * Add samples to one or more existing time series. Unlike {@link #tsAdd(Object, long, double)}, the command does not create
     * series.
     * <p>
     * The server handles each sample on its own. A rejected sample, for example a duplicate under policy {@code BLOCK} or a
     * missing key, gives {@link Value#empty()} at its position, and the other samples are still added. To add a single sample
     * with options, use {@link #tsAdd(Object, long, double, TsAddArgs)}.
     * <p>
     * With the Redis Cluster API ({@code RedisAdvancedClusterCommands} and its async and reactive variants), samples whose keys
     * hash to different slots are sent as one command per slot. The split is not atomic across slots: each slot's command is
     * applied on its own. The result keeps the order of {@code samples}.
     *
     * @param samples the samples, must not be {@code null} or empty and must not contain {@code null} elements.
     * @return Long one element per sample, in the order of {@code samples}: the timestamp of the added sample, the highest
     *         timestamp of the series if the sample was ignored as a duplicate, or {@link Value#empty()} if the server rejected
     *         the sample.
     * @throws IllegalArgumentException if {@code samples} is {@code null}, empty or contains {@code null} elements.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.madd/">TS.MADD</a>
     */
    Flux<Value<Long>> tsMAdd(List<TsMAddValue<K>> samples);

    /**
     * Increase the value of the latest sample of a time series, creating the series if it does not exist. The sample gets the
     * current server time; if that equals the highest timestamp of the series, the latest sample is updated in place, otherwise
     * a new sample with the increased value is added.
     *
     * @param key the key, must not be {@code null}.
     * @param addend the amount to add, must not be NaN.
     * @return Long integer-reply the timestamp of the updated or added sample, or the highest timestamp of the series if the
     *         sample was ignored as a duplicate ({@code IGNORE}).
     * @throws IllegalArgumentException if {@code key} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.incrby/">TS.INCRBY</a>
     */
    Mono<Long> tsIncrBy(K key, double addend);

    /**
     * Increase the value of the latest sample of a time series, with options. With {@link TsIncrByArgs#timestamp(long)}, the
     * timestamp must be equal to or higher than the highest timestamp of the series; an equal timestamp updates the latest
     * sample in place.
     * <p>
     * If the series does not exist, it is created. The series options in {@code args} apply only when the series is created;
     * for an existing series the server ignores them.
     *
     * @param key the key, must not be {@code null}.
     * @param addend the amount to add, must not be NaN.
     * @param args the options, must not be {@code null}.
     * @return Long integer-reply the timestamp of the updated or added sample, or the highest timestamp of the series if the
     *         sample was ignored as a duplicate ({@code IGNORE}).
     * @throws IllegalArgumentException if {@code key} or {@code args} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.incrby/">TS.INCRBY</a>
     */
    Mono<Long> tsIncrBy(K key, double addend, TsIncrByArgs args);

    /**
     * Decrease the value of the latest sample of a time series, creating the series if it does not exist. The sample gets the
     * current server time; if that equals the highest timestamp of the series, the latest sample is updated in place, otherwise
     * a new sample with the decreased value is added.
     *
     * @param key the key, must not be {@code null}.
     * @param subtrahend the amount to subtract, must not be NaN.
     * @return Long integer-reply the timestamp of the updated or added sample, or the highest timestamp of the series if the
     *         sample was ignored as a duplicate ({@code IGNORE}).
     * @throws IllegalArgumentException if {@code key} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.decrby/">TS.DECRBY</a>
     */
    Mono<Long> tsDecrBy(K key, double subtrahend);

    /**
     * Decrease the value of the latest sample of a time series, with options. With {@link TsIncrByArgs#timestamp(long)}, the
     * timestamp must be equal to or higher than the highest timestamp of the series; an equal timestamp updates the latest
     * sample in place.
     * <p>
     * If the series does not exist, it is created. The series options in {@code args} apply only when the series is created;
     * for an existing series the server ignores them.
     *
     * @param key the key, must not be {@code null}.
     * @param subtrahend the amount to subtract, must not be NaN.
     * @param args the options, must not be {@code null}.
     * @return Long integer-reply the timestamp of the updated or added sample, or the highest timestamp of the series if the
     *         sample was ignored as a duplicate ({@code IGNORE}).
     * @throws IllegalArgumentException if {@code key} or {@code args} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.decrby/">TS.DECRBY</a>
     */
    Mono<Long> tsDecrBy(K key, double subtrahend, TsIncrByArgs args);

    /**
     * Get the latest sample of a time series. For a compaction, the latest bucket is reported only once it is complete; use
     * {@link #tsGet(Object, boolean)} with {@code latest} set to also get the open bucket.
     *
     * @param key the key, must not be {@code null}.
     * @return TsSample the latest sample, or an empty {@code Mono} if the series has no samples.
     * @throws IllegalArgumentException if {@code key} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.get/">TS.GET</a>
     */
    Mono<TsSample> tsGet(K key);

    /**
     * Get the latest sample of a time series. With {@code latest} set, the command sends {@code LATEST}: for a compaction, the
     * latest, possibly partial, bucket is reported. The server ignores it for a series that is not a compaction.
     *
     * @param key the key, must not be {@code null}.
     * @param latest {@code true} to report the latest, possibly partial, bucket of a compaction.
     * @return TsSample the latest sample, or an empty {@code Mono} if the series has no samples.
     * @throws IllegalArgumentException if {@code key} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.get/">TS.GET</a>
     */
    Mono<TsSample> tsGet(K key, boolean latest);

    /**
     * Get the samples of a time series within a range of timestamps, in ascending order.
     * <p>
     * An inclusive bound sends that timestamp, and an unbounded side sends {@code -} (earliest) or {@code +} (latest). Because
     * timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor: an exclusive lower bound
     * {@code t} sends {@code t + 1}, an exclusive upper bound {@code t} sends {@code t - 1}.
     *
     * @param key the key, must not be {@code null}.
     * @param range the range of timestamps in milliseconds, must not be {@code null}.
     * @return List&lt;TsSample&gt; the samples in ascending order; empty if the range has no samples.
     * @throws IllegalArgumentException if {@code key} or {@code range} is {@code null}, or if {@code range} has an exclusive
     *         lower bound at {@link Long#MAX_VALUE} or an exclusive upper bound at {@link Long#MIN_VALUE}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.range/">TS.RANGE</a>
     */
    Flux<TsSample> tsRange(K key, Range<Long> range);

    /**
     * Get the samples of a time series within a range of timestamps, in ascending order, with filters, a limit or an
     * aggregation. With an aggregation, each sample is one bucket and has one value per aggregation, in the order of the
     * request. Several aggregations require Redis 8.8 or later.
     * <p>
     * The range bounds follow the same rules as in {@link #tsRange(Object, Range)}. For a compaction, the last reported bucket
     * may include samples newer than the upper bound, because the bound limits only the bucket start.
     *
     * @param key the key, must not be {@code null}.
     * @param range the range of timestamps in milliseconds, must not be {@code null}.
     * @param args the options, must not be {@code null}.
     * @return List&lt;TsSample&gt; the samples or buckets in ascending order; empty if the range has no samples.
     * @throws IllegalArgumentException if {@code key}, {@code range} or {@code args} is {@code null}, if {@code range} has an
     *         exclusive lower bound at {@link Long#MAX_VALUE} or an exclusive upper bound at {@link Long#MIN_VALUE}, or if
     *         {@code args} sets {@code ALIGN}, {@code BUCKETTIMESTAMP} or {@code EMPTY} without an aggregation.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.range/">TS.RANGE</a>
     */
    Flux<TsSample> tsRange(K key, Range<Long> range, TsRangeArgs args);

    /**
     * Get the samples of a time series within a range of timestamps, in descending order.
     * <p>
     * An inclusive bound sends that timestamp, and an unbounded side sends {@code -} (earliest) or {@code +} (latest). Because
     * timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor: an exclusive lower bound
     * {@code t} sends {@code t + 1}, an exclusive upper bound {@code t} sends {@code t - 1}.
     *
     * @param key the key, must not be {@code null}.
     * @param range the range of timestamps in milliseconds, must not be {@code null}.
     * @return List&lt;TsSample&gt; the samples in descending order; empty if the range has no samples.
     * @throws IllegalArgumentException if {@code key} or {@code range} is {@code null}, or if {@code range} has an exclusive
     *         lower bound at {@link Long#MAX_VALUE} or an exclusive upper bound at {@link Long#MIN_VALUE}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.revrange/">TS.REVRANGE</a>
     */
    Flux<TsSample> tsRevRange(K key, Range<Long> range);

    /**
     * Get the samples of a time series within a range of timestamps, in descending order, with filters, a limit or an
     * aggregation. With an aggregation, each sample is one bucket and has one value per aggregation, in the order of the
     * request. Several aggregations require Redis 8.8 or later.
     * <p>
     * The range bounds follow the same rules as in {@link #tsRevRange(Object, Range)}. For a compaction, the last reported
     * bucket may include samples newer than the upper bound, because the bound limits only the bucket start.
     *
     * @param key the key, must not be {@code null}.
     * @param range the range of timestamps in milliseconds, must not be {@code null}.
     * @param args the options, must not be {@code null}.
     * @return List&lt;TsSample&gt; the samples or buckets in descending order; empty if the range has no samples.
     * @throws IllegalArgumentException if {@code key}, {@code range} or {@code args} is {@code null}, if {@code range} has an
     *         exclusive lower bound at {@link Long#MAX_VALUE} or an exclusive upper bound at {@link Long#MIN_VALUE}, or if
     *         {@code args} sets {@code ALIGN}, {@code BUCKETTIMESTAMP} or {@code EMPTY} without an aggregation.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.revrange/">TS.REVRANGE</a>
     */
    Flux<TsSample> tsRevRange(K key, Range<Long> range, TsRangeArgs args);

    /**
     * Read the samples of a time series from {@code offset} on, in ascending order, without blocking. Requires Redis 8.10 or
     * later.
     * <p>
     * Use {@link TsReadOffset#latest()} or {@link TsReadOffset#newSamples()} only on the first call, and continue with
     * {@code TsReadOffset.from(lastTimestamp + 1)}. Without {@code BLOCK}, {@link TsReadOffset#newSamples()} always returns an
     * empty list. A key that does not exist returns an empty list.
     *
     * @param key the key, must not be {@code null}.
     * @param offset the inclusive lower bound, must not be {@code null}.
     * @return List&lt;TsSample&gt; the samples in ascending order; empty if there are none.
     * @throws IllegalArgumentException if {@code key} or {@code offset} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.read/">TS.READ</a>
     */
    Flux<TsSample> tsRead(K key, TsReadOffset offset);

    /**
     * Read the samples of a time series from {@code offset} on, in ascending order, optionally waiting for new samples.
     * Requires Redis 8.10 or later.
     * <p>
     * With {@link TsReadArgs#block(long, long)}, the command waits until enough samples exist, the block time ends, or the key
     * is removed. A timeout returns the samples found so far. Deleting or expiring the key (DEL, UNLINK, FLUSHDB, FLUSHALL,
     * expiry, eviction) wakes every waiter with an empty list, {@code BLOCK 0} included, so an empty list means either a
     * timeout or a removed key. {@code BLOCK 0} waits without a time limit.
     * <p>
     * While it waits, the command occupies its connection and stalls every other command sent on that connection. Use a
     * dedicated connection whose command timeout is longer than the block time; Lettuce does not extend the timeout. In Redis
     * Cluster, a cluster connection shares one connection per node, so a blocked TS.READ stalls every command that cluster
     * connection sends to that node; use a dedicated {@code StatefulRedisClusterConnection}. With {@code ReadFrom} set to a
     * replica, TS.READ runs on a replica, which wakes only after the write has replicated to it; read from the primary for
     * low-latency tailing. The server rejects {@code BLOCK} inside MULTI and Lua scripts.
     * <p>
     * Use {@link TsReadOffset#latest()} or {@link TsReadOffset#newSamples()} only on the first call, and continue with
     * {@code TsReadOffset.from(lastTimestamp + 1)}.
     *
     * @param key the key, must not be {@code null}.
     * @param offset the inclusive lower bound, must not be {@code null}.
     * @param args the options, must not be {@code null}.
     * @return List&lt;TsSample&gt; the samples in ascending order; empty on a timeout without samples or when the key was
     *         removed.
     * @throws IllegalArgumentException if {@code key}, {@code offset} or {@code args} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.read/">TS.READ</a>
     */
    Flux<TsSample> tsRead(K key, TsReadOffset offset, TsReadArgs args);

    /**
     * Get the latest sample of every time series that matches the filters. No labels are returned.
     * <p>
     * The server requires at least one filter that matches a value ({@link TsFilter#equal(String, String)} or
     * {@link TsFilter#in(String, String...)}).
     * <p>
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * {@code TIMESERIES.REFRESHCLUSTER} has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param filter the filter, must not be {@code null}.
     * @return List&lt;TsMGetValue&gt; one entry per matching series; empty if no series matches.
     * @throws IllegalArgumentException if {@code filter} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mget/">TS.MGET</a>
     */
    Flux<TsMGetValue<K>> tsMGet(TsFilter filter);

    /**
     * Get the latest sample of every time series that matches the filters. No labels are returned.
     * <p>
     * The server requires at least one filter that matches a value ({@link TsFilter#equal(String, String)} or
     * {@link TsFilter#in(String, String...)}).
     * <p>
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * {@code TIMESERIES.REFRESHCLUSTER} has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param filters the filters, combined with AND, must not be {@code null} or empty and must not contain {@code null}
     *        elements.
     * @return List&lt;TsMGetValue&gt; one entry per matching series; empty if no series matches.
     * @throws IllegalArgumentException if {@code filters} is {@code null}, empty or contains {@code null} elements.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mget/">TS.MGET</a>
     */
    Flux<TsMGetValue<K>> tsMGet(TsFilter... filters);

    /**
     * Get the latest sample of every time series that matches the filters, with options such as labels.
     * <p>
     * The server requires at least one filter that matches a value ({@link TsFilter#equal(String, String)} or
     * {@link TsFilter#in(String, String...)}).
     * <p>
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * {@code TIMESERIES.REFRESHCLUSTER} has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param args the options, must not be {@code null}.
     * @param filter the filter, must not be {@code null}.
     * @return List&lt;TsMGetValue&gt; one entry per matching series; empty if no series matches.
     * @throws IllegalArgumentException if {@code args} is {@code null}, or {@code filter} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mget/">TS.MGET</a>
     */
    Flux<TsMGetValue<K>> tsMGet(TsMGetArgs args, TsFilter filter);

    /**
     * Get the latest sample of every time series that matches the filters, with options such as labels.
     * <p>
     * The server requires at least one filter that matches a value ({@link TsFilter#equal(String, String)} or
     * {@link TsFilter#in(String, String...)}).
     * <p>
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * {@code TIMESERIES.REFRESHCLUSTER} has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param args the options, must not be {@code null}.
     * @param filters the filters, combined with AND, must not be {@code null} or empty and must not contain {@code null}
     *        elements.
     * @return List&lt;TsMGetValue&gt; one entry per matching series; empty if no series matches.
     * @throws IllegalArgumentException if {@code args} is {@code null}, or {@code filters} is {@code null}, empty or contains
     *         {@code null} elements.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mget/">TS.MGET</a>
     */
    Flux<TsMGetValue<K>> tsMGet(TsMGetArgs args, TsFilter... filters);

    /**
     * Get the keys of all time series that match the filters. The result includes series the user cannot read.
     * <p>
     * The server requires at least one filter that matches a value ({@link TsFilter#equal(String, String)} or
     * {@link TsFilter#in(String, String...)}).
     * <p>
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * {@code TIMESERIES.REFRESHCLUSTER} has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param filter the filter, must not be {@code null}.
     * @return List&lt;K&gt; the keys of the matching series; empty if no series matches.
     * @throws IllegalArgumentException if {@code filter} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.queryindex/">TS.QUERYINDEX</a>
     */
    Flux<K> tsQueryIndex(TsFilter filter);

    /**
     * Get the keys of all time series that match the filters. The result includes series the user cannot read.
     * <p>
     * The server requires at least one filter that matches a value ({@link TsFilter#equal(String, String)} or
     * {@link TsFilter#in(String, String...)}).
     * <p>
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * {@code TIMESERIES.REFRESHCLUSTER} has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param filters the filters, combined with AND, must not be {@code null} or empty and must not contain {@code null}
     *        elements.
     * @return List&lt;K&gt; the keys of the matching series; empty if no series matches.
     * @throws IllegalArgumentException if {@code filters} is {@code null}, empty or contains {@code null} elements.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.queryindex/">TS.QUERYINDEX</a>
     */
    Flux<K> tsQueryIndex(TsFilter... filters);

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in ascending order. No labels
     * are returned.
     * <p>
     * An inclusive bound sends that timestamp, and an unbounded side sends {@code -} (earliest) or {@code +} (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound {@code t} sends {@code t + 1}, an exclusive upper bound {@code t} sends {@code t - 1}.
     * <p>
     * The server requires at least one filter that matches a value ({@link TsFilter#equal(String, String)} or
     * {@link TsFilter#in(String, String...)}).
     * <p>
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * {@code TIMESERIES.REFRESHCLUSTER} has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     * <p>
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be {@code null}.
     * @param filter the filter, must not be {@code null}.
     * @return List&lt;TsSeries&gt; one entry per matching series, or per group with {@code GROUPBY}; a series without samples
     *         in the range has an empty sample list unless {@code EXCLUDEEMPTY} is set.
     * @throws IllegalArgumentException if {@code range} is {@code null}, {@code filter} is {@code null}, or if {@code range}
     *         has an exclusive lower bound at {@link Long#MAX_VALUE} or an exclusive upper bound at {@link Long#MIN_VALUE}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrange/">TS.MRANGE</a>
     */
    Flux<TsSeries<K>> tsMRange(Range<Long> range, TsFilter filter);

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in ascending order. No labels
     * are returned.
     * <p>
     * An inclusive bound sends that timestamp, and an unbounded side sends {@code -} (earliest) or {@code +} (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound {@code t} sends {@code t + 1}, an exclusive upper bound {@code t} sends {@code t - 1}.
     * <p>
     * The server requires at least one filter that matches a value ({@link TsFilter#equal(String, String)} or
     * {@link TsFilter#in(String, String...)}).
     * <p>
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * {@code TIMESERIES.REFRESHCLUSTER} has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     * <p>
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be {@code null}.
     * @param filters the filters, combined with AND, must not be {@code null} or empty and must not contain {@code null}
     *        elements.
     * @return List&lt;TsSeries&gt; one entry per matching series, or per group with {@code GROUPBY}; a series without samples
     *         in the range has an empty sample list unless {@code EXCLUDEEMPTY} is set.
     * @throws IllegalArgumentException if {@code range} is {@code null}, {@code filters} is {@code null}, empty or contains
     *         {@code null} elements, or if {@code range} has an exclusive lower bound at {@link Long#MAX_VALUE} or an exclusive
     *         upper bound at {@link Long#MIN_VALUE}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrange/">TS.MRANGE</a>
     */
    Flux<TsSeries<K>> tsMRange(Range<Long> range, TsFilter... filters);

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in ascending order, with
     * options such as labels, aggregation and {@code GROUPBY}.
     * <p>
     * An inclusive bound sends that timestamp, and an unbounded side sends {@code -} (earliest) or {@code +} (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound {@code t} sends {@code t + 1}, an exclusive upper bound {@code t} sends {@code t - 1}.
     * <p>
     * With an aggregation, each sample is one bucket with one value per aggregation; several aggregations require Redis 8.8 or
     * later and cannot be combined with {@code GROUPBY}. With {@code GROUPBY}, each entry is one group of series combined by
     * the reducer. {@code EXCLUDEEMPTY} requires Redis 8.10 or later.
     * <p>
     * The server requires at least one filter that matches a value ({@link TsFilter#equal(String, String)} or
     * {@link TsFilter#in(String, String...)}).
     * <p>
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * {@code TIMESERIES.REFRESHCLUSTER} has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     * <p>
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be {@code null}.
     * @param args the options, must not be {@code null}.
     * @param filter the filter, must not be {@code null}.
     * @return List&lt;TsSeries&gt; one entry per matching series, or per group with {@code GROUPBY}; a series without samples
     *         in the range has an empty sample list unless {@code EXCLUDEEMPTY} is set.
     * @throws IllegalArgumentException if {@code range} or {@code args} is {@code null}, {@code filter} is {@code null}, if
     *         {@code range} has an exclusive lower bound at {@link Long#MAX_VALUE} or an exclusive upper bound at
     *         {@link Long#MIN_VALUE}, or if {@code args} has conflicting options.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrange/">TS.MRANGE</a>
     */
    Flux<TsSeries<K>> tsMRange(Range<Long> range, TsMRangeArgs args, TsFilter filter);

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in ascending order, with
     * options such as labels, aggregation and {@code GROUPBY}.
     * <p>
     * An inclusive bound sends that timestamp, and an unbounded side sends {@code -} (earliest) or {@code +} (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound {@code t} sends {@code t + 1}, an exclusive upper bound {@code t} sends {@code t - 1}.
     * <p>
     * With an aggregation, each sample is one bucket with one value per aggregation; several aggregations require Redis 8.8 or
     * later and cannot be combined with {@code GROUPBY}. With {@code GROUPBY}, each entry is one group of series combined by
     * the reducer. {@code EXCLUDEEMPTY} requires Redis 8.10 or later.
     * <p>
     * The server requires at least one filter that matches a value ({@link TsFilter#equal(String, String)} or
     * {@link TsFilter#in(String, String...)}).
     * <p>
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * {@code TIMESERIES.REFRESHCLUSTER} has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     * <p>
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be {@code null}.
     * @param args the options, must not be {@code null}.
     * @param filters the filters, combined with AND, must not be {@code null} or empty and must not contain {@code null}
     *        elements.
     * @return List&lt;TsSeries&gt; one entry per matching series, or per group with {@code GROUPBY}; a series without samples
     *         in the range has an empty sample list unless {@code EXCLUDEEMPTY} is set.
     * @throws IllegalArgumentException if {@code range} or {@code args} is {@code null}, {@code filters} is {@code null}, empty
     *         or contains {@code null} elements, if {@code range} has an exclusive lower bound at {@link Long#MAX_VALUE} or an
     *         exclusive upper bound at {@link Long#MIN_VALUE}, or if {@code args} has conflicting options.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrange/">TS.MRANGE</a>
     */
    Flux<TsSeries<K>> tsMRange(Range<Long> range, TsMRangeArgs args, TsFilter... filters);

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in descending order. No
     * labels are returned.
     * <p>
     * An inclusive bound sends that timestamp, and an unbounded side sends {@code -} (earliest) or {@code +} (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound {@code t} sends {@code t + 1}, an exclusive upper bound {@code t} sends {@code t - 1}.
     * <p>
     * The server requires at least one filter that matches a value ({@link TsFilter#equal(String, String)} or
     * {@link TsFilter#in(String, String...)}).
     * <p>
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * {@code TIMESERIES.REFRESHCLUSTER} has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     * <p>
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be {@code null}.
     * @param filter the filter, must not be {@code null}.
     * @return List&lt;TsSeries&gt; one entry per matching series, or per group with {@code GROUPBY}; a series without samples
     *         in the range has an empty sample list unless {@code EXCLUDEEMPTY} is set.
     * @throws IllegalArgumentException if {@code range} is {@code null}, {@code filter} is {@code null}, or if {@code range}
     *         has an exclusive lower bound at {@link Long#MAX_VALUE} or an exclusive upper bound at {@link Long#MIN_VALUE}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrevrange/">TS.MREVRANGE</a>
     */
    Flux<TsSeries<K>> tsMRevRange(Range<Long> range, TsFilter filter);

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in descending order. No
     * labels are returned.
     * <p>
     * An inclusive bound sends that timestamp, and an unbounded side sends {@code -} (earliest) or {@code +} (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound {@code t} sends {@code t + 1}, an exclusive upper bound {@code t} sends {@code t - 1}.
     * <p>
     * The server requires at least one filter that matches a value ({@link TsFilter#equal(String, String)} or
     * {@link TsFilter#in(String, String...)}).
     * <p>
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * {@code TIMESERIES.REFRESHCLUSTER} has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     * <p>
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be {@code null}.
     * @param filters the filters, combined with AND, must not be {@code null} or empty and must not contain {@code null}
     *        elements.
     * @return List&lt;TsSeries&gt; one entry per matching series, or per group with {@code GROUPBY}; a series without samples
     *         in the range has an empty sample list unless {@code EXCLUDEEMPTY} is set.
     * @throws IllegalArgumentException if {@code range} is {@code null}, {@code filters} is {@code null}, empty or contains
     *         {@code null} elements, or if {@code range} has an exclusive lower bound at {@link Long#MAX_VALUE} or an exclusive
     *         upper bound at {@link Long#MIN_VALUE}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrevrange/">TS.MREVRANGE</a>
     */
    Flux<TsSeries<K>> tsMRevRange(Range<Long> range, TsFilter... filters);

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in descending order, with
     * options such as labels, aggregation and {@code GROUPBY}.
     * <p>
     * An inclusive bound sends that timestamp, and an unbounded side sends {@code -} (earliest) or {@code +} (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound {@code t} sends {@code t + 1}, an exclusive upper bound {@code t} sends {@code t - 1}.
     * <p>
     * With an aggregation, each sample is one bucket with one value per aggregation; several aggregations require Redis 8.8 or
     * later and cannot be combined with {@code GROUPBY}. With {@code GROUPBY}, each entry is one group of series combined by
     * the reducer. {@code EXCLUDEEMPTY} requires Redis 8.10 or later.
     * <p>
     * The server requires at least one filter that matches a value ({@link TsFilter#equal(String, String)} or
     * {@link TsFilter#in(String, String...)}).
     * <p>
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * {@code TIMESERIES.REFRESHCLUSTER} has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     * <p>
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be {@code null}.
     * @param args the options, must not be {@code null}.
     * @param filter the filter, must not be {@code null}.
     * @return List&lt;TsSeries&gt; one entry per matching series, or per group with {@code GROUPBY}; a series without samples
     *         in the range has an empty sample list unless {@code EXCLUDEEMPTY} is set.
     * @throws IllegalArgumentException if {@code range} or {@code args} is {@code null}, {@code filter} is {@code null}, if
     *         {@code range} has an exclusive lower bound at {@link Long#MAX_VALUE} or an exclusive upper bound at
     *         {@link Long#MIN_VALUE}, or if {@code args} has conflicting options.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrevrange/">TS.MREVRANGE</a>
     */
    Flux<TsSeries<K>> tsMRevRange(Range<Long> range, TsMRangeArgs args, TsFilter filter);

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in descending order, with
     * options such as labels, aggregation and {@code GROUPBY}.
     * <p>
     * An inclusive bound sends that timestamp, and an unbounded side sends {@code -} (earliest) or {@code +} (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound {@code t} sends {@code t + 1}, an exclusive upper bound {@code t} sends {@code t - 1}.
     * <p>
     * With an aggregation, each sample is one bucket with one value per aggregation; several aggregations require Redis 8.8 or
     * later and cannot be combined with {@code GROUPBY}. With {@code GROUPBY}, each entry is one group of series combined by
     * the reducer. {@code EXCLUDEEMPTY} requires Redis 8.10 or later.
     * <p>
     * The server requires at least one filter that matches a value ({@link TsFilter#equal(String, String)} or
     * {@link TsFilter#in(String, String...)}).
     * <p>
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * {@code TIMESERIES.REFRESHCLUSTER} has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     * <p>
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be {@code null}.
     * @param args the options, must not be {@code null}.
     * @param filters the filters, combined with AND, must not be {@code null} or empty and must not contain {@code null}
     *        elements.
     * @return List&lt;TsSeries&gt; one entry per matching series, or per group with {@code GROUPBY}; a series without samples
     *         in the range has an empty sample list unless {@code EXCLUDEEMPTY} is set.
     * @throws IllegalArgumentException if {@code range} or {@code args} is {@code null}, {@code filters} is {@code null}, empty
     *         or contains {@code null} elements, if {@code range} has an exclusive lower bound at {@link Long#MAX_VALUE} or an
     *         exclusive upper bound at {@link Long#MIN_VALUE}, or if {@code args} has conflicting options.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrevrange/">TS.MREVRANGE</a>
     */
    Flux<TsSeries<K>> tsMRevRange(Range<Long> range, TsMRangeArgs args, TsFilter... filters);

}
