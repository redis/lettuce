/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.HotkeysArgs;
import io.lettuce.core.HotkeysReply;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisServerCommands} HOTKEYS commands.
 *
 * @author Aleksandar Todorov
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("HOTKEYS")
public class HotkeysCommandIntegrationTests extends TestSupport {

    protected RedisCommands<String, String> redis;

    @Inject
    protected HotkeysCommandIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeAll
    void setUp() {
        clearState();
    }

    @AfterEach
    void tearDown() {
        clearState();
    }

    private void clearState() {
        redis.flushall();
        redis.hotkeysStop();
        redis.hotkeysReset();
    }

    /**
     * Tests the complete HOTKEYS lifecycle: START -> operations -> STOP -> restart clears data -> RESET.
     */
    @Test
    void hotkeysLifecycle() {
        // START tracking
        redis.hotkeysStart(HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU));
        redis.set("key1", "value1");
        redis.get("key1");

        // STOP - tracking inactive but data preserved
        redis.hotkeysStop();
        HotkeysReply reply = redis.hotkeysGet();
        assertThat(reply.isTrackingActive()).isFalse();
        assertThat(reply.getByCpuTimeUs()).containsKey("key1");

        // Restart clears previous data
        redis.hotkeysStart(HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU));
        redis.set("key2", "val2");
        reply = redis.hotkeysGet();
        assertThat(reply.isTrackingActive()).isTrue();
        assertThat(reply.getByCpuTimeUs()).containsKey("key2");
        assertThat(reply.getByCpuTimeUs()).doesNotContainKey("key1");

        // RESET clears all data
        redis.hotkeysStop();
        assertThat(redis.hotkeysReset()).isEqualTo("OK");
    }

    /**
     * Tests that both CPU and NET metrics detect hotspots correctly.
     */
    @Test
    void hotkeysBothMetrics() {
        redis.hotkeysStart(HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU, HotkeysArgs.Metric.NET).sample(1));

        // CPU hotspot: frequently accessed key
        String cpuHot = "stats:counter";
        for (int i = 0; i < 20; i++) {
            redis.incr(cpuHot);
        }

        // NET hotspot: large value
        String netHot = "blob:data";
        char[] largeChars = new char[6000];
        Arrays.fill(largeChars, 'x');
        redis.set(netHot, new String(largeChars));
        redis.get(netHot);

        HotkeysReply reply = redis.hotkeysGet();

        // Verify CPU hotspot detected
        assertThat(reply.getByCpuTimeUs()).containsKey(cpuHot);
        assertThat(reply.getByCpuTimeUs().get(cpuHot)).isGreaterThan(0L);

        // Verify NET hotspot detected
        assertThat(reply.getByNetBytes()).containsKey(netHot);
        assertThat(reply.getByNetBytes().get(netHot)).isGreaterThan(6000L);

        redis.hotkeysStop();
    }

    /**
     * Tests SAMPLE, COUNT, and DURATION options.
     */
    @Test
    void hotkeysStartOptions() {
        // Test SAMPLE option
        redis.hotkeysStart(HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU).sample(5));
        for (int i = 0; i < 20; i++) {
            redis.set("samplekey" + i, "value" + i);
        }
        HotkeysReply reply = redis.hotkeysGet();
        assertThat(reply.getSampleRatio()).isEqualTo(5);
        assertThat(reply.getByCpuTimeUs().size()).isLessThan(20);
        redis.hotkeysStop();
        redis.hotkeysReset();

        // Test COUNT option (min 10, max 64)
        redis.hotkeysStart(HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU).count(10));
        for (int i = 1; i <= 25; i++) {
            redis.set("countkey" + i, "value" + i);
        }
        reply = redis.hotkeysGet();
        assertThat(reply.getByCpuTimeUs().size()).isLessThanOrEqualTo(10);
        redis.hotkeysStop();
        redis.hotkeysReset();

        // Test DURATION option (auto-stop) - wait for tracking to stop automatically
        redis.hotkeysStart(HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU).duration(1));
        redis.set("durationkey", "testvalue");
        await().until(() -> !redis.hotkeysGet().isTrackingActive());
        reply = redis.hotkeysGet();
        assertThat(reply.getCollectionDurationMs()).isGreaterThanOrEqualTo(1000L);
        assertThat(reply.getByCpuTimeUs()).containsKey("durationkey");
    }

    /**
     * Tests that HOTKEYS GET returns null before START is called.
     */
    @Test
    void hotkeysGetBeforeStart() {
        HotkeysReply reply = redis.hotkeysGet();
        assertThat(reply).isNull();
    }

    /**
     * Tests INFO output for Hotkeys section across all states: never started, active, stopped, and reset.
     */
    @Test
    void infoHotkeysSection() {
        // Never started - no section
        assertThat(redis.info()).doesNotContain("# Hotkeys");

        // Active - section present with tracking-active:1
        redis.hotkeysStart(HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU));
        String info = redis.info();
        assertThat(info).contains("# Hotkeys");
        assertThat(info).contains("hotkeys-tracking-active:1");

        // Stopped - section still present with tracking-active:0
        redis.hotkeysStop();
        info = redis.info();
        assertThat(info).contains("# Hotkeys");
        assertThat(info).contains("hotkeys-tracking-active:0");

        // Reset - no section
        redis.hotkeysReset();
        assertThat(redis.info()).doesNotContain("# Hotkeys");
    }

}
