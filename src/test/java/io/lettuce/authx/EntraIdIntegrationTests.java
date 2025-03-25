package io.lettuce.authx;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.support.PubSubTestListener;
import io.lettuce.test.Wait;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;
import org.junit.jupiter.api.*;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.entraid.AzureTokenAuthConfigBuilder;
import redis.clients.authentication.entraid.EntraIDTokenAuthConfigBuilder;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.lettuce.TestTags.ENTRA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag(ENTRA_ID)
public class EntraIdIntegrationTests {

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

        TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder().clientId(testCtx.getClientId())
                .secret(testCtx.getClientSecret()).authority(testCtx.getAuthority()).scopes(testCtx.getRedisScopes())
                .expirationRefreshRatio(0.0000001F).build();

        TokenBasedRedisCredentialsProvider credentialsProvider = TokenBasedRedisCredentialsProvider.create(tokenAuthConfig);

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

    // T.1.1
    // Verify authentication using Azure AD with service principals using Redis Standalone client
    @Test
    public void standaloneWithSecret_azureServicePrincipalIntegrationTest() throws ExecutionException, InterruptedException {
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

    // T.2.2
    // Test that the Redis client is not blocked/interrupted during token renewal.
    @Test
    public void renewalDuringOperationsTest() throws InterruptedException {
        AtomicInteger commandCycleCount = new AtomicInteger(0);

        Thread commandThread = new Thread(() -> {
            try (StatefulRedisConnection<String, String> connection = client.connect()) {
                RedisAsyncCommands<String, String> async = connection.async();
                for (int i = 1; i <= 10; i++) {
                    async.multi();
                    async.set("key", "1");
                    async.incrby("key", 1);
                    RedisFuture<TransactionResult> exec = async.exec();
                    TransactionResult results = exec.get(1, TimeUnit.SECONDS);

                    commandCycleCount.incrementAndGet();

                    assertThat(results).hasSize(2);
                    assertThat((String) results.get(0)).isEqualTo("OK");
                    assertThat((Long) results.get(1)).isEqualTo(2L);
                }
            } catch (Exception e) {
                fail("Command execution failed during token refresh", e);
            }
        });

        commandThread.start();

        CountDownLatch latch = new CountDownLatch(10); // Wait for at least 10 token renewalss
        credentialsProvider.credentials().subscribe(cred -> latch.countDown());

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue(); // Wait to reach 10 renewals
        commandThread.join(); // Wait for the command thread to finish

        assertThat(commandCycleCount.get()).isGreaterThanOrEqualTo(10);
    }

    // T.2.2
    // Test basic Pub/Sub functionality is not blocked/interrupted during token renewal.
    @Test
    public void renewalDuringPubSubOperationsTest() throws InterruptedException {
        assumeTrue(standalone != null, "Skipping EntraID tests. Redis host with enabled EntraId not provided!");

        try (StatefulRedisPubSubConnection<String, String> connectionPubSub = client.connectPubSub();
                StatefulRedisPubSubConnection<String, String> connectionPubSub1 = client.connectPubSub()) {

            PubSubTestListener listener = new PubSubTestListener();
            connectionPubSub.addListener(listener);
            connectionPubSub.sync().subscribe("channel");

            Thread pubsubThread = new Thread(() -> {
                for (int i = 1; i <= 100; i++) {
                    connectionPubSub1.sync().publish("channel", "message");
                }
            });

            pubsubThread.start();

            CountDownLatch latch = new CountDownLatch(10);
            credentialsProvider.credentials().subscribe(cred -> latch.countDown());

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue(); // Wait for at least 10 token renewals
            pubsubThread.join(); // Wait for the pub/sub thread to finish

            Wait.untilEquals(100, () -> listener.getMessages().size()).waitOrTimeout();
            assertThat(listener.getMessages()).allMatch(msg -> msg.equals("message"));
        }
    }

    @Test
    public void azureTokenAuthWithDefaultAzureCredentials() throws ExecutionException, InterruptedException {
        DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();

        TokenAuthConfig tokenAuthConfig = AzureTokenAuthConfigBuilder.builder().defaultAzureCredential(credential)
                .tokenRequestExecTimeoutInMs(2000).build();

        try (RedisClient azureCredClient = createClient(credentialsProvider);
                TokenBasedRedisCredentialsProvider credentialsProvider = TokenBasedRedisCredentialsProvider
                        .create(tokenAuthConfig);) {
            RedisCredentials credentials = credentialsProvider.resolveCredentials().block(Duration.ofSeconds(5));
            assertThat(credentials).isNotNull();

            String key = UUID.randomUUID().toString();
            try (StatefulRedisConnection<String, String> connection = azureCredClient.connect()) {
                RedisCommands<String, String> sync = connection.sync();
                assertThat(sync.aclWhoami()).isEqualTo(credentials.getUsername());
                sync.set(key, "value");
                assertThat(sync.get(key)).isEqualTo("value");
                assertThat(connection.async().get(key).get()).isEqualTo("value");
                assertThat(connection.reactive().get(key).block()).isEqualTo("value");
                sync.del(key);
            }
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
