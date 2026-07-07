/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisTDigestCommands}.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("TDIGEST.ADD")
public class RedisTDigestIntegrationTests {

    private static final String KEY = "tdigest:key";

    protected final RedisCommands<String, String> redis;

    @Inject
    protected RedisTDigestIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        redis.flushall();
    }

    @Test
    void createAndInfo() {
        assertThat(redis.tdigestCreate(KEY)).isEqualTo("OK");

        TDigestInfoValue info = redis.tdigestInfo(KEY);
        assertThat(info).isNotNull();
        assertThat(info.getCompression()).isNotNull();
        assertThat(info.getObservations()).isEqualTo(0L);
    }

    @Test
    void createWithCompression() {
        assertThat(redis.tdigestCreate(KEY, 200)).isEqualTo("OK");
        assertThat(redis.tdigestInfo(KEY).getCompression()).isEqualTo(200L);
    }

    @Test
    void addAndObservations() {
        redis.tdigestCreate(KEY);
        assertThat(redis.tdigestAdd(KEY, "1", "2", "3", "4", "5")).isEqualTo("OK");
        assertThat(redis.tdigestInfo(KEY).getObservations()).isEqualTo(5L);
    }

    @Test
    void minMax() {
        redis.tdigestCreate(KEY);
        redis.tdigestAdd(KEY, "1", "2", "3", "4", "5");

        assertThat(redis.tdigestMin(KEY)).isEqualTo(1.0);
        assertThat(redis.tdigestMax(KEY)).isEqualTo(5.0);
    }

    @Test
    void minMaxOnEmptySketchReturnNaN() {
        redis.tdigestCreate(KEY);

        assertThat(redis.tdigestMin(KEY)).isNaN();
        assertThat(redis.tdigestMax(KEY)).isNaN();
    }

    @Test
    void quantile() {
        redis.tdigestCreate(KEY);
        redis.tdigestAdd(KEY, "1", "2", "3", "4", "5");

        List<Double> result = redis.tdigestQuantile(KEY, 0.0, 1.0);
        assertThat(result).containsExactly(1.0, 5.0);
    }

    @Test
    void cdf() {
        redis.tdigestCreate(KEY);
        redis.tdigestAdd(KEY, "1", "2", "3", "4", "5");

        List<Double> result = redis.tdigestCDF(KEY, "3");
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isBetween(0.0, 1.0);
    }

    @Test
    void byRankAndByRevRank() {
        redis.tdigestCreate(KEY);
        redis.tdigestAdd(KEY, "1", "2", "3", "4", "5");

        assertThat(redis.tdigestByRank(KEY, 0)).containsExactly(1.0);
        assertThat(redis.tdigestByRevRank(KEY, 0)).containsExactly(5.0);
    }

    @Test
    void byRankOutOfRangeReturnsInfinity() {
        redis.tdigestCreate(KEY);
        redis.tdigestAdd(KEY, "1", "2", "3");

        // rank beyond the number of observations is estimated as +inf
        assertThat(redis.tdigestByRank(KEY, 100)).containsExactly(Double.POSITIVE_INFINITY);
    }

    @Test
    void rankAndRevRank() {
        redis.tdigestCreate(KEY);
        redis.tdigestAdd(KEY, "1", "2", "3", "4", "5");

        assertThat(redis.tdigestRank(KEY, "3")).hasSize(1);
        assertThat(redis.tdigestRevRank(KEY, "3")).hasSize(1);
    }

    @Test
    void trimmedMean() {
        redis.tdigestCreate(KEY);
        redis.tdigestAdd(KEY, "1", "2", "3", "4", "5");

        assertThat(redis.tdigestTrimmedMean(KEY, 0.0, 1.0)).isEqualTo(3.0);
    }

    @Test
    void reset() {
        redis.tdigestCreate(KEY);
        redis.tdigestAdd(KEY, "1", "2", "3");
        assertThat(redis.tdigestReset(KEY)).isEqualTo("OK");
        assertThat(redis.tdigestInfo(KEY).getObservations()).isEqualTo(0L);
    }

    @Test
    void merge() {
        redis.tdigestCreate("{td}src1");
        redis.tdigestCreate("{td}src2");
        redis.tdigestAdd("{td}src1", "1", "2", "3");
        redis.tdigestAdd("{td}src2", "4", "5", "6");

        assertThat(redis.tdigestMerge("{td}dest", "{td}src1", "{td}src2")).isEqualTo("OK");
        assertThat(redis.tdigestInfo("{td}dest").getObservations()).isEqualTo(6L);
    }

    @Test
    void mergeWithCompressionAndOverride() {
        redis.tdigestCreate("{td}dest");
        redis.tdigestCreate("{td}src");
        redis.tdigestAdd("{td}src", "1", "2", "3");

        assertThat(redis.tdigestMerge("{td}dest", 100, true, "{td}src")).isEqualTo("OK");
        assertThat(redis.tdigestInfo("{td}dest").getObservations()).isEqualTo(3L);
    }

}
