/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.inject.Inject;

import io.lettuce.core.HotkeysArgs;
import io.lettuce.core.HotkeysReply;
import io.lettuce.core.Range;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.Executions;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration tests for HOTKEYS commands using Redis Cluster. Verifies that HOTKEYS commands are not supported on the cluster
 * client directly but work via NodeSelection API.
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
        redis.upstream().commands().hotkeysStop();
        redis.upstream().commands().hotkeysReset();
    }

    /**
     * Verifies that hotkeysStart throws UnsupportedOperationException on cluster client.
     */
    @Test
    void hotkeysStartThrowsOnClusterClient() {
        assertThatThrownBy(() -> redis.hotkeysStart(HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("HOTKEYS commands are not supported on cluster client");
    }

    /**
     * Verifies that hotkeysStop throws UnsupportedOperationException on cluster client.
     */
    @Test
    void hotkeysStopThrowsOnClusterClient() {
        assertThatThrownBy(() -> redis.hotkeysStop()).isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("HOTKEYS commands are not supported on cluster client");
    }

    /**
     * Verifies that hotkeysReset throws UnsupportedOperationException on cluster client.
     */
    @Test
    void hotkeysResetThrowsOnClusterClient() {
        assertThatThrownBy(() -> redis.hotkeysReset()).isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("HOTKEYS commands are not supported on cluster client");
    }

    /**
     * Verifies that hotkeysGet throws UnsupportedOperationException on cluster client.
     */
    @Test
    void hotkeysGetThrowsOnClusterClient() {
        assertThatThrownBy(() -> redis.hotkeysGet()).isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("HOTKEYS commands are not supported on cluster client");
    }

    /**
     * Verifies that HOTKEYS commands work via NodeSelection API.
     */
    @Test
    void hotkeysWorksViaNodeSelectionApi() {
        // Start on all masters via NodeSelection
        redis.upstream().commands()
                .hotkeysStart(HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU, HotkeysArgs.Metric.NET).sample(2));

        // Generate traffic on keys
        for (int i = 0; i < 50; i++) {
            redis.set("foo", "value" + i);
            redis.get("foo");
            redis.set("bar", "value" + i);
            redis.get("bar");
            redis.set("baz", "value" + i);
            redis.get("baz");
        }

        // Get results from all masters
        Executions<HotkeysReply> executions = redis.upstream().commands().hotkeysGet();
        assertThat(executions).isNotEmpty();

        // Verify at least one node has tracking data
        HotkeysReply reply = executions.stream().filter(r -> r != null && r.isTrackingActive()).findFirst().orElse(null);
        assertThat(reply).isNotNull();

        // 1) tracking-active
        assertThat(reply.isTrackingActive()).isTrue();

        // 3) sample-ratio
        assertThat(reply.getSampleRatio()).isEqualTo(2);

        // 5) selected-slots - returns ranges; each node returns its own slot range (not full 0-16383)
        assertThat(reply.getSelectedSlots()).isNotEmpty();
        Range<Integer> firstRange = reply.getSelectedSlots().get(0);
        // Verify the range structure is valid (start <= end, within valid slot range)
        assertThat(firstRange.getLower().getValue()).isGreaterThanOrEqualTo(0);
        assertThat(firstRange.getUpper().getValue()).isLessThanOrEqualTo(16383);
        assertThat(firstRange.getLower().getValue()).isLessThanOrEqualTo(firstRange.getUpper().getValue());

        // 11) all-commands-all-slots-us
        assertThat(reply.getAllCommandsAllSlotsUs()).isNotNull();

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

        // 29) by-cpu-time-us - should have entries for keys
        assertThat(reply.getByCpuTimeUs()).isNotEmpty();

        // 31) by-net-bytes - should have entries for keys
        assertThat(reply.getByNetBytes()).isNotEmpty();

        redis.upstream().commands().hotkeysStop();
    }

    /**
     * Verifies that HOTKEYS commands work via getConnection(nodeId) for node-specific connections.
     */
    @Test
    void hotkeysWorksViaNodeConnection() {
        // Get a specific node connection
        String nodeId = redis.getStatefulConnection().getPartitions().getPartition(0).getNodeId();
        RedisClusterCommands<String, String> nodeCommands = redis.getConnection(nodeId);

        // Start tracking on this specific node
        String startResult = nodeCommands.hotkeysStart(HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU));
        assertThat(startResult).isEqualTo("OK");

        // Generate some traffic
        for (int i = 0; i < 10; i++) {
            redis.set("test-key", "value" + i);
        }

        // Get results from this node
        HotkeysReply reply = nodeCommands.hotkeysGet();
        assertThat(reply).isNotNull();
        assertThat(reply.isTrackingActive()).isTrue();

        // Stop and reset
        nodeCommands.hotkeysStop();
        nodeCommands.hotkeysReset();
    }

    /**
     * Verifies that HOTKEYS commands work with specific slots parameter. This test:
     * <ol>
     * <li>Selects a node and gets slots from its range</li>
     * <li>Starts HOTKEYS tracking with those specific slots</li>
     * <li>Generates traffic on keys that hash to those slots</li>
     * <li>Verifies the reply contains the expected slot ranges and metrics</li>
     * </ol>
     */
    @Test
    void hotkeysWorksWithSlotsParameter() {
        // Get a node that handles slot 123 and its connection
        RedisClusterNode node = redis.upstream().asMap().keySet().stream().filter(n -> n.hasSlot(123)).findFirst()
                .orElseThrow(() -> new RuntimeException("No node found for slot 123"));
        RedisCommands<String, String> commands = redis.upstream().asMap().get(node);

        // Get the first 3 slots from this node's range
        int slot1 = node.getSlots().get(0);
        int slot2 = node.getSlots().get(1);
        int slot3 = node.getSlots().get(2);

        // Start tracking with specific slots from this node's range
        commands.hotkeysStart(HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU, HotkeysArgs.Metric.NET).sample(2).slots(slot1,
                slot2, slot3));

        // Find keys that hash to the monitored slots
        String key1 = findKeyForSlot(slot1);
        String key2 = findKeyForSlot(slot2);
        String key3 = findKeyForSlot(slot3);

        // Generate traffic on those keys - they will be tracked since we're monitoring their slots
        for (int i = 0; i < 50; i++) {
            commands.set(key1, "value" + i);
            commands.get(key1);
            commands.set(key2, "value" + i);
            commands.get(key2);
            commands.set(key3, "value" + i);
            commands.get(key3);
        }

        // Get results
        HotkeysReply reply = commands.hotkeysGet();

        // Verify tracking is active
        assertThat(reply).isNotNull();
        assertThat(reply.isTrackingActive()).isTrue();
        assertThat(reply.getSampleRatio()).isEqualTo(2);

        // Verify selected-slots contains the range we specified
        assertThat(reply.getSelectedSlots()).isNotEmpty();
        Range<Integer> firstRange = reply.getSelectedSlots().get(0);
        assertThat(firstRange.getLower().getValue()).isEqualTo(slot1);
        assertThat(firstRange.getUpper().getValue()).isEqualTo(slot3);

        // Verify CPU time metrics (in microseconds)
        assertThat(reply.getSampledCommandSelectedSlotsUs()).isNotNull();
        assertThat(reply.getSampledCommandSelectedSlotsUs()).isGreaterThan(0L);

        // Stop tracking
        commands.hotkeysStop();
        commands.hotkeysReset();
    }

    /**
     * Find a key that hashes to the specified slot.
     */
    private String findKeyForSlot(int targetSlot) {
        for (int i = 0; i < 100000; i++) {
            String key = "key" + i;
            if (SlotHash.getSlot(key) == targetSlot) {
                return key;
            }
        }
        throw new IllegalStateException("Could not find key for slot " + targetSlot);
    }

}
