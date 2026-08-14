package io.lettuce.core.support.caching;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.TrackingArgs;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for server-side assisted cache invalidation using Redis Cluster.
 *
 * @author Julien Ruaux
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@EnabledOnCommand("ACL")
public class ClusterClientsideCachingIntegrationTests {

    private final RedisClusterClient clusterClient;

    @Inject
    public ClusterClientsideCachingIntegrationTests(RedisClusterClient clusterClient) {
        this.clusterClient = clusterClient;
    }

    @BeforeEach
    void setUp() {

        try (StatefulRedisClusterConnection<String, String> connection = clusterClient.connect()) {
            connection.sync().flushall();
        }
    }

    @Test
    void serverAssistedCachingShouldUseClientCache() {

        Map<String, String> clientCache = new ConcurrentHashMap<>();

        StatefulRedisClusterConnection<String, String> otherParty = clusterClient.connect();
        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

        CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
                TrackingArgs.Builder.enabled());

        String key = "key";
        otherParty.sync().set(key, "value");

        assertThat(frontend.get(key)).isEqualTo("value");
        assertThat(clientCache).hasSize(1);

        otherParty.close();
        frontend.close();
    }

    @Test
    void serverAssistedCachingShouldInvalidateAcrossAllNodes() {

        Map<String, String> clientCache = new ConcurrentHashMap<>();

        StatefulRedisClusterConnection<String, String> otherParty = clusterClient.connect();
        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

        CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
                TrackingArgs.Builder.enabled());

        // Invalidation messages are emitted by the node serving a key's slot, so exercise a key on
        // each upstream node.
        for (RedisClusterNode node : connection.getPartitions()) {

            if (!node.is(RedisClusterNode.NodeFlag.UPSTREAM)) {
                continue;
            }

            String key = keyOwnedBy(connection, node);

            otherParty.sync().set(key, "value");
            assertThat(frontend.get(key)).isEqualTo("value");
            assertThat(clientCache).containsKey(key);

            otherParty.sync().set(key, "changed");

            Wait.untilTrue(() -> !clientCache.containsKey(key)).waitOrTimeout();
            assertThat(frontend.get(key)).isEqualTo("changed");
        }

        otherParty.close();
        frontend.close();
    }

    @Test
    void serverAssistedCachingShouldClearOnFlush() {

        Map<String, String> clientCache = new ConcurrentHashMap<>();

        StatefulRedisClusterConnection<String, String> otherParty = clusterClient.connect();
        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

        CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
                TrackingArgs.Builder.enabled());

        String key = "key";
        otherParty.sync().set(key, "value");

        assertThat(frontend.get(key)).isEqualTo("value");
        assertThat(clientCache).hasSize(1);

        otherParty.sync().flushall();

        Wait.untilTrue(clientCache::isEmpty).waitOrTimeout();
        assertThat(frontend.get(key)).isNull();

        otherParty.close();
        frontend.close();
    }

    private static String keyOwnedBy(StatefulRedisClusterConnection<String, String> connection, RedisClusterNode node) {

        return IntStream.range(0, SlotHash.SLOT_COUNT).mapToObj(i -> "key-" + i)
                .filter(key -> node.hasSlot(SlotHash.getSlot(key))).findFirst()
                .orElseThrow(() -> new IllegalStateException("No key found for node " + node.getNodeId()));
    }

}
