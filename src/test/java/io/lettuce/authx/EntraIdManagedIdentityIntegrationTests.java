package io.lettuce.authx;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.entraid.EntraIDTokenAuthConfigBuilder;
import redis.clients.authentication.entraid.ManagedIdentityInfo;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static io.lettuce.TestTags.ENTRA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag(ENTRA_ID)
public class EntraIdManagedIdentityIntegrationTests {

    private static final EntraIdTestContext testCtx = EntraIdTestContext.DEFAULT;

    private static RedisClient client;

    private static Endpoint standalone;

    private static Set<String> managedIdentityAudience = Collections.singleton("https://redis.azure.com");

    @BeforeAll
    public static void setup() {
        standalone = Endpoints.DEFAULT.getEndpoint("standalone-entraid-acl");
        assumeTrue(standalone != null, "Skipping test because no Redis endpoint is configured!");
    }

    @Test
    public void withUserAssignedId_azureManagedIdentityIntegrationTest() throws ExecutionException, InterruptedException {

        // enable re-authentication
        ClusterClientOptions clientOptions = ClusterClientOptions.builder()
                .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS).build();

        TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder()
                .userAssignedManagedIdentity(ManagedIdentityInfo.UserManagedIdentityType.OBJECT_ID,
                        testCtx.getUserAssignedManagedIdentity())
                .scopes(managedIdentityAudience).build();

        try (TokenBasedRedisCredentialsProvider credentialsProvider = TokenBasedRedisCredentialsProvider
                .create(tokenAuthConfig)) {

            RedisURI uri = RedisURI.create((standalone.getEndpoints().get(0)));
            uri.setCredentialsProvider(credentialsProvider);
            client = RedisClient.create(uri);
            client.setOptions(clientOptions);

            try (StatefulRedisConnection<String, String> connection = client.connect()) {
                RedisCommands<String, String> sync = connection.sync();
                String key = UUID.randomUUID().toString();
                sync.set(key, "value");
                assertThat(connection.sync().get(key)).isEqualTo("value");
                assertThat(connection.async().get(key).get()).isEqualTo("value");
                assertThat(connection.reactive().get(key).block()).isEqualTo("value");
                sync.del(key);
            }
        }
    }

    @Test
    public void withSystemAssignedId_azureManagedIdentityIntegrationTest() throws ExecutionException, InterruptedException {
        // enable re-authentication
        ClusterClientOptions clientOptions = ClusterClientOptions.builder()
                .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS).build();

        TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder().systemAssignedManagedIdentity()
                .scopes(managedIdentityAudience).build();

        try (TokenBasedRedisCredentialsProvider credentialsProvider = TokenBasedRedisCredentialsProvider
                .create(tokenAuthConfig)) {

            RedisURI uri = RedisURI.create((standalone.getEndpoints().get(0)));
            uri.setCredentialsProvider(credentialsProvider);
            client = RedisClient.create(uri);
            client.setOptions(clientOptions);

            try (StatefulRedisConnection<String, String> connection = client.connect()) {
                RedisCommands<String, String> sync = connection.sync();
                String key = UUID.randomUUID().toString();
                sync.set(key, "value");
                assertThat(connection.sync().get(key)).isEqualTo("value");
                assertThat(connection.async().get(key).get()).isEqualTo("value");
                assertThat(connection.reactive().get(key).block()).isEqualTo("value");
                sync.del(key);
            }
        }
    }

}
