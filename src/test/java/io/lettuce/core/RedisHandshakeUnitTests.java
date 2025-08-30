package io.lettuce.core;

import static io.lettuce.TestTags.*;
import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.ProtocolVersion;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * Unit tests for {@link RedisHandshake}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class RedisHandshakeUnitTests {

    public static final String ERR_UNKNOWN_COMMAND = "ERR unknown command 'CLIENT', with args beginning with: 'SETINFO' 'lib-name' 'Lettuce'";

    @Test
    void handshakeWithResp3ShouldPass() {

        EmbeddedChannel channel = new EmbeddedChannel(true, false);

        ConnectionState state = new ConnectionState();
        state.setCredentialsProvider(new StaticCredentialsProvider("foo", "bar".toCharArray()));
        RedisHandshake handshake = new RedisHandshake(ProtocolVersion.RESP3, false, state, null);
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
        RedisHandshake handshake = new RedisHandshake(null, false, state, null);
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
        RedisHandshake handshake = new RedisHandshake(null, false, state, null);
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
        RedisHandshake handshake = new RedisHandshake(null, false, state, null);
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
    void handshakeWithInvalidResponseShouldPropagateException() {

        EmbeddedChannel channel = new EmbeddedChannel(true, false);

        ConnectionState state = new ConnectionState();
        state.setCredentialsProvider(new StaticCredentialsProvider(null, null));
        RedisHandshake handshake = new RedisHandshake(null, false, state, null);
        CompletionStage<Void> handshakeInit = handshake.initialize(channel);

        AsyncCommand<String, String, Map<String, String>> hello = channel.readOutbound();
        helloStringIdResponse(hello.getOutput());
        hello.complete();

        assertThat(handshakeInit.toCompletableFuture().isCompletedExceptionally()).isTrue();
    }

    @Test
    void handshakeDelayedCredentialProvider() {

        DelayedRedisCredentialsProvider cp = new DelayedRedisCredentialsProvider();
        // RedisCredentialsProvider cp = () -> Mono.just(RedisCredentials.just("foo",
        // "bar")).delayElement(Duration.ofMillis(3));
        EmbeddedChannel channel = new EmbeddedChannel(true, false);

        ConnectionMetadata connectionMetdata = new ConnectionMetadata();
        connectionMetdata.setLibraryName("library-name");
        connectionMetdata.setLibraryVersion("library-version");

        ConnectionState state = new ConnectionState();
        state.setCredentialsProvider(cp);
        state.apply(connectionMetdata);
        RedisHandshake handshake = new RedisHandshake(null, false, state, null);
        CompletionStage<Void> handshakeInit = handshake.initialize(channel);
        cp.completeCredentials(RedisCredentials.just("foo", "bar"));

        Awaitility.await().atMost(100, MILLISECONDS) // Wait up to 5 seconds
                .pollInterval(5, MILLISECONDS) // Poll every 50 milliseconds
                .until(() -> !channel.outboundMessages().isEmpty());

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

    @Test
    void handshakeWithoutMaintenanceEventsOptionsShouldNotIncludeMaintNotifications() {

        EmbeddedChannel channel = new EmbeddedChannel(true, false);

        ConnectionState state = new ConnectionState();
        state.setCredentialsProvider(new StaticCredentialsProvider(null, null));
        RedisHandshake handshake = new RedisHandshake(ProtocolVersion.RESP3, false, state, null);
        handshake.initialize(channel);

        // Should have no post-handshake command for MAINT_NOTIFICATIONS
        AsyncCommand<String, String, Map<String, String>> hello = channel.readOutbound();
        helloResponse(hello.getOutput());
        hello.complete();

        // No post-handshake commands should be sent when addressTypeSource is null
        assertThat(channel.outboundMessages()).isEmpty();
    }

    @Test
    void handshakeWithMaintenanceEventsOptionsShouldIncludeMaintNotifications() {

        EmbeddedChannel channel = new EmbeddedChannel(true, false);

        // Mock address type source
        MaintenanceEventsOptions.AddressTypeSource addressTypeSource = mock(MaintenanceEventsOptions.AddressTypeSource.class);
        when(addressTypeSource.getAddressType(any(SocketAddress.class), anyBoolean()))
                .thenReturn(MaintenanceEventsOptions.AddressType.INTERNAL_IP);

        ConnectionState state = new ConnectionState();
        state.setCredentialsProvider(new StaticCredentialsProvider(null, null));
        RedisHandshake handshake = new RedisHandshake(ProtocolVersion.RESP3, false, state, addressTypeSource);
        handshake.initialize(channel);

        // Should have one post-handshake command for MAINT_NOTIFICATIONS
        AsyncCommand<String, String, Map<String, String>> hello = channel.readOutbound();
        helloResponse(hello.getOutput());
        hello.complete();

        List<AsyncCommand<?, ?, ?>> commands = channel.readOutbound();
        assertThat(commands).hasSize(1);

        AsyncCommand<?, ?, ?> maintCommand = commands.get(0);
        // Verify it's a CLIENT command with MAINT_NOTIFICATIONS
        assertThat(maintCommand.getType().toString()).isEqualTo("CLIENT");
        assertThat(maintCommand.getArgs().toString()).contains("MAINT_NOTIFICATIONS");
        assertThat(maintCommand.getArgs().toString()).contains("on");
        assertThat(maintCommand.getArgs().toString()).contains("moving-endpoint-type");
        assertThat(maintCommand.getArgs().toString()).contains("internal-ip");
    }

    @Test
    void handshakeWithMaintenanceEventsOptionsExternalIpShouldIncludeCorrectAddressType() {

        EmbeddedChannel channel = new EmbeddedChannel(true, false);

        // Mock address type source
        MaintenanceEventsOptions.AddressTypeSource addressTypeSource = mock(MaintenanceEventsOptions.AddressTypeSource.class);
        when(addressTypeSource.getAddressType(any(SocketAddress.class), anyBoolean()))
                .thenReturn(MaintenanceEventsOptions.AddressType.EXTERNAL_IP);

        ConnectionState state = new ConnectionState();
        state.setCredentialsProvider(new StaticCredentialsProvider(null, null));
        RedisHandshake handshake = new RedisHandshake(ProtocolVersion.RESP3, false, state, addressTypeSource);
        handshake.initialize(channel);

        // Should have one post-handshake command for MAINT_NOTIFICATIONS
        // Should have one post-handshake command for MAINT_NOTIFICATIONS
        AsyncCommand<String, String, Map<String, String>> hello = channel.readOutbound();
        helloResponse(hello.getOutput());
        hello.complete();

        List<AsyncCommand<?, ?, ?>> commands = channel.readOutbound();
        assertThat(commands).hasSize(1);

        AsyncCommand<?, ?, ?> maintCommand = commands.get(0);

        // Verify it contains the correct address type
        assertThat(maintCommand.getArgs().toString()).contains("external-ip");
    }

    @Test
    void handshakeWithMaintenanceEventsOptionsNullAddressTypeShouldNotIncludeMovingEndpointType() {

        EmbeddedChannel channel = new EmbeddedChannel(true, false);

        // Mock address type source that returns null
        MaintenanceEventsOptions.AddressTypeSource addressTypeSource = mock(MaintenanceEventsOptions.AddressTypeSource.class);
        when(addressTypeSource.getAddressType(any(SocketAddress.class), anyBoolean())).thenReturn(null);

        ConnectionState state = new ConnectionState();
        state.setCredentialsProvider(new StaticCredentialsProvider(null, null));
        RedisHandshake handshake = new RedisHandshake(ProtocolVersion.RESP3, false, state, addressTypeSource);
        handshake.initialize(channel);

        // Should have one post-handshake command for MAINT_NOTIFICATIONS
        AsyncCommand<String, String, Map<String, String>> hello = channel.readOutbound();
        helloResponse(hello.getOutput());
        hello.complete();

        List<AsyncCommand<?, ?, ?>> commands = channel.readOutbound();
        assertThat(commands).hasSize(1);

        AsyncCommand<?, ?, ?> maintCommand = commands.get(0);

        // Verify it contains MAINT_NOTIFICATIONS but not moving-endpoint-type
        assertThat(maintCommand.getArgs().toString()).contains("MAINT_NOTIFICATIONS");
        assertThat(maintCommand.getArgs().toString()).contains("on");
        assertThat(maintCommand.getArgs().toString()).doesNotContain("moving-endpoint-type");
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

    private static void helloStringIdResponse(CommandOutput<String, String, Map<String, String>> output) {

        output.multiMap(8);
        output.set(ByteBuffer.wrap("id".getBytes()));
        output.set(ByteBuffer.wrap("1".getBytes()));

        output.set(ByteBuffer.wrap("mode".getBytes()));
        output.set(ByteBuffer.wrap("master".getBytes()));

        output.set(ByteBuffer.wrap("role".getBytes()));
        output.set(ByteBuffer.wrap("master".getBytes()));

        output.set(ByteBuffer.wrap("version".getBytes()));
        output.set(ByteBuffer.wrap("1.2.3".getBytes()));
    }

    static class DelayedRedisCredentialsProvider implements RedisCredentialsProvider {

        private final Sinks.One<RedisCredentials> credentialsSink = Sinks.one();

        @Override
        public Mono<RedisCredentials> resolveCredentials() {
            return credentialsSink.asMono();
        }

        public void completeCredentials(RedisCredentials credentials) {
            credentialsSink.tryEmitValue(credentials);
        }

    }

}
