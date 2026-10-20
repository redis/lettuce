/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries

import io.lettuce.TestTags
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.timeseries.arguments.TsIncrByArgs
import io.lettuce.test.LettuceExtension
import io.lettuce.test.condition.EnabledOnCommand
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.inject.Inject

/**
 * Integration tests for the Kotlin coroutines TimeSeries write commands.
 */
@Tag(TestTags.INTEGRATION_TEST)
@ExtendWith(LettuceExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("TS.ADD")
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisTimeSeriesCoroutinesIntegrationTests @Inject constructor(
    private val connection: StatefulRedisConnection<String, String>
) {

    private val key = "{ts}:k"

    @BeforeEach
    fun setUp() {
        connection.sync().flushall()
    }

    /**
     * Scenario L04: the coroutines `tsMAdd` returns `List<Long?>` with `null` for a rejected sample (duplicate under BLOCK,
     * missing key), in input order.
     */
    @Test
    fun maddReturnsNullForRejectedSample(): Unit = runBlocking {

        val ts = connection.coroutines()
        ts.tsCreate(key)
        assertThat(ts.tsAdd(key, 1000L, 5.0)).isEqualTo(1000L)

        val result: List<Long?> = ts.tsMAdd(
            listOf(
                TsMAddValue.of(key, 1000L, 6.0),
                TsMAddValue.of(key, 2000L, 7.0),
                TsMAddValue.of("{ts}:missing", 1000L, 1.0)
            )
        )

        assertThat(result).containsExactly(null, 2000L, null)
        assertThat(connection.sync().tsInfo(key).totalSamples).isEqualTo(2)
    }

    /**
     * Scenario L04: the other write commands return the timestamp.
     */
    @Test
    fun writesReturnTimestamps(): Unit = runBlocking {

        val ts = connection.coroutines()

        assertThat(ts.tsAdd(key, 1000L, 1.0)).isEqualTo(1000L)
        assertThat(ts.tsIncrBy(key, 1.0, TsIncrByArgs.Builder.timestamp(2000)))
            .isEqualTo(2000L)
        assertThat(ts.tsDecrBy(key, 1.0, TsIncrByArgs.Builder.timestamp(3000)))
            .isEqualTo(3000L)
        assertThat(ts.tsMAdd(listOf(TsMAddValue.of(key, 4000L, 4.0)))).containsExactly(4000L)
        assertThat(connection.sync().tsInfo(key).totalSamples).isEqualTo(4)
    }
}
