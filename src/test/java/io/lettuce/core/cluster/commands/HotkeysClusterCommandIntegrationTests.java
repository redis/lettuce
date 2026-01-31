/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.test.LettuceExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.HotkeysArgs;
import io.lettuce.core.HotkeysReply;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisServerCommands} HOTKEYS commands using Redis Cluster. Tests
 * cluster-specific features like SLOTS filtering and validates all response fields.
 *
 * Expected HOTKEYS GET response format:
 *
 * @author Aleksandar Todorov
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("HOTKEYS")
public class HotkeysClusterCommandIntegrationTests {

    protected RedisAdvancedClusterCommands<String, String> redis;

    @Inject
    protected HotkeysClusterCommandIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        this.redis = connection.sync();
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
     * Comprehensive test that verifies SLOTS filtering and all 16 response fields. Uses SAMPLE > 1 and SLOTS to trigger all
     * conditional fields.
     */
    @Test
    void hotkeysWithSlotsAndAllFields() {
        // Get actual slot numbers for our test keys
        int fooSlot = SlotHash.getSlot("foo");
        int barSlot = SlotHash.getSlot("bar");
        int bazSlot = SlotHash.getSlot("baz");

        // Use SAMPLE > 1 and SLOTS to trigger all conditional fields
        redis.hotkeysStart(HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU, HotkeysArgs.Metric.NET).sample(2).slots(fooSlot,
                barSlot, bazSlot));

        // Generate traffic on keys that hash to the selected slots
        for (int i = 0; i < 50; i++) {
            redis.set("foo", "value" + i);
            redis.get("foo");
            redis.set("bar", "value" + i);
            redis.get("bar");
            redis.set("baz", "value" + i);
            redis.get("baz");
        }
        // Also set a key outside selected slots
        redis.set("other", "value");

        HotkeysReply reply = redis.hotkeysGet();

        // 1) tracking-active
        assertThat(reply.isTrackingActive()).isTrue();

        // 3) sample-ratio
        assertThat(reply.getSampleRatio()).isEqualTo(2);

        // 5) selected-slots - verify SLOTS filtering works
        assertThat(reply.getSelectedSlots()).containsExactlyInAnyOrder(fooSlot, barSlot, bazSlot);

        // 7) sampled-command-selected-slots-ms (conditional)
        assertThat(reply.getSampledCommandSelectedSlotsMs()).isNotNull();

        // 9) all-commands-selected-slots-ms (conditional)
        assertThat(reply.getAllCommandsSelectedSlotsMs()).isNotNull();

        // 11) all-commands-all-slots-ms
        assertThat(reply.getAllCommandsAllSlotsMs()).isNotNull();

        // 13) net-bytes-sampled-commands-selected-slots (conditional)
        assertThat(reply.getNetBytesSampledCommandsSelectedSlots()).isNotNull();

        // 15) net-bytes-all-commands-selected-slots (conditional)
        assertThat(reply.getNetBytesAllCommandsSelectedSlots()).isNotNull();

        // 17) net-bytes-all-commands-all-slots
        assertThat(reply.getNetBytesAllCommandsAllSlots()).isNotNull();
        assertThat(reply.getNetBytesAllCommandsAllSlots()).isGreaterThan(0L);

        // 19) collection-start-time-unix-ms
        assertThat(reply.getCollectionStartTimeUnixMs()).isGreaterThan(0L);

        // 21) collection-duration-ms
        assertThat(reply.getCollectionDurationMs()).isGreaterThanOrEqualTo(0L);

        // 23) total-cpu-time-user-ms
        assertThat(reply.getTotalCpuTimeUserMs()).isGreaterThanOrEqualTo(0L);

        // 25) total-cpu-time-sys-ms
        assertThat(reply.getTotalCpuTimeSysMs()).isGreaterThanOrEqualTo(0L);

        // 27) total-net-bytes
        assertThat(reply.getTotalNetBytes()).isNotNull();

        // 29) by-cpu-time - should have entries for keys in selected slots
        assertThat(reply.getByCpuTime()).isNotEmpty();
        assertThat(reply.getByCpuTime().keySet()).containsAnyOf("foo", "bar", "baz");

        // 31) by-net-bytes - should have entries for keys in selected slots
        assertThat(reply.getByNetBytes()).isNotEmpty();
        assertThat(reply.getByNetBytes().keySet()).containsAnyOf("foo", "bar", "baz");

        redis.hotkeysStop();
    }

}
