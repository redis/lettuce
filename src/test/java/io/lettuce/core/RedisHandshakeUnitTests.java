package io.lettuce.core;

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.Test;

import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.ProtocolVersion;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * Unit tests for {@link RedisHandshake}.
 *
 * @author Mark Paluch
 */
class RedisHandshakeUnitTests {

    public static final String ERR_UNKNOWN_COMMAND = "ERR unknown command 'CLIENT', with args beginning with: 'SETINFO' 'lib-name' 'Lettuce'";

    @Test
    void handshakeWithResp3ShouldPass() {

        EmbeddedChannel channel = new EmbeddedChannel(true, false);

        ConnectionState state = new ConnectionState();
        state.setCredentialsProvider(new StaticCredentialsProvider("foo", "bar".toCharArray()));
        RedisHandshake handshake = new RedisHandshake(ProtocolVersion.RESP3, false, state);
        handshake.initialize(channel);

        AsyncCommand<String, String, Map<String, String>> hello = channel.readOutbound();
        helloResponse(hello.getOutput());
        hello.complete();

        assertThat(state.getNegotiatedProtocolVersion()).isEqualTo(ProtocolVersion.RESP3);
    }

    @Test
    void handshakeWithDiscoveryShouldPass() {

        EmbeddedChannel channel = new EmbeddedChannel(true, false);

        ConnectionState state = new ConnectionState();
        state.setCredentialsProvider(new StaticCredentialsProvider("foo", "bar".toCharArray()));
        RedisHandshake handshake = new RedisHandshake(null, false, state);
        handshake.initialize(channel);

        AsyncCommand<String, String, Map<String, String>> hello = channel.readOutbound();
        helloResponse(hello.getOutput());
        hello.complete();

        assertThat(state.getNegotiatedProtocolVersion()).isEqualTo(ProtocolVersion.RESP3);
    }

    @Test
    void handshakeWithDiscoveryShouldDowngrade() {

        EmbeddedChannel channel = new EmbeddedChannel(true, false);

        ConnectionState state = new ConnectionState();
        state.setCredentialsProvider(new StaticCredentialsProvider(null, null));
        RedisHandshake handshake = new RedisHandshake(null, false, state);
        handshake.initialize(channel);

        AsyncCommand<String, String, Map<String, String>> hello = channel.readOutbound();
        hello.getOutput().setError("NOPROTO");
        hello.completeExceptionally(new RedisException("NOPROTO"));
        hello.complete();

        assertThat(state.getNegotiatedProtocolVersion()).isEqualTo(ProtocolVersion.RESP2);
    }

    @Test
    void handshakeFireAndForgetPostHandshake() {

        EmbeddedChannel channel = new EmbeddedChannel(true, false);

        ConnectionMetadata connectionMetdata = new ConnectionMetadata();
        connectionMetdata.setLibraryName("library-name");
        connectionMetdata.setLibraryVersion("library-version");

        ConnectionState state = new ConnectionState();
        state.setCredentialsProvider(new StaticCredentialsProvider(null, null));
        state.apply(connectionMetdata);
        RedisHandshake handshake = new RedisHandshake(null, false, state);
        CompletionStage<Void> handshakeInit = handshake.initialize(channel);

        AsyncCommand<String, String, Map<String, String>> hello = channel.readOutbound();
        helloResponse(hello.getOutput());
        hello.complete();

        List<AsyncCommand<String, String, Map<String, String>>> postHandshake = channel.readOutbound();
        postHandshake.get(0).getOutput().setError(ERR_UNKNOWN_COMMAND);
        postHandshake.get(0).completeExceptionally(new RedisException(ERR_UNKNOWN_COMMAND));
        postHandshake.get(0).complete();

        assertThat(postHandshake.size()).isEqualTo(2);
        assertThat(handshakeInit.toCompletableFuture().isCompletedExceptionally()).isFalse();
    }

    @Test
    void shouldParseVersionWithCharacters() {

        assertThat(RedisHandshake.RedisVersion.of("1.2.3").toString()).isEqualTo("1.2.3");
        assertThat(RedisHandshake.RedisVersion.of("01.02.03").toString()).isEqualTo("1.2.3");
        assertThat(RedisHandshake.RedisVersion.of("01.02").toString()).isEqualTo("1.2.0");
        assertThat(RedisHandshake.RedisVersion.of("01").toString()).isEqualTo("1.0.0");

        assertThat(RedisHandshake.RedisVersion.of("1.2a.3").toString()).isEqualTo("1.2.3");
        assertThat(RedisHandshake.RedisVersion.of("1.2.3a").toString()).isEqualTo("1.2.3");
        assertThat(RedisHandshake.RedisVersion.of("1.2.3(c)").toString()).isEqualTo("1.2.3");
        assertThat(RedisHandshake.RedisVersion.of("a.2.3(c)").toString()).isEqualTo("2.3.0");
    }

    private static void helloResponse(CommandOutput<String, String, Map<String, String>> output) {

        output.multiMap(8);
        output.set(ByteBuffer.wrap("id".getBytes()));
        output.set(1);

        output.set(ByteBuffer.wrap("mode".getBytes()));
        output.set(ByteBuffer.wrap("master".getBytes()));

        output.set(ByteBuffer.wrap("role".getBytes()));
        output.set(ByteBuffer.wrap("master".getBytes()));

        output.set(ByteBuffer.wrap("version".getBytes()));
        output.set(ByteBuffer.wrap("1.2.3".getBytes()));
    }

}
