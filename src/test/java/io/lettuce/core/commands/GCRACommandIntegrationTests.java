/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.GCRAArgs;
import io.lettuce.core.GCRAResponse;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for the Redis GCRA (Generic Cell Rate Algorithm) command.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("GCRA")
public class GCRACommandIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    @Inject
    protected GCRACommandIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        redis.flushall();
    }

    @Test
    void gcraFirstRequestIsNotLimited() {
        GCRAArgs args = GCRAArgs.Builder.rate(5, 10, 60);

        GCRAResponse response = redis.gcra("rate:test", args);

        assertThat(response).isNotNull();
        assertThat(response.isLimited()).isFalse();
        assertThat(response.getMaxRequests()).isEqualTo(6); // max_burst + 1
        assertThat(response.getAvailableRequests()).isEqualTo(5);
        assertThat(response.getRetryAfter()).isEqualTo(-1);
        assertThat(response.getFullBurstAfter()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void gcraExhaustsBurstAndGetsLimited() {
        GCRAArgs args = GCRAArgs.Builder.rate(0, 1, 60);

        // First request should succeed
        GCRAResponse first = redis.gcra("rate:exhaust", args);
        assertThat(first.isLimited()).isFalse();
        assertThat(first.getMaxRequests()).isEqualTo(1); // max_burst(0) + 1
        assertThat(first.getAvailableRequests()).isEqualTo(0);

        // Second request should be limited
        GCRAResponse second = redis.gcra("rate:exhaust", args);
        assertThat(second.isLimited()).isTrue();
        assertThat(second.getRetryAfter()).isGreaterThan(0);
        assertThat(second.getAvailableRequests()).isEqualTo(0);
    }

    @Test
    void gcraWithNumRequests() {
        GCRAArgs args = GCRAArgs.Builder.rate(5, 10, 60).numRequests(3);

        GCRAResponse response = redis.gcra("rate:weighted", args);

        assertThat(response).isNotNull();
        assertThat(response.isLimited()).isFalse();
        assertThat(response.getMaxRequests()).isEqualTo(6); // max_burst + 1
        // 3 tokens consumed, so 3 available
        assertThat(response.getAvailableRequests()).isEqualTo(3);
    }

    @Test
    void gcraWithNumRequestsExceedingBurst() {
        // max_burst=2, so max_requests=3. Request 4 tokens at once.
        GCRAArgs args = GCRAArgs.Builder.rate(2, 1, 60).numRequests(4);

        GCRAResponse response = redis.gcra("rate:exceed", args);

        assertThat(response.isLimited()).isTrue();
        assertThat(response.getAvailableRequests()).isEqualTo(3);
    }

    @Test
    void gcraWithZeroNumRequestsIsRejected() {
        assertThatThrownBy(() -> GCRAArgs.Builder.rate(5, 10, 60).numRequests(0)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("numRequests must be >= 1");
    }

    @Test
    void gcraResponseToString() {
        GCRAArgs args = GCRAArgs.Builder.rate(5, 10, 60);

        GCRAResponse response = redis.gcra("rate:tostring", args);

        String str = response.toString();
        assertThat(str).contains("GCRAResponse");
        assertThat(str).contains("limited=false");
        assertThat(str).contains("maxRequests=6");
    }

    @Test
    void gcraMultipleKeysAreIndependent() {
        GCRAArgs args = GCRAArgs.Builder.rate(0, 1, 60);

        // Exhaust key1
        redis.gcra("rate:key1", args);
        GCRAResponse key1Second = redis.gcra("rate:key1", args);
        assertThat(key1Second.isLimited()).isTrue();

        // key2 should still be available
        GCRAResponse key2First = redis.gcra("rate:key2", args);
        assertThat(key2First.isLimited()).isFalse();
    }

}
