package io.lettuce.authx;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.entraid.EntraIDTokenAuthConfigBuilder;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class EntraIdIntegrationTests {

    private static EntraIdTestContext testCtx = EntraIdTestContext.DEFAULT;;

    @BeforeAll
    public static void setup() {
        Assumptions.assumeTrue(testCtx.host() != null && !testCtx.host().isEmpty(),
                "Skipping EntraID tests. Redis host with enabled EntraId not provided!");
    }

    // T.1.1
    // Verify authentication using Azure AD with service principals using Redis Standalone client
    @Test
    public void standaloneWithSecret_azureServicePrincipalIntegrationTest() throws ExecutionException, InterruptedException {
        TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder().clientId(testCtx.getClientId())
                .secret(testCtx.getClientSecret()).authority(testCtx.getAuthority()).scopes(testCtx.getRedisScopes()).build();

        // Configure timeout options to assure fast test failover
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).build())
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(1)))
                .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS).build();

        try (TokenBasedRedisCredentialsProvider credentialsProvider = new TokenBasedRedisCredentialsProvider(tokenAuthConfig)) {
            RedisURI uri = RedisURI.builder().withHost(testCtx.host()).withPort(testCtx.port())
                    .withAuthentication(credentialsProvider).build();

            try (RedisClient client = RedisClient.create(uri)) {
                client.setOptions(clientOptions);

                try (StatefulRedisConnection<String, String> connection = client.connect()) {
                    assertThat(connection.sync().aclWhoami()).isEqualTo(testCtx.getSpOID());
                    assertThat(connection.async().aclWhoami().get()).isEqualTo(testCtx.getSpOID());
                    assertThat(connection.reactive().aclWhoami().block()).isEqualTo(testCtx.getSpOID());
                }
            }
        }
    }

    // T.1.1
    // Verify authentication using Azure AD with service principals using Redis Cluster Client
    @Test
    public void clusterWithSecret_azureServicePrincipalIntegrationTest() throws ExecutionException, InterruptedException {
        TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder().clientId(testCtx.getClientId())
                .secret(testCtx.getClientSecret()).authority(testCtx.getAuthority()).scopes(testCtx.getRedisScopes()).build();

        // Configure timeout options to assure fast test failover
        ClusterClientOptions clientOptions = ClusterClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).build())
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(1)))
                .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS).build();

        try (TokenBasedRedisCredentialsProvider credentialsProvider = new TokenBasedRedisCredentialsProvider(tokenAuthConfig)) {
            RedisURI uri = RedisURI.builder().withHost(testCtx.clusterHost().get(0)).withPort(testCtx.clusterPort())
                    .withAuthentication(credentialsProvider).build();

            try (RedisClusterClient client = RedisClusterClient.create(uri)) {
                client.setOptions(clientOptions);

                try (StatefulRedisClusterConnection<String, String> connection = client.connect()) {
                    assertThat(connection.sync().aclWhoami()).isEqualTo(testCtx.getSpOID());
                    assertThat(connection.async().aclWhoami().get()).isEqualTo(testCtx.getSpOID());
                    assertThat(connection.reactive().aclWhoami().block()).isEqualTo(testCtx.getSpOID());

                    connection.getPartitions().forEach((partition) -> {
                        try (StatefulRedisConnection<?, ?> nodeConnection = connection.getConnection(partition.getNodeId())) {
                            assertThat(nodeConnection.sync().aclWhoami()).isEqualTo(testCtx.getSpOID());
                        }
                    });
                }
            }
        }
    }

    // T.2.2
    // Test that the Redis client is not blocked/interrupted during token renewal.
    @Test
    public void renewalDuringOperationsTest() throws InterruptedException, ExecutionException {
        TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder().clientId(testCtx.getClientId())
                .secret(testCtx.getClientSecret()).authority(testCtx.getAuthority()).scopes(testCtx.getRedisScopes())
                .expirationRefreshRatio(0.000001F).build();

        // Configure timeout options to assure fast test failover
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).build())
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(1)))
                .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS).build();

        try (TokenBasedRedisCredentialsProvider credentialsProvider = new TokenBasedRedisCredentialsProvider(tokenAuthConfig)) {
            RedisURI uri = RedisURI.builder().withHost(testCtx.host()).withPort(testCtx.port())
                    .withAuthentication(credentialsProvider).build();

            try (RedisClient client = RedisClient.create(uri)) {
                client.setOptions(clientOptions);

                try (StatefulRedisConnection<String, String> connection = client.connect()) {

                    // Counter to track the number of command cycles
                    AtomicInteger commandCycleCount = new AtomicInteger(0);

                    // Start a thread to continuously send Redis commands
                    Thread commandThread = new Thread(() -> {
                        try {
                            RedisAsyncCommands<String, String> async = client.connect().async();
                            for (int i = 1; i <= 10; i++) {
                                // Start a transaction with SET and INCRBY commands
                                RedisFuture<String> multi = async.multi();
                                RedisFuture<String> set = async.set("key", "1");
                                RedisFuture<Long> incrby = async.incrby("key", 1);
                                RedisFuture<TransactionResult> exec = async.exec();
                                TransactionResult results = exec.get(1, TimeUnit.SECONDS);

                                // Increment the command cycle count after each execution
                                commandCycleCount.incrementAndGet();

                                // Verify the results from EXEC
                                assertThat(results).hasSize(2); // We expect 2 responses: SET and INCRBY

                                // Check the response from each command in the transaction
                                assertThat((String) results.get(0)).isEqualTo("OK"); // SET "key" = "1"
                                assertThat((Long) results.get(1)).isEqualTo(2L); // INCRBY "key" by 1, expected result is 2
                            }
                        } catch (Exception e) {
                            fail("Command execution failed during token refresh", e);
                        }
                    });

                    commandThread.start();

                    // Count token renewals directly within the main thread
                    AtomicInteger renewalCount = new AtomicInteger(0);
                    CountDownLatch latch = new CountDownLatch(10); // Wait for at least 10 token renewals

                    credentialsProvider.credentials().subscribe(cred -> {
                        latch.countDown(); // Signal each renewal as it's received
                    });

                    latch.await(1, TimeUnit.SECONDS); // Wait to reach 10 renewals
                    commandThread.join(); // Wait for the command thread to finish

                    // Verify that at least 10 command cycles were executed during the test
                    assertThat(commandCycleCount.get()).isGreaterThanOrEqualTo(10);
                }
            }
        }
    }

}
