package io.lettuce.authx;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DnsResolver;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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

            resources = ClientResources.builder()
                    // .dnsResolver(DnsResolver.jvmDefault())
                    .build();

            List<RedisURI> seedURI = new ArrayList<>();
            for (String addr : cluster.getRawEndpoints().get(0).getAddr()) {
                seedURI.add(RedisURI.builder().withAuthentication(credentialsProvider).withHost(addr)
                        .withPort(cluster.getRawEndpoints().get(0).getPort()).build());
            }

            clusterClient = RedisClusterClient.create(resources, seedURI);
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

        try (StatefulRedisClusterConnection<String, String> defaultConnection = clusterClient.connect()) {
            RedisAdvancedClusterCommands<String, String> sync = defaultConnection.sync();
            String keyPrefix = UUID.randomUUID().toString();
            Map<String, String> mset = prepareMset(keyPrefix);

            assertThat(sync.mset(mset)).isEqualTo("OK");

            for (String mykey : mset.keySet()) {
                assertThat(defaultConnection.sync().get(mykey)).isEqualTo("value-" + mykey);
                assertThat(defaultConnection.async().get(mykey).get()).isEqualTo("value-" + mykey);
                assertThat(defaultConnection.reactive().get(mykey).block()).isEqualTo("value-" + mykey);
            }
            assertThat(sync.del(mset.keySet().toArray(new String[0]))).isEqualTo(mset.keySet().size());

            // Test connections to each node
            defaultConnection.getPartitions().forEach((partition) -> {
                StatefulRedisConnection<?, ?> nodeConnection = defaultConnection.getConnection(partition.getNodeId());
                assertThat(nodeConnection.sync().ping()).isEqualTo("PONG");
            });

            defaultConnection.getPartitions().forEach((partition) -> {
                StatefulRedisConnection<?, ?> nodeConnection = defaultConnection.getConnection(partition.getUri().getHost(),
                        partition.getUri().getPort());
                assertThat(nodeConnection.sync().ping()).isEqualTo("PONG");
            });
        }
    }

    Map<String, String> prepareMset(String keyPrefix) {
        Map<String, String> mset = new HashMap<>();
        for (char c = 'a'; c <= 'z'; c++) {
            String keySuffix = new String(new char[] { c, c, c }); // Generates "aaa", "bbb", etc.
            String key = String.format("%s-{%s}", keyPrefix, keySuffix);
            mset.put(key, "value-" + key);
        }
        return mset;
    }

}
