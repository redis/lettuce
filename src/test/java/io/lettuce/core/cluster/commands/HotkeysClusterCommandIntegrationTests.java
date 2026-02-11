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

    // Test slots - consecutive slots (server groups them as a range) and a non-consecutive slot
    private static final int SLOT_0 = 0;

    private static final int SLOT_1 = 1;

    private static final int SLOT_2 = 2;

    private static final int SLOT_100 = 100; // Non-consecutive slot to test single slot parsing

    // Keys with hash tags that hash to slots 0, 1, 2
    // The hash tag content was found by iterating SlotHash.getSlot()
    private static final String KEY_SLOT_0 = "key{3560}"; // {3560} hashes to slot 0

    private static final String KEY_SLOT_1 = "key{22179}"; // {22179} hashes to slot 1

    private static final String KEY_SLOT_2 = "key{48756}"; // {48756} hashes to slot 2

    /**
     * Tests HOTKEYS with SLOTS parameter by connecting directly to a single cluster node. Verifies:
     * <ul>
     * <li>Consecutive slots (0, 1, 2) are grouped as a range [0, 2]</li>
     * <li>Non-consecutive slot (100) is returned as a single slot [100, 100]</li>
     * <li>All response fields are correctly parsed</li>
     * </ul>
     */
    @Test
    void hotkeysWithSlotsParameter() {
        // Verify our pre-computed keys hash to the expected slots
        assertThat(SlotHash.getSlot(KEY_SLOT_0)).isEqualTo(SLOT_0);
        assertThat(SlotHash.getSlot(KEY_SLOT_1)).isEqualTo(SLOT_1);
        assertThat(SlotHash.getSlot(KEY_SLOT_2)).isEqualTo(SLOT_2);

        // Get a node that handles slot 0 and its connection
        RedisClusterNode node = redis.upstream().asMap().keySet().stream().filter(n -> n.hasSlot(SLOT_0)).findFirst()
                .orElseThrow(() -> new RuntimeException("No node found for slot 0"));
        RedisCommands<String, String> commands = redis.upstream().asMap().get(node);

        // Start hotkeys tracking with consecutive slots (0, 1, 2) and a non-consecutive slot (100)
        // Server should group consecutive slots into a range and keep non-consecutive as single slot
        commands.hotkeysStart(HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU, HotkeysArgs.Metric.NET).sample(2)
                .slots(SLOT_0, SLOT_1, SLOT_2, SLOT_100));

        // Generate traffic on keys that hash to slots 0, 1, 2
        for (int i = 0; i < 50; i++) {
            commands.set(KEY_SLOT_0, "value" + i);
            commands.get(KEY_SLOT_0);
            commands.set(KEY_SLOT_1, "value" + i);
            commands.get(KEY_SLOT_1);
            commands.set(KEY_SLOT_2, "value" + i);
            commands.get(KEY_SLOT_2);
        }

        // Get results
        HotkeysReply reply = commands.hotkeysGet();
        assertThat(reply).isNotNull();

        // Verify tracking state
        assertThat(reply.isTrackingActive()).isTrue();
        assertThat(reply.getSampleRatio()).isEqualTo(2);

        // Verify selected slots - should have 2 entries:
        // 1. Range [0, 2] for consecutive slots 0, 1, 2
        // 2. Single slot [100, 100] for non-consecutive slot (represented as range with same start/end)
        assertThat(reply.getSelectedSlots()).hasSize(2);
        Range<Integer> firstRange = reply.getSelectedSlots().get(0);
        assertThat(firstRange.getLower().getValue()).isEqualTo(SLOT_0);
        assertThat(firstRange.getUpper().getValue()).isEqualTo(SLOT_2);
        Range<Integer> secondRange = reply.getSelectedSlots().get(1);
        assertThat(secondRange.getLower().getValue()).isEqualTo(SLOT_100);
        assertThat(secondRange.getUpper().getValue()).isEqualTo(SLOT_100);

        // Verify CPU time metrics (in microseconds)
        assertThat(reply.getSampledCommandSelectedSlotsUs()).isNotNull().isGreaterThan(0L);
        assertThat(reply.getAllCommandsSelectedSlotsUs()).isNotNull().isGreaterThan(0L);
        assertThat(reply.getAllCommandsAllSlotsUs()).isNotNull().isGreaterThan(0L);

        // Verify net bytes metrics
        assertThat(reply.getNetBytesSampledCommandsSelectedSlots()).isNotNull().isGreaterThan(0L);
        assertThat(reply.getNetBytesAllCommandsSelectedSlots()).isNotNull().isGreaterThan(0L);
        assertThat(reply.getNetBytesAllCommandsAllSlots()).isNotNull().isGreaterThan(0L);

        // Verify timing fields
        assertThat(reply.getCollectionStartTimeUnixMs()).isGreaterThan(0L);
        assertThat(reply.getCollectionDurationMs()).isGreaterThanOrEqualTo(0L);
        assertThat(reply.getTotalCpuTimeUserMs()).isGreaterThanOrEqualTo(0L);
        assertThat(reply.getTotalCpuTimeSysMs()).isGreaterThanOrEqualTo(0L);
        assertThat(reply.getTotalNetBytes()).isGreaterThan(0L);

        // Verify key metrics maps contain our 3 keys
        assertThat(reply.getByCpuTimeUs()).hasSize(3).containsKeys(KEY_SLOT_0, KEY_SLOT_1, KEY_SLOT_2);
        assertThat(reply.getByCpuTimeUs().get(KEY_SLOT_0)).isGreaterThan(0L);
        assertThat(reply.getByCpuTimeUs().get(KEY_SLOT_1)).isGreaterThan(0L);
        assertThat(reply.getByCpuTimeUs().get(KEY_SLOT_2)).isGreaterThan(0L);

        assertThat(reply.getByNetBytes()).hasSize(3).containsKeys(KEY_SLOT_0, KEY_SLOT_1, KEY_SLOT_2);
        assertThat(reply.getByNetBytes().get(KEY_SLOT_0)).isGreaterThan(0L);
        assertThat(reply.getByNetBytes().get(KEY_SLOT_1)).isGreaterThan(0L);
        assertThat(reply.getByNetBytes().get(KEY_SLOT_2)).isGreaterThan(0L);

        // Stop tracking
        commands.hotkeysStop();
        commands.hotkeysReset();
    }

}
