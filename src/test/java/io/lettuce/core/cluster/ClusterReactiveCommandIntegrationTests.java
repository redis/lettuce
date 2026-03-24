package io.lettuce.core.cluster;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import reactor.test.StepVerifier;
import io.lettuce.core.AclSetuserArgs;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.ClusterPartitionParser;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.models.slots.ClusterSlotRange;
import io.lettuce.core.cluster.models.slots.ClusterSlotsParser;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class ClusterReactiveCommandIntegrationTests {

    private final RedisClusterClient clusterClient;

    private final StatefulRedisClusterConnection<String, String> connection;

    private final RedisClusterReactiveCommands<String, String> reactive;

    private final RedisClusterCommands<String, String> sync;

    @Inject
    ClusterReactiveCommandIntegrationTests(RedisClusterClient clusterClient,
            StatefulRedisClusterConnection<String, String> connection) {
        this.clusterClient = clusterClient;
        this.connection = connection;

        this.reactive = connection.reactive();
        this.sync = connection.sync();
    }

    @Test
    void testClusterBumpEpoch() {
        StepVerifier.create(reactive.clusterBumpepoch())
                .consumeNextWith(actual -> assertThat(actual).matches("(BUMPED|STILL).*")).verifyComplete();
    }

    @Test
    void testClusterInfo() {

        StepVerifier.create(reactive.clusterInfo()).consumeNextWith(actual -> {
            assertThat(actual).contains("cluster_known_nodes:");
            assertThat(actual).contains("cluster_slots_fail:0");
            assertThat(actual).contains("cluster_state:");
        }).verifyComplete();
    }

    @Test
    void testClusterNodes() {

        StepVerifier.create(reactive.clusterNodes()).consumeNextWith(actual -> {
            assertThat(actual).contains("connected");
            assertThat(actual).contains("master");
            assertThat(actual).contains("myself");
        }).verifyComplete();
    }

    @Test
    void testAsking() {
        StepVerifier.create(reactive.asking()).expectNext("OK").verifyComplete();
    }

    @Test
    void testClusterSlots() {

        List<Object> reply = reactive.clusterSlots().collectList().block();
        assertThat(reply.size()).isGreaterThan(1);

        List<ClusterSlotRange> parse = ClusterSlotsParser.parse(reply);
        assertThat(parse).hasSize(2);

        ClusterSlotRange clusterSlotRange = parse.get(0);
        assertThat(clusterSlotRange.getFrom()).isEqualTo(0);
        assertThat(clusterSlotRange.getTo()).isEqualTo(11999);

        assertThat(clusterSlotRange.toString()).contains(ClusterSlotRange.class.getSimpleName());
    }

    @Test
    void clusterSlaves() {

        RedisClusterNode master = clusterClient.getPartitions().stream().filter(it -> it.is(RedisClusterNode.NodeFlag.UPSTREAM))
                .findFirst().get();

        List<String> result = reactive.clusterSlaves(master.getNodeId()).collectList().block();

        assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    void testClusterLinks() {
        List<Map<String, Object>> values = reactive.clusterLinks().block();
        for (Map<String, Object> value : values) {
            assertThat(value).containsKeys("direction", "node", "create-time", "events", "send-buffer-allocated",
                    "send-buffer-used");
        }
    }

    /**
     * Test that clusterMyId() falls back to CLUSTER NODES parsing when CLUSTER MYID command is not available. This simulates
     * environments like Redis Enterprise OSS cluster mode where CLUSTER MYID is not supported.
     */
    @Test
    @EnabledOnCommand("ACL") // Requires Redis 6+ for ACL support
    void clusterMyIdFallsBackToClusterNodesWhenCommandDenied() {

        String testUser = "noclustermyid_reactive";
        String testPassword = "testpass123";

        // Create a ProtocolKeyword for the MYID subcommand
        ProtocolKeyword myidSubcommand = new ProtocolKeyword() {

            @Override
            public byte[] getBytes() {
                return "MYID".getBytes();
            }

            @Override
            public String toString() {
                return "MYID";
            }

        };

        RedisAdvancedClusterCommands<String, String> advancedSync = connection.sync();

        try {
            // Create a user on ALL cluster nodes so authentication works during topology refresh
            AclSetuserArgs userArgs = AclSetuserArgs.Builder.on().addPassword(testPassword).allKeys().allChannels()
                    .allCommands().removeCommand(CommandType.CLUSTER, myidSubcommand);

            for (RedisClusterNode node : connection.getPartitions()) {
                advancedSync.getConnection(node.getNodeId()).aclSetuser(testUser, userArgs);
            }

            // Connect with the restricted user
            RedisURI restrictedUri = RedisURI.Builder.redis(ClusterTestSettings.host, ClusterTestSettings.port1)
                    .withAuthentication(testUser, testPassword.toCharArray()).build();

            try (RedisClusterClient restrictedClient = RedisClusterClient.create(restrictedUri);
                    StatefulRedisClusterConnection<String, String> restrictedConnection = restrictedClient.connect()) {

                RedisAdvancedClusterReactiveCommands<String, String> restrictedReactive = restrictedConnection.reactive();

                // This should trigger the fallback to CLUSTER NODES parsing
                StepVerifier.create(restrictedReactive.clusterMyId().zipWith(restrictedReactive.clusterNodes()))
                        .assertNext(tuple -> {
                            String nodeId = tuple.getT1();
                            String clusterNodes = tuple.getT2();

                            // Verify we got a valid node ID (the fallback worked)
                            assertThat(nodeId).isNotNull();
                            assertThat(nodeId).isNotEmpty();

                            // Verify the node ID matches what we'd get from CLUSTER NODES parsing
                            Partitions partitions = ClusterPartitionParser.parse(clusterNodes);
                            String expectedNodeId = partitions.stream().filter(n -> n.is(RedisClusterNode.NodeFlag.MYSELF))
                                    .findFirst().map(RedisClusterNode::getNodeId).orElse(null);

                            assertThat(nodeId).isEqualTo(expectedNodeId);
                        }).verifyComplete();
            }
        } finally {
            // Always clean up the test user on ALL nodes
            for (RedisClusterNode node : connection.getPartitions()) {
                advancedSync.getConnection(node.getNodeId()).aclDeluser(testUser);
            }
        }
    }

}
