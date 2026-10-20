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
import io.lettuce.core.timeseries.arguments.TsAlterArgs
import io.lettuce.core.timeseries.arguments.TsCreateArgs

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

}
