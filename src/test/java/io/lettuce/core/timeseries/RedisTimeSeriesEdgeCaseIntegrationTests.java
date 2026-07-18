/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Edge-case integration tests for Redis TimeSeries commands, split out from {@link RedisTimeSeriesIntegrationTests} because
 * these round-trips assert against a live server rather than just a round-trip of client-supplied values: {@code Double}
 * special values ({@code Infinity}/{@code NaN}) sent through {@code TS.ADD}, the (undocumented) interaction between a
 * {@code NaN} value and each {@code DUPLICATE_POLICY}, and async error propagation on a server-rejected command.
 *
 * @author Gyumin Hwang
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("TS.CREATE")
class RedisTimeSeriesEdgeCaseIntegrationTests {

    private static final String MY_KEY = "temperature:sensor1";

    private final RedisCommands<String, String> redis;

    private final RedisAsyncCommands<String, String> async;

    @Inject
    RedisTimeSeriesEdgeCaseIntegrationTests(StatefulRedisConnection<String, String> connection) {
        this.redis = connection.sync();
        this.async = connection.async();
    }

    @BeforeEach
    void prepare() {
        redis.flushall();
    }

    @AfterAll
    void teardown() {
        redis.flushall();
    }

    // ------------------------------------------------------------------------------------------------------------
    // P1-2: Infinity value rejection / NaN acceptance (asymmetric)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void tsAddRejectsPositiveInfinity() {
        assertThatThrownBy(() -> redis.tsAdd(MY_KEY, 1000, Double.POSITIVE_INFINITY))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("TSDB: invalid value");
    }

    @Test
    void tsAddRejectsNegativeInfinity() {
        assertThatThrownBy(() -> redis.tsAdd(MY_KEY, 1000, Double.NEGATIVE_INFINITY))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("TSDB: invalid value");
    }

    /**
     * Unlike {@code Infinity}, {@code NaN} is accepted by {@code TS.ADD} and stored as-is: an asymmetry in the server's value
     * validation that a Java caller passing a {@code double} through unchanged can easily trip over.
     */
    @Test
    void tsAddAcceptsNaNAndStoresIt() {
        Long timestamp = redis.tsAdd(MY_KEY, 1000, Double.NaN);

        assertThat(timestamp).isEqualTo(1000L);
        assertThat(redis.tsGet(MY_KEY).getValue()).isNaN();
    }

    // ------------------------------------------------------------------------------------------------------------
    // P1-3: NaN x DUPLICATE_POLICY matrix (undocumented server behavior)
    // ------------------------------------------------------------------------------------------------------------

    /**
     * With {@code DUPLICATE_POLICY=LAST}, overwriting an existing valid sample with a {@code NaN} value at the same timestamp
     * does <b>not</b> take effect: the server silently keeps the existing valid value instead of replacing it with {@code NaN}.
     * This behavior is not documented on redis.io; confirmed directly against a live server.
     */
    @Test
    void tsAddWithNaNUnderDuplicatePolicyLastPreservesExistingValue() {
        redis.tsCreate(MY_KEY, TsCreateArgs.Builder.duplicatePolicy(TsDuplicatePolicy.LAST));
        redis.tsAdd(MY_KEY, 1000, 5.0);

        redis.tsAdd(MY_KEY, 1000, Double.NaN);

        assertThat(redis.tsGet(MY_KEY).getValue()).isEqualTo(5.0);
    }

    @Test
    void tsAddWithNaNUnderDuplicatePolicyMaxFails() {
        assertDuplicateNaNRejected(TsDuplicatePolicy.MAX);
    }

    @Test
    void tsAddWithNaNUnderDuplicatePolicyMinFails() {
        assertDuplicateNaNRejected(TsDuplicatePolicy.MIN);
    }

    @Test
    void tsAddWithNaNUnderDuplicatePolicySumFails() {
        assertDuplicateNaNRejected(TsDuplicatePolicy.SUM);
    }

    @Test
    void tsAddWithNaNUnderDuplicatePolicyBlockFails() {
        assertDuplicateNaNRejected(TsDuplicatePolicy.BLOCK);
    }

    /**
     * Shared assertion for every {@code DUPLICATE_POLICY} other than {@code LAST}/{@code FIRST}: re-adding a {@code NaN} value
     * at a timestamp that already holds a valid sample is rejected, whatever the reason (merge policy vs. {@code BLOCK} mode),
     * because the server reports both conditions through the identical error message.
     */
    private void assertDuplicateNaNRejected(TsDuplicatePolicy policy) {
        redis.tsCreate(MY_KEY, TsCreateArgs.Builder.duplicatePolicy(policy));
        redis.tsAdd(MY_KEY, 1000, 5.0);

        assertThatThrownBy(() -> redis.tsAdd(MY_KEY, 1000, Double.NaN)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("DUPLICATE_POLICY is MAX/MIN/SUM");
    }

    // ------------------------------------------------------------------------------------------------------------
    // P1-1: async error propagation on a server-rejected command (contrast with the reactive API's hang, see
    // RedisTimeSeriesReactiveIntegrationTests#tsMGetWithoutEqualityFilterFails)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void tsInfoOnMissingKeyCompletesExceptionallyInsteadOfHanging() {
        RedisFuture<TsInfoValue<String>> future = async.tsInfo("no-such-key");

        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RedisCommandExecutionException.class).cause()
                .hasMessageContaining("TSDB: the key does not exist");
    }

    @Test
    void tsMGetWithoutEqualityFilterCompletesExceptionallyInsteadOfHanging() {
        RedisFuture<?> future = async.tsMGet("type!=temp");

        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RedisCommandExecutionException.class).cause()
                .hasMessageContaining("TSDB: please provide at least one matcher");
    }

}
