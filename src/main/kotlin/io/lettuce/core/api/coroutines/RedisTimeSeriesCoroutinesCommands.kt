/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */

package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.Range
import io.lettuce.core.timeseries.TsAggregation
import io.lettuce.core.timeseries.TsFilter
import io.lettuce.core.timeseries.TsInfoValue
import io.lettuce.core.timeseries.TsMAddValue
import io.lettuce.core.timeseries.TsMGetValue
import io.lettuce.core.timeseries.TsReadOffset
import io.lettuce.core.timeseries.TsSample
import io.lettuce.core.timeseries.TsSeries
import io.lettuce.core.timeseries.arguments.TsAddArgs
import io.lettuce.core.timeseries.arguments.TsAlterArgs
import io.lettuce.core.timeseries.arguments.TsCreateArgs
import io.lettuce.core.timeseries.arguments.TsIncrByArgs
import io.lettuce.core.timeseries.arguments.TsMGetArgs
import io.lettuce.core.timeseries.arguments.TsMRangeArgs
import io.lettuce.core.timeseries.arguments.TsRangeArgs
import io.lettuce.core.timeseries.arguments.TsReadArgs

/**
 * Coroutine executed commands for Redis TimeSeries.
 *
 * Sample values are `double` and labels are `String`, so the value type `V` is not used by these commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @see <a href="https://redis.io/docs/latest/develop/data-types/timeseries/">Redis TimeSeries</a>
 * @since 8.0
 */
@ExperimentalLettuceCoroutinesApi
interface RedisTimeSeriesCoroutinesCommands<K : Any, V : Any> {

    /**
     * Create a new time series with the server's default options.
     *
     * @param key the key, must not be `null`.
     * @return String simple-string-reply `OK` if the series was created.
     * @throws IllegalArgumentException if `key` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.create/">TS.CREATE</a>
     */
    suspend fun tsCreate(key: K): String?

    /**
     * Create a new time series with the given options. Options that are not set use the server's defaults.
     *
     * @param key the key, must not be `null`.
     * @param args the series options, must not be `null`.
     * @return String simple-string-reply `OK` if the series was created.
     * @throws IllegalArgumentException if `key` or `args` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.create/">TS.CREATE</a>
     */
    suspend fun tsCreate(key: K, args: TsCreateArgs): String?

    /**
     * Update the options of an existing time series. Only the options set in `args` change. Labels set in `args`
     * replace all labels of the series; an empty label map removes all labels.
     *
     * @param key the key, must not be `null`.
     * @param args the options to change, must not be `null`.
     * @return String simple-string-reply `OK` if the series was updated.
     * @throws IllegalArgumentException if `key` or `args` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.alter/">TS.ALTER</a>
     */
    suspend fun tsAlter(key: K, args: TsAlterArgs): String?

    /**
     * Delete all samples of a time series within a range of timestamps.
     *
     * An inclusive bound sends that timestamp, and an unbounded side sends `-` (earliest) or `+` (latest). Because
     * timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor: an exclusive lower bound
     * `t` sends `t + 1`, an exclusive upper bound `t` sends `t - 1`.
     *
     * @param key the key, must not be `null`.
     * @param range the range of timestamps in milliseconds, must not be `null`.
     * @return Long integer-reply the number of deleted samples.
     * @throws IllegalArgumentException if `key` or `range` is `null`, or if `range` has an exclusive
     *         lower bound at [Long.MAX_VALUE] or an exclusive upper bound at [Long.MIN_VALUE].
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.del/">TS.DEL</a>
     */
    suspend fun tsDel(key: K, range: Range<Long>): Long?

    /**
     * Get information about a time series.
     *
     * @param key the key, must not be `null`.
     * @return TsInfoValue the information about the series; [TsInfoValue.getChunks] and
     *         [TsInfoValue.getKeySelfName] are `null`.
     * @throws IllegalArgumentException if `key` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.info/">TS.INFO</a>
     */
    suspend fun tsInfo(key: K): TsInfoValue<K>?

    /**
     * Get information about a time series, optionally including debug information. With `debug` set, the command sends
     * `DEBUG` and the result includes the data chunks ([TsInfoValue.getChunks]) and the key name
     * ([TsInfoValue.getKeySelfName]).
     *
     * @param key the key, must not be `null`.
     * @param debug `true` to request debug information.
     * @return TsInfoValue the information about the series.
     * @throws IllegalArgumentException if `key` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.info/">TS.INFO</a>
     */
    suspend fun tsInfo(key: K, debug: Boolean): TsInfoValue<K>?

    /**
     * Create a compaction rule that aggregates the samples of `sourceKey` into `destKey`. Both series must exist,
     * and `destKey` must not be the source or destination of another rule. Only samples added after the rule is created
     * are aggregated. Each compacted sample has the start of its bucket as timestamp; buckets start at multiples of
     * `bucketDuration` from the epoch.
     *
     * Both keys must hash to the same slot, for example by sharing a hash tag such as `{sensor1}`. In Redis Cluster, keys
     * in different slots are rejected with `CROSSSLOT` on Redis 8.4 and later; Redis 8.0 and 8.2 reply
     * `TSDB: the key does not exist` instead, even though both keys exist.
     *
     * @param sourceKey the key of the series to aggregate, must not be `null`.
     * @param destKey the key of the series that receives the compacted samples, must not be `null`.
     * @param aggregation the aggregation, must not be `null`. `countNaN` and `countAll` require Redis 8.6 or
     *        later.
     * @param bucketDuration the duration of each bucket in milliseconds, must be positive.
     * @return String simple-string-reply `OK` if the rule was created.
     * @throws IllegalArgumentException if `sourceKey`, `destKey` or `aggregation` is `null`, or if
     *         `bucketDuration` is not positive.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.createrule/">TS.CREATERULE</a>
     */
    suspend fun tsCreateRule(sourceKey: K, destKey: K, aggregation: TsAggregation, bucketDuration: Long): String?

    /**
     * Create a compaction rule with aligned buckets that aggregates the samples of `sourceKey` into `destKey`. Like
     * [tsCreateRule], but a bucket starts exactly at `alignTimestamp`.
     *
     * Both keys must hash to the same slot, for example by sharing a hash tag such as `{sensor1}`. In Redis Cluster, keys
     * in different slots are rejected with `CROSSSLOT` on Redis 8.4 and later; Redis 8.0 and 8.2 reply
     * `TSDB: the key does not exist` instead, even though both keys exist.
     *
     * @param sourceKey the key of the series to aggregate, must not be `null`.
     * @param destKey the key of the series that receives the compacted samples, must not be `null`.
     * @param aggregation the aggregation, must not be `null`. `countNaN` and `countAll` require Redis 8.6 or
     *        later.
     * @param bucketDuration the duration of each bucket in milliseconds, must be positive.
     * @param alignTimestamp the timestamp in milliseconds at which a bucket starts, must not be negative.
     * @return String simple-string-reply `OK` if the rule was created.
     * @throws IllegalArgumentException if `sourceKey`, `destKey` or `aggregation` is `null`, if
     *         `bucketDuration` is not positive, or if `alignTimestamp` is negative.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.createrule/">TS.CREATERULE</a>
     */
    suspend fun tsCreateRule(sourceKey: K, destKey: K, aggregation: TsAggregation, bucketDuration: Long, alignTimestamp: Long): String?

    /**
     * Delete the compaction rule from `sourceKey` to `destKey`. Both series are kept.
     *
     * Both keys must hash to the same slot, for example by sharing a hash tag such as `{sensor1}`. In Redis Cluster, keys
     * in different slots are rejected with `CROSSSLOT` on Redis 8.4 and later; Redis 8.0 and 8.2 reply
     * `TSDB: compaction rule does not exist` instead, even though both keys exist.
     *
     * @param sourceKey the key of the source series, must not be `null`.
     * @param destKey the key of the destination series, must not be `null`.
     * @return String simple-string-reply `OK` if the rule was deleted.
     * @throws IllegalArgumentException if `sourceKey` or `destKey` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.deleterule/">TS.DELETERULE</a>
     */
    suspend fun tsDeleteRule(sourceKey: K, destKey: K): String?

    /**
     * Add a sample with the current server time to a time series, creating the series if it does not exist.
     *
     * The sample gets timestamp `*`, so the server clock sets it. To set the timestamp, use
     * `tsAdd(key, timestamp, value)`.
     *
     * @param key the key, must not be `null`.
     * @param value the value.
     * @return Long integer-reply the timestamp of the added sample, or the highest timestamp of the series if the sample was
     *         ignored as a duplicate (`IGNORE`).
     * @throws IllegalArgumentException if `key` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.add/">TS.ADD</a>
     */
    suspend fun tsAdd(key: K, value: Double): Long?

    /**
     * Add a sample with the current server time to a time series, with options.
     *
     * The sample gets timestamp `*`, so the server clock sets it. To set the timestamp, use
     * `tsAdd(key, timestamp, value, args)`.
     *
     * If the series does not exist, it is created. The series options in `args` apply only when the series is created;
     * for an existing series the server ignores them.
     *
     * @param key the key, must not be `null`.
     * @param value the value.
     * @param args the options, must not be `null`.
     * @return Long integer-reply the timestamp of the added sample, or the highest timestamp of the series if the sample was
     *         ignored as a duplicate (`IGNORE`).
     * @throws IllegalArgumentException if `key` or `args` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.add/">TS.ADD</a>
     */
    suspend fun tsAdd(key: K, value: Double, args: TsAddArgs): Long?

    /**
     * Add a sample to a time series, creating the series if it does not exist.
     *
     * @param key the key, must not be `null`.
     * @param timestamp the timestamp in milliseconds.
     * @param value the value. NaN requires Redis 8.6 or later.
     * @return Long integer-reply the timestamp of the added sample, or the highest timestamp of the series if the sample was
     *         ignored as a duplicate (`IGNORE`).
     * @throws IllegalArgumentException if `key` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.add/">TS.ADD</a>
     */
    suspend fun tsAdd(key: K, timestamp: Long, value: Double): Long?

    /**
     * Add a sample to a time series, with options.
     *
     * If the series does not exist, it is created. The series options in `args` apply only when the series is created;
     * for an existing series the server ignores them.
     *
     * @param key the key, must not be `null`.
     * @param timestamp the timestamp in milliseconds.
     * @param value the value. NaN requires Redis 8.6 or later.
     * @param args the options, must not be `null`.
     * @return Long integer-reply the timestamp of the added sample, or the highest timestamp of the series if the sample was
     *         ignored as a duplicate (`IGNORE`).
     * @throws IllegalArgumentException if `key` or `args` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.add/">TS.ADD</a>
     */
    suspend fun tsAdd(key: K, timestamp: Long, value: Double, args: TsAddArgs): Long?

    /**
     * Add samples to one or more existing time series. Unlike [tsAdd], the command does not create
     * series.
     *
     * The server handles each sample on its own. A rejected sample, for example a duplicate under policy `BLOCK` or a
     * missing key, gives a `null` element at its position, and the other samples are still added. To add a single sample
     * with options, use [tsAdd].
     *
     * With the Redis Cluster API (`RedisAdvancedClusterCommands` and its async and reactive variants), samples whose keys
     * hash to different slots are sent as one command per slot. The split is not atomic across slots: each slot's command is
     * applied on its own. The result keeps the order of `samples`.
     *
     * @param samples the samples, must not be `null` or empty and must not contain `null` elements.
     * @return List<Long> one element per sample, in the order of `samples`: the timestamp of the added sample, the
     *         highest timestamp of the series if the sample was ignored as a duplicate, or `null` if the server rejected
     *         the sample.
     * @throws IllegalArgumentException if `samples` is `null`, empty or contains `null` elements.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.madd/">TS.MADD</a>
     */
    suspend fun tsMAdd(samples: List<TsMAddValue<K>>): List<Long?>

    /**
     * Increase the value of the latest sample of a time series, creating the series if it does not exist. The sample gets the
     * current server time; if that equals the highest timestamp of the series, the latest sample is updated in place, otherwise
     * a new sample with the increased value is added.
     *
     * @param key the key, must not be `null`.
     * @param addend the amount to add, must not be NaN.
     * @return Long integer-reply the timestamp of the updated or added sample, or the highest timestamp of the series if the
     *         sample was ignored as a duplicate (`IGNORE`).
     * @throws IllegalArgumentException if `key` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.incrby/">TS.INCRBY</a>
     */
    suspend fun tsIncrBy(key: K, addend: Double): Long?

    /**
     * Increase the value of the latest sample of a time series, with options. With [TsIncrByArgs.timestamp], the
     * timestamp must be equal to or higher than the highest timestamp of the series; an equal timestamp updates the latest
     * sample in place.
     *
     * If the series does not exist, it is created. The series options in `args` apply only when the series is created;
     * for an existing series the server ignores them.
     *
     * @param key the key, must not be `null`.
     * @param addend the amount to add, must not be NaN.
     * @param args the options, must not be `null`.
     * @return Long integer-reply the timestamp of the updated or added sample, or the highest timestamp of the series if the
     *         sample was ignored as a duplicate (`IGNORE`).
     * @throws IllegalArgumentException if `key` or `args` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.incrby/">TS.INCRBY</a>
     */
    suspend fun tsIncrBy(key: K, addend: Double, args: TsIncrByArgs): Long?

    /**
     * Decrease the value of the latest sample of a time series, creating the series if it does not exist. The sample gets the
     * current server time; if that equals the highest timestamp of the series, the latest sample is updated in place, otherwise
     * a new sample with the decreased value is added.
     *
     * @param key the key, must not be `null`.
     * @param subtrahend the amount to subtract, must not be NaN.
     * @return Long integer-reply the timestamp of the updated or added sample, or the highest timestamp of the series if the
     *         sample was ignored as a duplicate (`IGNORE`).
     * @throws IllegalArgumentException if `key` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.decrby/">TS.DECRBY</a>
     */
    suspend fun tsDecrBy(key: K, subtrahend: Double): Long?

    /**
     * Decrease the value of the latest sample of a time series, with options. With [TsIncrByArgs.timestamp], the
     * timestamp must be equal to or higher than the highest timestamp of the series; an equal timestamp updates the latest
     * sample in place.
     *
     * If the series does not exist, it is created. The series options in `args` apply only when the series is created;
     * for an existing series the server ignores them.
     *
     * @param key the key, must not be `null`.
     * @param subtrahend the amount to subtract, must not be NaN.
     * @param args the options, must not be `null`.
     * @return Long integer-reply the timestamp of the updated or added sample, or the highest timestamp of the series if the
     *         sample was ignored as a duplicate (`IGNORE`).
     * @throws IllegalArgumentException if `key` or `args` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.decrby/">TS.DECRBY</a>
     */
    suspend fun tsDecrBy(key: K, subtrahend: Double, args: TsIncrByArgs): Long?

    /**
     * Get the latest sample of a time series. For a compaction, the latest bucket is reported only once it is complete; use
     * [tsGet] with `latest` set to also get the open bucket.
     *
     * @param key the key, must not be `null`.
     * @return TsSample the latest sample, or `null` if the series has no samples.
     * @throws IllegalArgumentException if `key` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.get/">TS.GET</a>
     */
    suspend fun tsGet(key: K): TsSample?

    /**
     * Get the latest sample of a time series. With `latest` set, the command sends `LATEST`: for a compaction, the
     * latest, possibly partial, bucket is reported. The server ignores it for a series that is not a compaction.
     *
     * @param key the key, must not be `null`.
     * @param latest `true` to report the latest, possibly partial, bucket of a compaction.
     * @return TsSample the latest sample, or `null` if the series has no samples.
     * @throws IllegalArgumentException if `key` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.get/">TS.GET</a>
     */
    suspend fun tsGet(key: K, latest: Boolean): TsSample?

    /**
     * Get the samples of a time series within a range of timestamps, in ascending order.
     *
     * An inclusive bound sends that timestamp, and an unbounded side sends `-` (earliest) or `+` (latest). Because
     * timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor: an exclusive lower bound
     * `t` sends `t + 1`, an exclusive upper bound `t` sends `t - 1`.
     *
     * @param key the key, must not be `null`.
     * @param range the range of timestamps in milliseconds, must not be `null`.
     * @return List<TsSample> the samples in ascending order; empty if the range has no samples.
     * @throws IllegalArgumentException if `key` or `range` is `null`, or if `range` has an exclusive
     *         lower bound at [Long.MAX_VALUE] or an exclusive upper bound at [Long.MIN_VALUE].
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.range/">TS.RANGE</a>
     */
    suspend fun tsRange(key: K, range: Range<Long>): List<TsSample>

    /**
     * Get the samples of a time series within a range of timestamps, in ascending order, with filters, a limit or an
     * aggregation. With an aggregation, each sample is one bucket and has one value per aggregation, in the order of the
     * request. Several aggregations require Redis 8.8 or later.
     *
     * The range bounds follow the same rules as in [tsRange]. For a compaction, the last reported bucket
     * may include samples newer than the upper bound, because the bound limits only the bucket start.
     *
     * @param key the key, must not be `null`.
     * @param range the range of timestamps in milliseconds, must not be `null`.
     * @param args the options, must not be `null`.
     * @return List<TsSample> the samples or buckets in ascending order; empty if the range has no samples.
     * @throws IllegalArgumentException if `key`, `range` or `args` is `null`, if `range` has an
     *         exclusive lower bound at [Long.MAX_VALUE] or an exclusive upper bound at [Long.MIN_VALUE], or if
     *         `args` sets `ALIGN`, `BUCKETTIMESTAMP` or `EMPTY` without an aggregation.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.range/">TS.RANGE</a>
     */
    suspend fun tsRange(key: K, range: Range<Long>, args: TsRangeArgs): List<TsSample>

    /**
     * Get the samples of a time series within a range of timestamps, in descending order.
     *
     * An inclusive bound sends that timestamp, and an unbounded side sends `-` (earliest) or `+` (latest). Because
     * timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor: an exclusive lower bound
     * `t` sends `t + 1`, an exclusive upper bound `t` sends `t - 1`.
     *
     * @param key the key, must not be `null`.
     * @param range the range of timestamps in milliseconds, must not be `null`.
     * @return List<TsSample> the samples in descending order; empty if the range has no samples.
     * @throws IllegalArgumentException if `key` or `range` is `null`, or if `range` has an exclusive
     *         lower bound at [Long.MAX_VALUE] or an exclusive upper bound at [Long.MIN_VALUE].
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.revrange/">TS.REVRANGE</a>
     */
    suspend fun tsRevRange(key: K, range: Range<Long>): List<TsSample>

    /**
     * Get the samples of a time series within a range of timestamps, in descending order, with filters, a limit or an
     * aggregation. With an aggregation, each sample is one bucket and has one value per aggregation, in the order of the
     * request. Several aggregations require Redis 8.8 or later.
     *
     * The range bounds follow the same rules as in [tsRevRange]. For a compaction, the last reported
     * bucket may include samples newer than the upper bound, because the bound limits only the bucket start.
     *
     * @param key the key, must not be `null`.
     * @param range the range of timestamps in milliseconds, must not be `null`.
     * @param args the options, must not be `null`.
     * @return List<TsSample> the samples or buckets in descending order; empty if the range has no samples.
     * @throws IllegalArgumentException if `key`, `range` or `args` is `null`, if `range` has an
     *         exclusive lower bound at [Long.MAX_VALUE] or an exclusive upper bound at [Long.MIN_VALUE], or if
     *         `args` sets `ALIGN`, `BUCKETTIMESTAMP` or `EMPTY` without an aggregation.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.revrange/">TS.REVRANGE</a>
     */
    suspend fun tsRevRange(key: K, range: Range<Long>, args: TsRangeArgs): List<TsSample>

    /**
     * Read the samples of a time series from `offset` on, in ascending order, without blocking. Requires Redis 8.10 or
     * later.
     *
     * Use [TsReadOffset.latest] or [TsReadOffset.newSamples] only on the first call, and continue with
     * `TsReadOffset.from(lastTimestamp + 1)`. Without `BLOCK`, [TsReadOffset.newSamples] always returns an
     * empty list. A key that does not exist returns an empty list.
     *
     * @param key the key, must not be `null`.
     * @param offset the inclusive lower bound, must not be `null`.
     * @return List<TsSample> the samples in ascending order; empty if there are none.
     * @throws IllegalArgumentException if `key` or `offset` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.read/">TS.READ</a>
     */
    suspend fun tsRead(key: K, offset: TsReadOffset): List<TsSample>

    /**
     * Read the samples of a time series from `offset` on, in ascending order, optionally waiting for new samples.
     * Requires Redis 8.10 or later.
     *
     * With [TsReadArgs.block], the command waits until enough samples exist, the block time ends, or the key
     * is removed. A timeout returns the samples found so far. Deleting or expiring the key (DEL, UNLINK, FLUSHDB, FLUSHALL,
     * expiry, eviction) wakes every waiter with an empty list, `BLOCK 0` included, so an empty list means either a
     * timeout or a removed key. `BLOCK 0` waits without a time limit.
     *
     * While it waits, the command occupies its connection and stalls every other command sent on that connection. Use a
     * dedicated connection whose command timeout is longer than the block time; Lettuce does not extend the timeout. In Redis
     * Cluster, a cluster connection shares one connection per node, so a blocked TS.READ stalls every command that cluster
     * connection sends to that node; use a dedicated `StatefulRedisClusterConnection`. With `ReadFrom` set to a
     * replica, TS.READ runs on a replica, which wakes only after the write has replicated to it; read from the primary for
     * low-latency tailing. The server rejects `BLOCK` inside MULTI and Lua scripts.
     *
     * Use [TsReadOffset.latest] or [TsReadOffset.newSamples] only on the first call, and continue with
     * `TsReadOffset.from(lastTimestamp + 1)`.
     *
     * @param key the key, must not be `null`.
     * @param offset the inclusive lower bound, must not be `null`.
     * @param args the options, must not be `null`.
     * @return List<TsSample> the samples in ascending order; empty on a timeout without samples or when the key was
     *         removed.
     * @throws IllegalArgumentException if `key`, `offset` or `args` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.read/">TS.READ</a>
     */
    suspend fun tsRead(key: K, offset: TsReadOffset, args: TsReadArgs): List<TsSample>

    /**
     * Get the latest sample of every time series that matches the filters. No labels are returned.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param filter the filter, must not be `null`.
     * @return List<TsMGetValue> one entry per matching series; empty if no series matches.
     * @throws IllegalArgumentException if `filter` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mget/">TS.MGET</a>
     */
    suspend fun tsMGet(filter: TsFilter): List<TsMGetValue<K>>

    /**
     * Get the latest sample of every time series that matches the filters. No labels are returned.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param filters the filters, combined with AND, must not be `null` or empty and must not contain `null`
     *        elements.
     * @return List<TsMGetValue> one entry per matching series; empty if no series matches.
     * @throws IllegalArgumentException if `filters` is `null`, empty or contains `null` elements.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mget/">TS.MGET</a>
     */
    suspend fun tsMGet(vararg filters: TsFilter): List<TsMGetValue<K>>

    /**
     * Get the latest sample of every time series that matches the filters, with options such as labels.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param args the options, must not be `null`.
     * @param filter the filter, must not be `null`.
     * @return List<TsMGetValue> one entry per matching series; empty if no series matches.
     * @throws IllegalArgumentException if `args` is `null`, or `filter` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mget/">TS.MGET</a>
     */
    suspend fun tsMGet(args: TsMGetArgs, filter: TsFilter): List<TsMGetValue<K>>

    /**
     * Get the latest sample of every time series that matches the filters, with options such as labels.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param args the options, must not be `null`.
     * @param filters the filters, combined with AND, must not be `null` or empty and must not contain `null`
     *        elements.
     * @return List<TsMGetValue> one entry per matching series; empty if no series matches.
     * @throws IllegalArgumentException if `args` is `null`, or `filters` is `null`, empty or contains
     *         `null` elements.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mget/">TS.MGET</a>
     */
    suspend fun tsMGet(args: TsMGetArgs, vararg filters: TsFilter): List<TsMGetValue<K>>

    /**
     * Get the keys of all time series that match the filters. The result includes series the user cannot read.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param filter the filter, must not be `null`.
     * @return List<K> the keys of the matching series; empty if no series matches.
     * @throws IllegalArgumentException if `filter` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.queryindex/">TS.QUERYINDEX</a>
     */
    suspend fun tsQueryIndex(filter: TsFilter): List<K>

    /**
     * Get the keys of all time series that match the filters. The result includes series the user cannot read.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param filters the filters, combined with AND, must not be `null` or empty and must not contain `null`
     *        elements.
     * @return List<K> the keys of the matching series; empty if no series matches.
     * @throws IllegalArgumentException if `filters` is `null`, empty or contains `null` elements.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.queryindex/">TS.QUERYINDEX</a>
     */
    suspend fun tsQueryIndex(vararg filters: TsFilter): List<K>

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in ascending order. No labels
     * are returned.
     *
     * An inclusive bound sends that timestamp, and an unbounded side sends `-` (earliest) or `+` (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound `t` sends `t + 1`, an exclusive upper bound `t` sends `t - 1`.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be `null`.
     * @param filter the filter, must not be `null`.
     * @return List<TsSeries> one entry per matching series, or per group with `GROUPBY`; a series without samples
     *         in the range has an empty sample list unless `EXCLUDEEMPTY` is set.
     * @throws IllegalArgumentException if `range` is `null`, `filter` is `null`, or if `range`
     *         has an exclusive lower bound at [Long.MAX_VALUE] or an exclusive upper bound at [Long.MIN_VALUE].
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrange/">TS.MRANGE</a>
     */
    suspend fun tsMRange(range: Range<Long>, filter: TsFilter): List<TsSeries<K>>

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in ascending order. No labels
     * are returned.
     *
     * An inclusive bound sends that timestamp, and an unbounded side sends `-` (earliest) or `+` (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound `t` sends `t + 1`, an exclusive upper bound `t` sends `t - 1`.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be `null`.
     * @param filters the filters, combined with AND, must not be `null` or empty and must not contain `null`
     *        elements.
     * @return List<TsSeries> one entry per matching series, or per group with `GROUPBY`; a series without samples
     *         in the range has an empty sample list unless `EXCLUDEEMPTY` is set.
     * @throws IllegalArgumentException if `range` is `null`, `filters` is `null`, empty or contains
     *         `null` elements, or if `range` has an exclusive lower bound at [Long.MAX_VALUE] or an exclusive
     *         upper bound at [Long.MIN_VALUE].
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrange/">TS.MRANGE</a>
     */
    suspend fun tsMRange(range: Range<Long>, vararg filters: TsFilter): List<TsSeries<K>>

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in ascending order, with
     * options such as labels, aggregation and `GROUPBY`.
     *
     * An inclusive bound sends that timestamp, and an unbounded side sends `-` (earliest) or `+` (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound `t` sends `t + 1`, an exclusive upper bound `t` sends `t - 1`.
     *
     * With an aggregation, each sample is one bucket with one value per aggregation; several aggregations require Redis 8.8 or
     * later and cannot be combined with `GROUPBY`. With `GROUPBY`, each entry is one group of series combined by
     * the reducer. `EXCLUDEEMPTY` requires Redis 8.10 or later.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be `null`.
     * @param args the options, must not be `null`.
     * @param filter the filter, must not be `null`.
     * @return List<TsSeries> one entry per matching series, or per group with `GROUPBY`; a series without samples
     *         in the range has an empty sample list unless `EXCLUDEEMPTY` is set.
     * @throws IllegalArgumentException if `range` or `args` is `null`, `filter` is `null`, if
     *         `range` has an exclusive lower bound at [Long.MAX_VALUE] or an exclusive upper bound at
     *         [Long.MIN_VALUE], or if `args` has conflicting options.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrange/">TS.MRANGE</a>
     */
    suspend fun tsMRange(range: Range<Long>, args: TsMRangeArgs, filter: TsFilter): List<TsSeries<K>>

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in ascending order, with
     * options such as labels, aggregation and `GROUPBY`.
     *
     * An inclusive bound sends that timestamp, and an unbounded side sends `-` (earliest) or `+` (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound `t` sends `t + 1`, an exclusive upper bound `t` sends `t - 1`.
     *
     * With an aggregation, each sample is one bucket with one value per aggregation; several aggregations require Redis 8.8 or
     * later and cannot be combined with `GROUPBY`. With `GROUPBY`, each entry is one group of series combined by
     * the reducer. `EXCLUDEEMPTY` requires Redis 8.10 or later.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be `null`.
     * @param args the options, must not be `null`.
     * @param filters the filters, combined with AND, must not be `null` or empty and must not contain `null`
     *        elements.
     * @return List<TsSeries> one entry per matching series, or per group with `GROUPBY`; a series without samples
     *         in the range has an empty sample list unless `EXCLUDEEMPTY` is set.
     * @throws IllegalArgumentException if `range` or `args` is `null`, `filters` is `null`, empty
     *         or contains `null` elements, if `range` has an exclusive lower bound at [Long.MAX_VALUE] or an
     *         exclusive upper bound at [Long.MIN_VALUE], or if `args` has conflicting options.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrange/">TS.MRANGE</a>
     */
    suspend fun tsMRange(range: Range<Long>, args: TsMRangeArgs, vararg filters: TsFilter): List<TsSeries<K>>

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in descending order. No
     * labels are returned.
     *
     * An inclusive bound sends that timestamp, and an unbounded side sends `-` (earliest) or `+` (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound `t` sends `t + 1`, an exclusive upper bound `t` sends `t - 1`.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be `null`.
     * @param filter the filter, must not be `null`.
     * @return List<TsSeries> one entry per matching series, or per group with `GROUPBY`; a series without samples
     *         in the range has an empty sample list unless `EXCLUDEEMPTY` is set.
     * @throws IllegalArgumentException if `range` is `null`, `filter` is `null`, or if `range`
     *         has an exclusive lower bound at [Long.MAX_VALUE] or an exclusive upper bound at [Long.MIN_VALUE].
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrevrange/">TS.MREVRANGE</a>
     */
    suspend fun tsMRevRange(range: Range<Long>, filter: TsFilter): List<TsSeries<K>>

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in descending order. No
     * labels are returned.
     *
     * An inclusive bound sends that timestamp, and an unbounded side sends `-` (earliest) or `+` (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound `t` sends `t + 1`, an exclusive upper bound `t` sends `t - 1`.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be `null`.
     * @param filters the filters, combined with AND, must not be `null` or empty and must not contain `null`
     *        elements.
     * @return List<TsSeries> one entry per matching series, or per group with `GROUPBY`; a series without samples
     *         in the range has an empty sample list unless `EXCLUDEEMPTY` is set.
     * @throws IllegalArgumentException if `range` is `null`, `filters` is `null`, empty or contains
     *         `null` elements, or if `range` has an exclusive lower bound at [Long.MAX_VALUE] or an exclusive
     *         upper bound at [Long.MIN_VALUE].
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrevrange/">TS.MREVRANGE</a>
     */
    suspend fun tsMRevRange(range: Range<Long>, vararg filters: TsFilter): List<TsSeries<K>>

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in descending order, with
     * options such as labels, aggregation and `GROUPBY`.
     *
     * An inclusive bound sends that timestamp, and an unbounded side sends `-` (earliest) or `+` (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound `t` sends `t + 1`, an exclusive upper bound `t` sends `t - 1`.
     *
     * With an aggregation, each sample is one bucket with one value per aggregation; several aggregations require Redis 8.8 or
     * later and cannot be combined with `GROUPBY`. With `GROUPBY`, each entry is one group of series combined by
     * the reducer. `EXCLUDEEMPTY` requires Redis 8.10 or later.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be `null`.
     * @param args the options, must not be `null`.
     * @param filter the filter, must not be `null`.
     * @return List<TsSeries> one entry per matching series, or per group with `GROUPBY`; a series without samples
     *         in the range has an empty sample list unless `EXCLUDEEMPTY` is set.
     * @throws IllegalArgumentException if `range` or `args` is `null`, `filter` is `null`, if
     *         `range` has an exclusive lower bound at [Long.MAX_VALUE] or an exclusive upper bound at
     *         [Long.MIN_VALUE], or if `args` has conflicting options.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrevrange/">TS.MREVRANGE</a>
     */
    suspend fun tsMRevRange(range: Range<Long>, args: TsMRangeArgs, filter: TsFilter): List<TsSeries<K>>

    /**
     * Get the samples within a range of timestamps of every time series that matches the filters, in descending order, with
     * options such as labels, aggregation and `GROUPBY`.
     *
     * An inclusive bound sends that timestamp, and an unbounded side sends `-` (earliest) or `+` (latest) across
     * the matching series. Because timestamps are integer milliseconds, an exclusive bound is sent as its inclusive neighbor:
     * an exclusive lower bound `t` sends `t + 1`, an exclusive upper bound `t` sends `t - 1`.
     *
     * With an aggregation, each sample is one bucket with one value per aggregation; several aggregations require Redis 8.8 or
     * later and cannot be combined with `GROUPBY`. With `GROUPBY`, each entry is one group of series combined by
     * the reducer. `EXCLUDEEMPTY` requires Redis 8.10 or later.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * The whole reply is held in memory before it is returned.
     *
     * @param range the range of timestamps in milliseconds, must not be `null`.
     * @param args the options, must not be `null`.
     * @param filters the filters, combined with AND, must not be `null` or empty and must not contain `null`
     *        elements.
     * @return List<TsSeries> one entry per matching series, or per group with `GROUPBY`; a series without samples
     *         in the range has an empty sample list unless `EXCLUDEEMPTY` is set.
     * @throws IllegalArgumentException if `range` or `args` is `null`, `filters` is `null`, empty
     *         or contains `null` elements, if `range` has an exclusive lower bound at [Long.MAX_VALUE] or an
     *         exclusive upper bound at [Long.MIN_VALUE], or if `args` has conflicting options.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.mrevrange/">TS.MREVRANGE</a>
     */
    suspend fun tsMRevRange(range: Range<Long>, args: TsMRangeArgs, vararg filters: TsFilter): List<TsSeries<K>>

    /**
     * Get the names of the labels of all indexed time series. Requires Redis 8.10 or later.
     *
     * Results are unordered and contain no duplicates. Labels are decoded as UTF-8, whatever codec the connection uses.
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @return List<String> the label names; empty if no series matches.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.querylabels/">TS.QUERYLABELS</a>
     */
    suspend fun tsQueryLabels(): List<String>

    /**
     * Get the names of the labels of the time series that match the filters. Requires Redis 8.10 or later.
     *
     * Results are unordered and contain no duplicates. Labels are decoded as UTF-8, whatever codec the connection uses.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param filter the filter, must not be `null`.
     * @return List<String> the label names; empty if no series matches.
     * @throws IllegalArgumentException if `filter` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.querylabels/">TS.QUERYLABELS</a>
     */
    suspend fun tsQueryLabels(filter: TsFilter): List<String>

    /**
     * Get the names of the labels of the time series that match the filters. Requires Redis 8.10 or later. At least one filter is required; use the overload without filters to query all series.
     *
     * Results are unordered and contain no duplicates. Labels are decoded as UTF-8, whatever codec the connection uses.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param filters the filters, combined with AND, must not be `null` or empty and must not contain `null`
     *        elements.
     * @return List<String> the label names; empty if no series matches.
     * @throws IllegalArgumentException if `filters` is `null`, empty or contains `null` elements.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.querylabels/">TS.QUERYLABELS</a>
     */
    suspend fun tsQueryLabels(vararg filters: TsFilter): List<String>

    /**
     * Get the values of label `label` across all indexed time series. Requires Redis 8.10 or later.
     *
     * Results are unordered and contain no duplicates. Labels are decoded as UTF-8, whatever codec the connection uses.
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param label the label name, must not be `null` or empty.
     * @return List<String> the values; empty if no series matches or no series has the label.
     * @throws IllegalArgumentException if `label` is `null` or empty.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.querylabels/">TS.QUERYLABELS</a>
     */
    suspend fun tsQueryLabelValues(label: String): List<String>

    /**
     * Get the values of label `label` of the time series that match the filters. Requires Redis 8.10 or later.
     *
     * Results are unordered and contain no duplicates. Labels are decoded as UTF-8, whatever codec the connection uses.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param label the label name, must not be `null` or empty.
     * @param filter the filter, must not be `null`.
     * @return List<String> the values; empty if no series matches or no series has the label.
     * @throws IllegalArgumentException if `label` is `null` or empty, or `filter` is `null`.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.querylabels/">TS.QUERYLABELS</a>
     */
    suspend fun tsQueryLabelValues(label: String, filter: TsFilter): List<String>

    /**
     * Get the values of label `label` of the time series that match the filters. Requires Redis 8.10 or later. At least one filter is required; use the overload without filters to query all series.
     *
     * Results are unordered and contain no duplicates. Labels are decoded as UTF-8, whatever codec the connection uses.
     *
     * The server requires at least one filter that matches a value ([TsFilter.equal] or
     * [TsFilter.in]).
     *
     * In Redis Cluster the command runs on one node. On Redis 8.0–8.8, results include every shard only after
     * `TIMESERIES.REFRESHCLUSTER` has run on that node. Result order is not guaranteed. The server rejects the command
     * inside MULTI on a multi-shard node.
     *
     * @param label the label name, must not be `null` or empty.
     * @param filters the filters, combined with AND, must not be `null` or empty and must not contain `null`
     *        elements.
     * @return List<String> the values; empty if no series matches or no series has the label.
     * @throws IllegalArgumentException if `label` is `null` or empty, or `filters` is `null`, empty or contains `null` elements.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.querylabels/">TS.QUERYLABELS</a>
     */
    suspend fun tsQueryLabelValues(label: String, vararg filters: TsFilter): List<String>

}
