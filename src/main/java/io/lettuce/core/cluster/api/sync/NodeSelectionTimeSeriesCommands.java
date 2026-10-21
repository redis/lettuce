/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cluster.api.sync;

import java.util.List;

import io.lettuce.core.Range;
import io.lettuce.core.timeseries.TsInfoValue;
import io.lettuce.core.timeseries.TsMAddValue;
import io.lettuce.core.timeseries.arguments.TsAddArgs;
import io.lettuce.core.timeseries.arguments.TsAlterArgs;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.core.timeseries.arguments.TsIncrByArgs;

/**
 * Synchronous executed commands on a node selection for Redis TimeSeries.
 * <p>
 * Sample values are {@code double} and labels are {@code String}, so the value type {@code V} is not used by these commands.
 * <p>
 * {@code tsMAdd} runs unchanged on every selected node; it is not split by slot, so the keys must hash to slots that node
 * serves.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @see <a href="https://redis.io/docs/latest/develop/data-types/timeseries/">Redis TimeSeries</a>
 * @since 8.0
 */
public interface NodeSelectionTimeSeriesCommands<K, V> {

    /**
     * Create a new time series with the server's default options.
     *
     * @param key the key, must not be {@code null}.
     * @return String simple-string-reply {@code OK} if the series was created.
     * @throws IllegalArgumentException if {@code key} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.create/">TS.CREATE</a>
     */
    Executions<String> tsCreate(K key);

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
    Executions<String> tsCreate(K key, TsCreateArgs args);

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
    Executions<String> tsAlter(K key, TsAlterArgs args);

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
    Executions<Long> tsDel(K key, Range<Long> range);

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
    Executions<TsInfoValue<K>> tsInfo(K key);

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
    Executions<TsInfoValue<K>> tsInfo(K key, boolean debug);

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
    Executions<Long> tsAdd(K key, double value);

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
    Executions<Long> tsAdd(K key, double value, TsAddArgs args);

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
    Executions<Long> tsAdd(K key, long timestamp, double value);

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
    Executions<Long> tsAdd(K key, long timestamp, double value, TsAddArgs args);

    /**
     * Add samples to one or more existing time series. Unlike {@link #tsAdd(Object, long, double)}, the command does not create
     * series.
     * <p>
     * The server handles each sample on its own. A rejected sample, for example a duplicate under policy {@code BLOCK} or a
     * missing key, gives a {@code null} element at its position, and the other samples are still added. To add a single sample
     * with options, use {@link #tsAdd(Object, long, double, TsAddArgs)}.
     * <p>
     * With the Redis Cluster API ({@code RedisAdvancedClusterCommands} and its async and reactive variants), samples whose keys
     * hash to different slots are sent as one command per slot. The split is not atomic across slots: each slot's command is
     * applied on its own. The result keeps the order of {@code samples}.
     *
     * @param samples the samples, must not be {@code null} or empty and must not contain {@code null} elements.
     * @return List&lt;Long&gt; one element per sample, in the order of {@code samples}: the timestamp of the added sample, the
     *         highest timestamp of the series if the sample was ignored as a duplicate, or {@code null} if the server rejected
     *         the sample.
     * @throws IllegalArgumentException if {@code samples} is {@code null}, empty or contains {@code null} elements.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.madd/">TS.MADD</a>
     */
    Executions<List<Long>> tsMAdd(List<TsMAddValue<K>> samples);

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
    Executions<Long> tsIncrBy(K key, double addend);

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
    Executions<Long> tsIncrBy(K key, double addend, TsIncrByArgs args);

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
    Executions<Long> tsDecrBy(K key, double subtrahend);

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
    Executions<Long> tsDecrBy(K key, double subtrahend, TsIncrByArgs args);

}
