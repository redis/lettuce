package io.lettuce.core.cluster.topology;

import io.lettuce.category.SlowTests;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTestSettings;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.lettuce.TestTags.INTEGRATION_TEST;

@Tag(INTEGRATION_TEST)
@SuppressWarnings({ "unchecked" })
@SlowTests
@ExtendWith(LettuceExtension.class)
class RepeatedRefreshPartitionsTest {

    private static final String host = TestSettings.hostAddr();

    private static final Logger log = LoggerFactory.getLogger(RepeatedRefreshPartitionsTest.class);

    private final RedisClient client;

    private RedisClusterClient clusterClient;

    private RedisCommands<String, String> redis1;

    private RedisCommands<String, String> redis2;

    private RedisCommands<String, String> redis3;

    @Inject
    RepeatedRefreshPartitionsTest(RedisClient client) {
        this.client = client;
    }

    @BeforeEach
    void openConnection() {
        clusterClient = RedisClusterClient.create(client.getResources(),
                RedisURI.Builder.redis(host, ClusterTestSettings.port1).build());
        redis1 = client.connect(RedisURI.Builder.redis(ClusterTestSettings.host, ClusterTestSettings.port1).build()).sync();
        redis2 = client.connect(RedisURI.Builder.redis(ClusterTestSettings.host, ClusterTestSettings.port2).build()).sync();
        redis3 = client.connect(RedisURI.Builder.redis(ClusterTestSettings.host, ClusterTestSettings.port3).build()).sync();
    }

    @AfterEach
    void closeConnection() {
        redis1.getStatefulConnection().close();
        redis2.getStatefulConnection().close();
        redis3.getStatefulConnection().close();
        FastShutdown.shutdown(clusterClient);
    }

    /**
     * Test to measure the number of times the slotCache reference is changed via updateCache() invocation.
     */
    @Test
    public void countUpdateCacheInvocations() throws Exception {

        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(true).refreshPeriod(10, TimeUnit.NANOSECONDS)
                .enableAdaptiveRefreshTrigger(ClusterTopologyRefreshOptions.RefreshTrigger.MOVED_REDIRECT).build();

        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());

        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

        Partitions partitions = clusterClient.getPartitions();

        Field slotCacheField = Partitions.class.getDeclaredField("slotCache");
        slotCacheField.setAccessible(true);

        Object previousSlotCache = slotCacheField.get(partitions);
        int updateCount = 0;
        long endTime = System.currentTimeMillis() + 100; // Measure for 0.1 seconds

        while (System.currentTimeMillis() < endTime) {
            Object currentSlotCache = slotCacheField.get(partitions);
            if (currentSlotCache != previousSlotCache) {
                updateCount++;
                previousSlotCache = currentSlotCache;
            }
        }

        Assertions.assertEquals(0, updateCount, "updateCache() should not be called");
        connection.close();
    }

    /**
     * Test to measure the number of times the slotCache reference is changed via updateCache() invocation when a topology
     * change is detected.
     */
    @Test
    public void countUpdateCacheInvocationsWhenTopologyChanges() throws Exception {

        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(true).refreshPeriod(10, TimeUnit.NANOSECONDS)
                .enableAdaptiveRefreshTrigger(ClusterTopologyRefreshOptions.RefreshTrigger.MOVED_REDIRECT).build();

        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());

        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

        Partitions partitions = clusterClient.getPartitions();

        Field slotCacheField = Partitions.class.getDeclaredField("slotCache");
        slotCacheField.setAccessible(true);

        Object baselineSlotCache = slotCacheField.get(partitions);

        List<RedisClusterNode> currentPartitions = partitions.getPartitions();
        List<RedisClusterNode> newPartitions = new ArrayList<>();

        for (RedisClusterNode node : currentPartitions) {
            newPartitions.add(node.clone());
        }
        if (!newPartitions.isEmpty()) {
            newPartitions.remove(0);
        }

        partitions.reload(newPartitions);

        Object updatedSlotCache = slotCacheField.get(partitions);
        Assertions.assertNotEquals(updatedSlotCache, baselineSlotCache, "new slotCache should be different");

        // --- Measure subsequent updateCache() invocation count ---
        // Although topology change is not continuously happening, for testing purposes, call reload() once more to observe
        // changes.
        Object previousSlotCache = updatedSlotCache;

        List<RedisClusterNode> newPartitions2 = new ArrayList<>();
        for (RedisClusterNode node : currentPartitions) {
            newPartitions2.add(node.clone());
        }

        if (newPartitions2.size() > 1) {
            newPartitions2.remove(1);
        }
        partitions.reload(newPartitions2);

        int updateCount = 0;
        long endTime = System.currentTimeMillis() + 100; // Measure for 0.1 seconds

        while (System.currentTimeMillis() < endTime) {
            Object currentSlotCache = slotCacheField.get(partitions);
            if (currentSlotCache != previousSlotCache) {
                updateCount++;
                previousSlotCache = currentSlotCache;
            }
        }

        Assertions.assertTrue(updateCount > 0, "updateCache() should be called at least once");

        connection.close();
    }

}
