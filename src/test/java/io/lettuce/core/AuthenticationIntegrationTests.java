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
    }

    private boolean isAuthCommandWithCredentials(RedisCommand<?, ?, ?> command, String username, char[] password) {
        if (command.getType() == CommandType.AUTH) {
            CommandArgs<?, ?> args = command.getArgs();
            return args.toCommandString().contains(username) && args.toCommandString().contains(String.valueOf(password));
        }
        return false;
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

}
