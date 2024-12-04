package io.lettuce.core.cluster;

import io.lettuce.core.AclSetuserArgs;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.MyStreamingRedisCredentialsProvider;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.Executions;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.test.CanConnect;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.test.settings.TestSettings.host;
import static io.lettuce.test.settings.TestSettings.hostAddr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Ivo Gaydajiev
 */
@Tag(INTEGRATION_TEST)
class RedisClusterStreamingCredentialsProviderIntegrationTests extends TestSupport {

    private static final int CLUSTER_PORT_SSL_1 = 7442;

    private static final int CLUSTER_PORT_SSL_2 = 7444; // replica cannot replicate properly with upstream

    private static final int CLUSTER_PORT_SSL_3 = 7445;

    private static final String SLOT_1_KEY = "8HMdi";

    private static final String SLOT_16352_KEY = "UyAa4KqoWgPGKa";

    private static MyStreamingRedisCredentialsProvider credentialsProvider = new MyStreamingRedisCredentialsProvider();

    private static RedisURI redisURI = RedisURI.Builder.redis(host(), CLUSTER_PORT_SSL_1).withSsl(true)
            .withAuthentication(credentialsProvider).withVerifyPeer(false).build();

    private static RedisClusterClient redisClient = RedisClusterClient.create(TestClientResources.get(), redisURI);

    @BeforeEach
    void before() {
        assumeTrue(CanConnect.to(host(), CLUSTER_PORT_SSL_1), "Assume that stunnel runs on port 7442");
        assumeTrue(CanConnect.to(host(), CLUSTER_PORT_SSL_2), "Assume that stunnel runs on port 7444");
        assumeTrue(CanConnect.to(host(), CLUSTER_PORT_SSL_3), "Assume that stunnel runs on port 7445");
        assumeTrue(CanConnect.to(host(), 7479), "Assume that Redis runs on port 7479");
        assumeTrue(CanConnect.to(host(), 7480), "Assume that Redis runs on port 7480");
        assumeTrue(CanConnect.to(host(), 7481), "Assume that Redis runs on port 7481");
    }

    @BeforeAll
    static void beforeClass() {
        credentialsProvider.emitCredentials(TestSettings.username(), TestSettings.password().toString().toCharArray());
    }

    @AfterAll
    static void afterClass() {
        credentialsProvider.shutdown();
        FastShutdown.shutdown(redisClient);
    }

    @Test
    void defaultClusterConnectionShouldWork() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        assertThat(connection.sync().ping()).isEqualTo("PONG");

        connection.close();
    }

    @Test
    void partitionViewShouldContainClusterPorts() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        List<Integer> ports = connection.getPartitions().stream().map(redisClusterNode -> redisClusterNode.getUri().getPort())
                .collect(Collectors.toList());
        connection.close();

        assertThat(ports).contains(CLUSTER_PORT_SSL_1, CLUSTER_PORT_SSL_3);
    }

    @Test
    void routedOperationsAreWorking() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        RedisAdvancedClusterCommands<String, String> sync = connection.sync();

        sync.set(SLOT_1_KEY, "value1");
        sync.set(SLOT_16352_KEY, "value2");

        assertThat(sync.get(SLOT_1_KEY)).isEqualTo("value1");
        assertThat(sync.get(SLOT_16352_KEY)).isEqualTo("value2");

        connection.close();
    }

    @Test
    void nodeConnectionsShouldWork() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();

        // master 2
        StatefulRedisConnection<String, String> node2Connection = connection.getConnection(hostAddr(), 7445);

        try {
            node2Connection.sync().get(SLOT_1_KEY);
        } catch (RedisCommandExecutionException e) {
            assertThat(e).hasMessage("MOVED 1 127.0.0.1:7442");
        }

        connection.close();
    }

    @Test
    void nodeSelectionApiShouldWork() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();

        Executions<String> ping = connection.sync().all().commands().ping();
        assertThat(ping).hasSize(3).contains("PONG");

        connection.close();
    }

    @Test
    void shouldPerformNodeConnectionReauth() {
        ClusterClientOptions origClientOptions = redisClient.getClusterClientOptions();
        redisClient.setOptions(origClientOptions.mutate()
                .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS).build());

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        connection.getPartitions().forEach(
                partition -> createTestUser(connection.getConnection(partition.getNodeId()).sync(), "steave", "foobared"));

        credentialsProvider.emitCredentials("steave", "foobared".toCharArray());

        // Verify each node's authenticated username matches the updated credentials
        connection.getPartitions().forEach(partition -> {
            StatefulRedisConnection<String, String> userConn = connection.getConnection(partition.getNodeId());
            assertThat(userConn.sync().aclWhoami()).isEqualTo("steave");
        });

        // re-auth with the default credentials
        credentialsProvider.emitCredentials(TestSettings.username(), TestSettings.password().toString().toCharArray());

        connection.getPartitions().forEach(partition -> {
            connection.getConnection(partition.getNodeId()).sync().aclDeluser("steave");
        });

        connection.close();
    }

    public static void createTestUser(RedisCommands<String, String> commands, String username, String password) {
        commands.aclSetuser(username, AclSetuserArgs.Builder.on().allCommands().addPassword(password));
    }

}
