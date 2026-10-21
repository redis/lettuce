/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */

package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.Range
import io.lettuce.core.timeseries.TsInfoValue
import io.lettuce.core.timeseries.TsMAddValue
import io.lettuce.core.timeseries.arguments.TsAddArgs
import io.lettuce.core.timeseries.arguments.TsAlterArgs
import io.lettuce.core.timeseries.arguments.TsCreateArgs
import io.lettuce.core.timeseries.arguments.TsIncrByArgs

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
     * In Redis Cluster, samples whose keys hash to different slots are sent as one command per slot. The split is not atomic
     * across slots: each slot's command is applied on its own. The result keeps the order of `samples`.
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

}
