package io.lettuce.core;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolVersion;

/**
 * Unit tests for {@link ClientOptions}.
 *
 * @author Mark Paluch
 */
class ClientOptionsUnitTests {

    @Test
    void testNew() {
        checkAssertions(ClientOptions.create());
    }

    @Test
    void testDefault() {

        ClientOptions options = ClientOptions.builder().build();

        assertThat(options.getReadOnlyCommands().isReadOnly(new Command<>(CommandType.SET, null))).isFalse();
        assertThat(options.getReadOnlyCommands().isReadOnly(new Command<>(CommandType.PUBLISH, null))).isFalse();
        assertThat(options.getReadOnlyCommands().isReadOnly(new Command<>(CommandType.GET, null))).isTrue();
    }

    @Test
    void testBuilder() {
        ClientOptions options = ClientOptions.builder().scriptCharset(StandardCharsets.US_ASCII).build();
        checkAssertions(options);
        assertThat(options.getScriptCharset()).isEqualTo(StandardCharsets.US_ASCII);
    }

    @Test
    void testCopy() {

        ClientOptions original = ClientOptions.builder().scriptCharset(StandardCharsets.US_ASCII).build();
        ClientOptions copy = ClientOptions.copyOf(original);

        checkAssertions(copy);
        assertThat(copy.getScriptCharset()).isEqualTo(StandardCharsets.US_ASCII);
        assertThat(copy.mutate().build().getScriptCharset()).isEqualTo(StandardCharsets.US_ASCII);

        assertThat(original.mutate()).isNotSameAs(copy.mutate());
    }

    void checkAssertions(ClientOptions sut) {
        assertThat(sut.isAutoReconnect()).isTrue();
        assertThat(sut.isCancelCommandsOnReconnectFailure()).isFalse();
        assertThat(sut.getProtocolVersion()).isEqualTo(ProtocolVersion.RESP3);
        assertThat(sut.isSuspendReconnectOnProtocolFailure()).isFalse();
        assertThat(sut.getDisconnectedBehavior()).isEqualTo(ClientOptions.DisconnectedBehavior.DEFAULT);
    }

}
