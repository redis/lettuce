package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.resource.FastShutdown;

class ClientOptionsHandshakeUnitTests {

    private static final ClientOptions FIRST = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build();

    private static final ClientOptions SECOND = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3).build();

    private final LegacyHandshakeClient client = new LegacyHandshakeClient();

    @AfterEach
    void tearDown() {
        FastShutdown.shutdown(client);
    }

    @Test
    void legacyHandshakeShouldUseCapturedOptionsAndRestoreAfterNestedCall() {
        AtomicBoolean firstCall = new AtomicBoolean(true);
        client.setOptions(SECOND);
        client.onHandshake = () -> {
            if (firstCall.compareAndSet(true, false)) {
                RedisHandshake nested = (RedisHandshake) client.createHandshake(new ConnectionState(), SECOND);
                assertThat(nested.getRequestedProtocolVersion()).isEqualTo(ProtocolVersion.RESP3);
            }
        };

        RedisHandshake outer = (RedisHandshake) client.createHandshake(new ConnectionState(), FIRST);

        assertThat(outer.getRequestedProtocolVersion()).isEqualTo(ProtocolVersion.RESP2);
        assertThat(client.createHandshake(new ConnectionState()).getRequestedProtocolVersion())
                .isEqualTo(ProtocolVersion.RESP3);
        assertThat(client.getOptions()).isSameAs(SECOND);
        assertThat(client.calls).isEqualTo(3);
    }

    @Test
    void legacyHandshakeFailureShouldClearCapturedOptions() {
        AtomicBoolean fail = new AtomicBoolean(true);
        client.setOptions(SECOND);
        client.onHandshake = () -> {
            if (fail.compareAndSet(true, false)) {
                throw new IllegalStateException("Handshake creation failed");
            }
        };

        assertThatThrownBy(() -> client.createHandshake(new ConnectionState(), FIRST)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Handshake creation failed");

        assertThat(client.createHandshake(new ConnectionState()).getRequestedProtocolVersion())
                .isEqualTo(ProtocolVersion.RESP3);
        assertThat(client.getOptions()).isSameAs(SECOND);
    }

    private static class LegacyHandshakeClient extends RedisClient {

        Runnable onHandshake;

        int calls;

        @Override
        protected RedisHandshake createHandshake(ConnectionState state) {
            calls++;
            onHandshake.run();
            return super.createHandshake(state);
        }

    }

}
