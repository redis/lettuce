package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.cluster.PartitionsConsensusTestSupport.createMap;
import static io.lettuce.core.cluster.PartitionsConsensusTestSupport.createNode;
import static io.lettuce.core.cluster.PartitionsConsensusTestSupport.createPartitions;
import static io.lettuce.core.cluster.PartitionsConsensusTestSupport.createSlots;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.resource.FastShutdown;

/**
 * Unit tests for {@link RedisClusterClient#determinePartitions}.
 *
 * @author Tony Zhang
 */
@Tag(UNIT_TEST)
class RedisClusterClientDeterminePartitionsUnitTests {

    private final ClientResources clientResources = ClientResources.builder().build();

    private final RedisClusterClient client = RedisClusterClient.create(clientResources, RedisURI.create("localhost", 6379));

    @AfterEach
    void tearDown() {
        FastShutdown.shutdown(client);
        FastShutdown.shutdown(clientResources);
    }

    @Test
    void shouldNotConsiderViewsWithoutFullSlotCoverage() {

        Partitions current = createPartitions(createNode(1), createNode(2), createNode(3));

        Partitions fullCoverage = createPartitions(createNode(1, createSlots(0, SlotHash.SLOT_COUNT / 2)),
                createNode(2, createSlots(SlotHash.SLOT_COUNT / 2, SlotHash.SLOT_COUNT)), createNode(3));

        // View as reported by a node that just joined the cluster: same nodes, but a master without slots.
        Partitions partialCoverage = createPartitions(createNode(1, createSlots(0, SlotHash.SLOT_COUNT / 2)), createNode(2),
                createNode(3));

        Map<RedisURI, Partitions> topologyViews = createMap(partialCoverage, fullCoverage);

        for (int i = 0; i < 10; i++) {
            assertThat(client.determinePartitions(current, topologyViews)).isSameAs(fullCoverage);
        }
    }

    @Test
    void shouldNotConsiderViewsWithoutFullSlotCoverageOnInitialLoad() {

        Partitions fullCoverage = createPartitions(createNode(1, createSlots(0, SlotHash.SLOT_COUNT)), createNode(2),
                createNode(3));

        Partitions partialCoverage = createPartitions(createNode(1, createSlots(0, 100)), createNode(2), createNode(3));

        Map<RedisURI, Partitions> topologyViews = createMap(partialCoverage, fullCoverage);

        for (int i = 0; i < 10; i++) {
            assertThat(client.determinePartitions(null, topologyViews)).isSameAs(fullCoverage);
        }
    }

    @Test
    void shouldConsiderAllViewsIfNoneHasFullSlotCoverage() {

        Partitions current = createPartitions(createNode(1), createNode(2));

        Partitions partitions1 = createPartitions(createNode(1, createSlots(0, 100)), createNode(2));
        Partitions partitions2 = createPartitions(createNode(1), createNode(2, createSlots(100, 200)));

        Map<RedisURI, Partitions> topologyViews = createMap(partitions1, partitions2);

        Partitions result = client.determinePartitions(current, topologyViews);

        assertThat(Arrays.asList(partitions1, partitions2)).contains(result);
    }

    @Test
    void shouldReturnViewWithoutFullSlotCoverageOnInitialLoad() {

        Partitions partialCoverage = createPartitions(createNode(1, createSlots(0, 100)), createNode(2));

        Map<RedisURI, Partitions> topologyViews = createMap(partialCoverage);

        assertThat(client.determinePartitions(null, topologyViews)).isSameAs(partialCoverage);
    }

}
