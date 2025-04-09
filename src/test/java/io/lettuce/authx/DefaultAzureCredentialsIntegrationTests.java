package io.lettuce.authx;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.entraid.AzureTokenAuthConfigBuilder;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static io.lettuce.TestTags.ENTRA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag(ENTRA_ID)
public class DefaultAzureCredentialsIntegrationTests {

    private static final EntraIdTestContext testCtx = EntraIdTestContext.DEFAULT;

    private RedisClient client;

    private Endpoint standalone;

    private ClientOptions clientOptions;

    private TokenBasedRedisCredentialsProvider credentialsProvider;

    @BeforeEach
    public void setup() {
        standalone = Endpoints.DEFAULT.getEndpoint("standalone-entraid-acl");
        assumeTrue(standalone != null, "Skipping EntraID tests. Redis host with enabled EntraId not provided!");
        Assumptions.assumeTrue(testCtx.getClientId() != null && testCtx.getClientSecret() != null,
                "Skipping EntraID tests. Azure AD credentials not provided!");

        clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).build())
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(1)))
                .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS).build();

        DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();

        TokenAuthConfig tokenAuthConfig = AzureTokenAuthConfigBuilder.builder().defaultAzureCredential(credential)
                .tokenRequestExecTimeoutInMs(2000).build();

        credentialsProvider = TokenBasedRedisCredentialsProvider.create(tokenAuthConfig);

        client = createClient(credentialsProvider);
    }

    @AfterEach
    public void cleanUp() {
        if (credentialsProvider != null) {
            credentialsProvider.close();
        }
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    public void azureTokenAuthWithDefaultAzureCredentials() throws ExecutionException, InterruptedException {

        RedisCredentials credentials = credentialsProvider.resolveCredentials().block(Duration.ofSeconds(5));
        assertThat(credentials).isNotNull();

        String key = UUID.randomUUID().toString();
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisCommands<String, String> sync = connection.sync();
            assertThat(sync.aclWhoami()).isEqualTo(credentials.getUsername());
            sync.set(key, "value");
            assertThat(sync.get(key)).isEqualTo("value");
            assertThat(connection.async().get(key).get()).isEqualTo("value");
            assertThat(connection.reactive().get(key).block()).isEqualTo("value");
            sync.del(key);
        }
    }

    private RedisClient createClient(TokenBasedRedisCredentialsProvider credentialsProvider) {
        RedisURI uri = RedisURI.create((standalone.getEndpoints().get(0)));
        uri.setCredentialsProvider(credentialsProvider);
        RedisClient redis = RedisClient.create(uri);
        redis.setOptions(clientOptions);
        return redis;
    }

}
