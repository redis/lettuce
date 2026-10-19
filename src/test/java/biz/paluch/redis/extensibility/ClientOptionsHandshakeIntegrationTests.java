package biz.paluch.redis.extensibility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionState;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.protocol.ConnectionInitializer;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;

class ClientOptionsHandshakeIntegrationTests {

    @Test
    void subclassOutsideCorePackageShouldCustomizeHandshakeWithCapturedOptions() {
        ClientOptions first = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build();
        ClientOptions second = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3).build();
        RedisClient client = new RedisClient() {

            @Override
            protected DefaultEndpoint createEndpoint(ClientOptions clientOptions) {
                setOptions(second);
                return new DefaultEndpoint(clientOptions, getResources());
            }

            @Override
            protected ConnectionInitializer createHandshake(ConnectionState state, ClientOptions clientOptions) {
                assertThat(clientOptions).isSameAs(first);
                assertThat(getOptions()).isSameAs(second);
                ConnectionInitializer handshake = super.createHandshake(state, clientOptions);
                return channel -> handshake.initialize(channel).thenRun(() -> {
                    assertThat(state.getNegotiatedProtocolVersion()).isEqualTo(ProtocolVersion.RESP2);
                });
            }

        };
        client.setOptions(first);

        try {
            try (StatefulRedisConnection<String, String> connection = client
                    .connect(RedisURI.create(TestSettings.host(), TestSettings.port()))) {
                assertThat(connection.sync().ping()).isEqualTo("PONG");
                assertThat(connection.getOptions().getConfiguredProtocolVersion()).isEqualTo(ProtocolVersion.RESP2);
            }
        } finally {
            FastShutdown.shutdown(client);
        }
    }

}
