package io.lettuce.core;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import javax.inject.Inject;

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

/**
 * Integration test for authentication.
 *
 * @author Mark Paluch
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

}
