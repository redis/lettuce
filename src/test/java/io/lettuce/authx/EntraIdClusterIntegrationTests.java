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
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DnsResolver;
import io.lettuce.core.support.PubSubTestListener;
import io.lettuce.test.Wait;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.entraid.EntraIDTokenAuthConfigBuilder;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.lettuce.TestTags.ENTRA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag(ENTRA_ID)
public class EntraIdClusterIntegrationTests {

    private static final EntraIdTestContext testCtx = EntraIdTestContext.DEFAULT;

    private static TokenBasedRedisCredentialsProvider credentialsProvider;

    private static RedisClusterClient clusterClient;

    private static ClientResources resources;

    private static Endpoint cluster;

    @BeforeAll
    public static void setup() {
        cluster = Endpoints.DEFAULT.getEndpoint("cluster-entraid-acl");
        if (cluster != null) {
            Assumptions.assumeTrue(testCtx.getClientId() != null && testCtx.getClientSecret() != null,
                    "Skipping EntraID tests. Azure AD credentials not provided!");
            // Configure timeout options to assure fast test failover
            ClusterClientOptions clientOptions = ClusterClientOptions.builder()
                    .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).build())
                    .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(1)))
                    // enable re-authentication
                    .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS).build();

            TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder().clientId(testCtx.getClientId())
                    .secret(testCtx.getClientSecret()).authority(testCtx.getAuthority()).scopes(testCtx.getRedisScopes())
                    .expirationRefreshRatio(0.0000001F).build();

            credentialsProvider = TokenBasedRedisCredentialsProvider.create(tokenAuthConfig);

            resources = ClientResources.builder().dnsResolver(DnsResolver.jvmDefault()).build();

            RedisURI clusterUri = RedisURI.create(cluster.getEndpoints().get(0));
            clusterUri.setCredentialsProvider(credentialsProvider);
            clusterClient = RedisClusterClient.create(resources, clusterUri);
            clusterClient.setOptions(clientOptions);
        }
    }

    @AfterAll
    public static void cleanup() {
        if (credentialsProvider != null) {
            credentialsProvider.close();
        }
        if (resources != null) {
            resources.shutdown();
        }
    }

    // T.1.1
    // Verify authentication using Azure AD with service principals using Redis Cluster Client
    @Test
    public void clusterWithSecret_azureServicePrincipalIntegrationTest() throws ExecutionException, InterruptedException {
        assumeTrue(cluster != null, "Skipping EntraID tests. Redis host with enabled EntraId not provided!");

        try (StatefulRedisClusterConnection<String, String> connection = clusterClient.connect()) {
            assertThat(connection.sync().aclWhoami()).isEqualTo(cluster.getUsername());
            assertThat(connection.async().aclWhoami().get()).isEqualTo(cluster.getUsername());
            assertThat(connection.reactive().aclWhoami().block()).isEqualTo(cluster.getUsername());

            connection.getPartitions().forEach((partition) -> {
                try (StatefulRedisConnection<?, ?> nodeConnection = connection.getConnection(partition.getNodeId())) {
                    assertThat(nodeConnection.sync().aclWhoami()).isEqualTo(cluster.getUsername());
                }
            });
        }
    }

}
