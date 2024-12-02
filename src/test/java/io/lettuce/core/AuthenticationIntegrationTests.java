package io.lettuce.core;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import javax.inject.Inject;

import io.lettuce.authx.TokenBasedRedisCredentialsProvider;
import io.lettuce.authx.TokenBasedRedisCredentialsProvider;
import io.lettuce.core.event.command.CommandListener;
import io.lettuce.core.event.command.CommandSucceededEvent;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.test.Delay;
import io.lettuce.test.Delay;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.WithPassword;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.settings.TestSettings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Integration test for authentication.
 *
 * @author Mark Paluch
 * @author Ivo Gaydajiev
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@EnabledOnCommand("ACL")
class AuthenticationIntegrationTests extends TestSupport {

    @BeforeEach
    @Inject
    void setUp(StatefulRedisConnection<String, String> connection) {

        connection.sync().dispatch(CommandType.ACL, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).add("SETUSER").add("john").add("on").add(">foobared").add("-@all"));

        connection.sync().dispatch(CommandType.ACL, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).add("SETUSER").add("steave").add("on").add(">foobared").add("+@all"));
    }

    @Test
    @Inject
    void authAsJohn(RedisClient client) {

        RedisURI uri = RedisURI.builder().withHost(TestSettings.host()).withPort(TestSettings.port())
                .withAuthentication("john", "foobared").withLibraryName("").withLibraryVersion("").build();

        StatefulRedisConnection<String, String> connection = client.connect(uri);

        assertThatThrownBy(() -> connection.sync().info()).hasMessageContaining("NOPERM");

        connection.close();
    }

    @Test
    @Inject
    void ownCredentialProvider(RedisClient client) {

        RedisURI uri = RedisURI.builder().withHost(TestSettings.host()).withPort(TestSettings.port()).withAuthentication(() -> {
            return Mono.just(RedisCredentials.just(null, TestSettings.password()));
        }).build();

        client.setOptions(ClientOptions.create());
        WithPassword.run(client, () -> {

            StatefulRedisConnection<String, String> connection = client.connect(uri);

            assertThat(connection.sync().ping()).isEqualTo("PONG");
            connection.close();
        });
    }

    // Simulate test user credential rotation, and verify that re-authentication is successful
    @Test
    @Inject
    void streamingCredentialProvider(RedisClient client) {

        TestCommandListener listener = new TestCommandListener();
        client.addListener(listener);
        client.setOptions(client.getOptions().mutate()
                .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS).build());

        // Build RedisURI with streaming credentials provider
        MyStreamingRedisCredentialsProvider credentialsProvider = new MyStreamingRedisCredentialsProvider();
        RedisURI uri = RedisURI.builder().withHost(TestSettings.host()).withPort(TestSettings.port())
                .withClientName("streaming_cred_test").withAuthentication(credentialsProvider)
                .withTimeout(Duration.ofSeconds(5)).build();

        credentialsProvider.emitCredentials(TestSettings.username(), TestSettings.password().toString().toCharArray());

        // verify that the initial connection is successful with default user credentials
        StatefulRedisConnection<String, String> connection = client.connect(uri);
        assertThat(connection.sync().aclWhoami()).isEqualTo(TestSettings.username());

        // rotate the credentials
        credentialsProvider.emitCredentials("steave", "foobared".toCharArray());

        Awaitility.await().atMost(Duration.ofSeconds(1)).until(() -> listener.succeeded.stream()
                .anyMatch(command -> isAuthCommandWithCredentials(command, "steave", "foobared".toCharArray())));

        // verify that the connection is re-authenticated with the new user credentials
        assertThat(connection.sync().aclWhoami()).isEqualTo("steave");

        credentialsProvider.shutdown();
        connection.close();
        client.removeListener(listener);
        client.setOptions(
                client.getOptions().mutate().reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.DEFAULT).build());
    }

    static class TestCommandListener implements CommandListener {

        final List<RedisCommand<?, ?, ?>> succeeded = new ArrayList<>();

        @Override
        public void commandSucceeded(CommandSucceededEvent event) {
            synchronized (succeeded) {
                succeeded.add(event.getCommand());
            }
        }

    }

    private boolean isAuthCommandWithCredentials(RedisCommand<?, ?, ?> command, String username, char[] password) {
        if (command.getType() == CommandType.AUTH) {
            CommandArgs<?, ?> args = command.getArgs();
            return args.toCommandString().contains(username) && args.toCommandString().contains(String.valueOf(password));
        }
        return false;
    }

}

@Test
@Inject
void tokenBasedCredentialProvider(RedisClient client) {

    ClientOptions clientOptions = ClientOptions.builder()
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS).build();
    client.setOptions(clientOptions);
    // Connection used to simulate test user credential rotation
    StatefulRedisConnection<String, String> defaultConnection = client.connect();

    String testUser = "streaming_cred_test_user";
    char[] testPassword1 = "token_1".toCharArray();
    char[] testPassword2 = "token_2".toCharArray();

    TestTokenManager tokenManager = new TestTokenManager(null, null);

    // streaming credentials provider that emits redis credentials which will trigger connection re-authentication
    // token manager is used to emit updated credentials
    TokenBasedRedisCredentialsProvider credentialsProvider = new TokenBasedRedisCredentialsProvider(tokenManager);

    RedisURI uri = RedisURI.builder().withTimeout(Duration.ofSeconds(1)).withClientName("streaming_cred_test")
            .withHost(TestSettings.host()).withPort(TestSettings.port()).withAuthentication(credentialsProvider).build();

    // create test user with initial credentials set to 'testPassword1'
    createTestUser(defaultConnection, testUser, testPassword1);
    tokenManager.emitToken(testToken(testUser, testPassword1));

    StatefulRedisConnection<String, String> connection = client.connect(StringCodec.UTF8, uri);
    assertThat(connection.sync().aclWhoami()).isEqualTo(testUser);

    // update test user credentials in Redis server (password changed to testPassword2)
    // then emit updated credentials trough streaming credentials provider
    // and trigger re-connect to force re-authentication
    // updated credentials should be used for re-authentication
    updateTestUser(defaultConnection, testUser, testPassword2);
    tokenManager.emitToken(testToken(testUser, testPassword2));
    connection.sync().quit();

    Delay.delay(Duration.ofMillis(100));
    assertThat(connection.sync().ping()).isEqualTo("PONG");

    String res = connection.sync().aclWhoami();
    assertThat(res).isEqualTo(testUser);

    defaultConnection.close();
    connection.close();
}
