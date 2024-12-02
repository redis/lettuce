package io.lettuce.core;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import javax.inject.Inject;

import io.lettuce.core.event.command.CommandListener;
import io.lettuce.core.event.command.CommandSucceededEvent;
import io.lettuce.core.protocol.RedisCommand;
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
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
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
    void renewableCredentialProvider(RedisClient client) {

        // Thread-safe list to capture intercepted commands
        List<RedisCommand<?, ?, ?>> interceptedCommands = Collections.synchronizedList(new ArrayList<>());

        // CommandListener to track successful commands
        CommandListener commandListener = new CommandListener() {

            @Override
            public void commandSucceeded(CommandSucceededEvent event) {
                interceptedCommands.add(event.getCommand());
            }

        };

        // Add CommandListener to the client
        client.addListener(commandListener);

        // Configure client options
        client.setOptions(
                ClientOptions.builder().disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS).build());

        // Connection for managing test user credential rotation
        StatefulRedisConnection<String, String> adminConnection = client.connect();

        String testUser = "streaming_cred_test_user";
        char[] initialPassword = "token_1".toCharArray();
        char[] updatedPassword = "token_2".toCharArray();

        // Streaming credentials provider to simulate token emission
        RenewableRedisCredentialsProvider credentialsProvider = new RenewableRedisCredentialsProvider();

        // Build RedisURI with streaming credentials provider
        RedisURI uri = RedisURI.builder().withHost(TestSettings.host()).withPort(TestSettings.port())
                .withClientName("streaming_cred_test").withAuthentication(credentialsProvider)
                .withTimeout(Duration.ofSeconds(1)).build();

        // Create test user and set initial credentials
        createTestUser(adminConnection, testUser, initialPassword);
        credentialsProvider.emitToken(new StaticRedisCredentials(testUser, initialPassword));

        // Establish connection using the streaming credentials provider
        StatefulRedisConnection<String, String> userConnection = client.connect(StringCodec.UTF8, uri);

        // Verify initial authentication
        assertThat(userConnection.sync().aclWhoami()).isEqualTo(testUser);

        // Update test user credentials and emit updated credentials
        updateTestUser(adminConnection, testUser, updatedPassword);
        credentialsProvider.emitToken(new StaticRedisCredentials(testUser, updatedPassword));

        // Wait for the `AUTH` command with updated credentials
        Awaitility.await().atMost(Duration.ofSeconds(1)).until(() -> interceptedCommands.stream()
                .anyMatch(command -> isAuthCommandWithCredentials(command, testUser, updatedPassword)));

        // Verify re-authentication and connection functionality
        assertThat(userConnection.sync().ping()).isEqualTo("PONG");
        assertThat(userConnection.sync().aclWhoami()).isEqualTo(testUser);

        // Clean up
        adminConnection.close();
        userConnection.close();
    }

    private void createTestUser(StatefulRedisConnection<String, String> connection, String username, char[] password) {
        AclSetuserArgs args = AclSetuserArgs.Builder.on().allCommands().allChannels().allKeys().nopass()
                .addPassword(String.valueOf(password));
        connection.sync().aclSetuser(username, args);
    }

    private void updateTestUser(StatefulRedisConnection<String, String> connection, String username, char[] newPassword) {
        AclSetuserArgs args = AclSetuserArgs.Builder.on().allCommands().allChannels().allKeys().nopass()
                .addPassword(String.valueOf(newPassword));
        connection.sync().aclSetuser(username, args);
    }

    private boolean isAuthCommandWithCredentials(RedisCommand<?, ?, ?> command, String username, char[] password) {
        if (command.getType() == CommandType.AUTH) {
            CommandArgs<?, ?> args = command.getArgs();
            return args.toCommandString().contains(username) && args.toCommandString().contains(String.valueOf(password));
        }
        return false;
    }

    static class RenewableRedisCredentialsProvider implements StreamingCredentialsProvider {

        private final Sinks.Many<RedisCredentials> credentialsSink = Sinks.many().replay().latest();

        @Override
        public Mono<RedisCredentials> resolveCredentials() {

            return credentialsSink.asFlux().next();
        }

        public Flux<RedisCredentials> credentials() {

            return credentialsSink.asFlux().onBackpressureLatest(); // Provide a continuous stream of credentials
        }

        public void shutdown() {
            credentialsSink.tryEmitComplete();
        }

        public void emitToken(RedisCredentials credentials) {
            credentialsSink.tryEmitNext(credentials);
        }

    }

}
