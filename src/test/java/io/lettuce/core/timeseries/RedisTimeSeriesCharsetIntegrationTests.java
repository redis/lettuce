/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import javax.inject.Inject;

import io.lettuce.core.RedisCommandExecutionException;
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
 * Integration tests for special-character and boundary-value handling of
 * {@link io.lettuce.core.api.sync.RedisTimeSeriesCommands}: forbidden {@code LABELS} characters, UTF-8 label/key
 * round-tripping, {@code timestamp} boundaries and {@code CHUNK_SIZE} boundaries.
 * <p>
 * Expectations below (server error messages and boundary values) were confirmed directly against a live server
 * ({@code redis-cli}) before being encoded as assertions; see the class-level PLAN comments on each test.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("TS.CREATE")
public class RedisTimeSeriesCharsetIntegrationTests {

    private static final String MY_KEY = "charset:1";

    protected final RedisCommands<String, String> redis;

    @Inject
    protected RedisTimeSeriesCharsetIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
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
    // B-1: label value forbidden characters -- server rejects "(", ")", "," and empty key/value.
    // Confirmed live: `TS.CREATE b1 LABELS region "us,west"` -> "ERR TSDB: Couldn't parse LABELS" (same message for
    // "(x)" and for an empty label key/value).
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void labelValueWithCommaIsRejected() {
        TsCreateArgs args = TsCreateArgs.Builder.label("region", "us,west");

        assertThatThrownBy(() -> redis.tsCreate(MY_KEY, args)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: Couldn't parse LABELS");
    }

    @Test
    void labelValueWithParenthesesIsRejected() {
        TsCreateArgs args = TsCreateArgs.Builder.label("k", "(x)");

        assertThatThrownBy(() -> redis.tsCreate(MY_KEY, args)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: Couldn't parse LABELS");
    }

    @Test
    void emptyLabelKeyIsRejected() {
        TsCreateArgs args = TsCreateArgs.Builder.label("", "val");

        assertThatThrownBy(() -> redis.tsCreate(MY_KEY, args)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: Couldn't parse LABELS");
    }

    @Test
    void emptyLabelValueIsRejected() {
        TsCreateArgs args = TsCreateArgs.Builder.label("k", "");

        assertThatThrownBy(() -> redis.tsCreate(MY_KEY, args)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: Couldn't parse LABELS");
    }

    // ------------------------------------------------------------------------------------------------------------
    // B-2: UTF-8 label/key round trip. Confirmed live: `TS.CREATE 온도:1 LABELS city 서울 emoji 🌡` succeeds and
    // `TS.INFO` echoes back the exact same bytes for both the label values and (via key redirection) the key.
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void unicodeLabelsAndKeyRoundTripThroughInfo() {
        String key = "온도:1";
        TsCreateArgs args = TsCreateArgs.Builder.label("city", "서울").label("emoji", "🌡");

        assertThat(redis.tsCreate(key, args)).isEqualTo("OK");

        TsInfoValue<String> info = redis.tsInfo(key);
        assertThat(info.getLabels()).containsEntry("city", "서울").containsEntry("emoji", "🌡");
    }

    // ------------------------------------------------------------------------------------------------------------
    // B-3: timestamp boundaries. Confirmed live:
    // `TS.ADD k 0 5.0` -> succeeds, `TS.GET` == (0, 5.0);
    // `TS.ADD k -1 5.0` -> "ERR TSDB: invalid timestamp, must be a nonnegative integer";
    // `TS.ADD k 9223372036854775807 5.0` -> succeeds, `TS.GET` == (Long.MAX_VALUE, 5.0).
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void timestampZeroIsValid() {
        assertThat(redis.tsAdd(MY_KEY, 0, 5.0)).isEqualTo(0L);

        TsSample sample = redis.tsGet(MY_KEY);
        assertThat(sample.getTimestamp()).isEqualTo(0L);
        assertThat(sample.getValue()).isEqualTo(5.0);
    }

    @Test
    void negativeTimestampIsRejected() {
        assertThatThrownBy(() -> redis.tsAdd(MY_KEY, -1, 5.0)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: invalid timestamp, must be a nonnegative integer");
    }

    @Test
    void maxLongTimestampIsValid() {
        assertThat(redis.tsAdd(MY_KEY, Long.MAX_VALUE, 5.0)).isEqualTo(Long.MAX_VALUE);

        TsSample sample = redis.tsGet(MY_KEY);
        assertThat(sample.getTimestamp()).isEqualTo(Long.MAX_VALUE);
        assertThat(sample.getValue()).isEqualTo(5.0);
    }

    // ------------------------------------------------------------------------------------------------------------
    // B-4: CHUNK_SIZE boundaries. Confirmed live: 48 and 1048576 (the documented [48..1048576] range ends) succeed;
    // 47 and 50 (not a multiple of 8) both fail with
    // "ERR TSDB: CHUNK_SIZE value must be a multiple of 8 in the range [48 .. 1048576]".
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void chunkSizeAtLowerBoundIsValid() {
        assertThat(redis.tsCreate(MY_KEY, TsCreateArgs.Builder.chunkSize(48))).isEqualTo("OK");
    }

    @Test
    void chunkSizeAtUpperBoundIsValid() {
        assertThat(redis.tsCreate(MY_KEY, TsCreateArgs.Builder.chunkSize(1048576))).isEqualTo("OK");
    }

    @Test
    void chunkSizeBelowLowerBoundIsRejected() {
        assertThatThrownBy(() -> redis.tsCreate(MY_KEY, TsCreateArgs.Builder.chunkSize(47)))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: CHUNK_SIZE value must be a multiple of 8 in the range [48 .. 1048576]");
    }

    @Test
    void chunkSizeNotAMultipleOfEightIsRejected() {
        assertThatThrownBy(() -> redis.tsCreate(MY_KEY, TsCreateArgs.Builder.chunkSize(50)))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: CHUNK_SIZE value must be a multiple of 8 in the range [48 .. 1048576]");
    }

}
