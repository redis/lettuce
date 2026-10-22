/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cluster.api.async;

import io.lettuce.core.Range;
import io.lettuce.core.timeseries.TsInfoValue;
import io.lettuce.core.timeseries.arguments.TsAlterArgs;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;

/**
 * Asynchronous executed commands on a node selection for Redis TimeSeries.
 * <p>
 * Sample values are {@code double} and labels are {@code String}, so the value type {@code V} is not used by these commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @see <a href="https://redis.io/docs/latest/develop/data-types/timeseries/">Redis TimeSeries</a>
 * @since 8.0
 */
public interface NodeSelectionTimeSeriesAsyncCommands<K, V> {

    /**
     * Create a new time series with the server's default options.
     *
     * @param key the key, must not be {@code null}.
     * @return String simple-string-reply {@code OK} if the series was created.
     * @throws IllegalArgumentException if {@code key} is {@code null}.
     * @since 8.0
     * @see <a href="https://redis.io/commands/ts.create/">TS.CREATE</a>
     */
    AsyncExecutions<String> tsCreate(K key);

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
    AsyncExecutions<String> tsCreate(K key, TsCreateArgs args);

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
    AsyncExecutions<String> tsAlter(K key, TsAlterArgs args);

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
    AsyncExecutions<Long> tsDel(K key, Range<Long> range);

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
    AsyncExecutions<TsInfoValue<K>> tsInfo(K key);

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
    AsyncExecutions<TsInfoValue<K>> tsInfo(K key, boolean debug);

}
