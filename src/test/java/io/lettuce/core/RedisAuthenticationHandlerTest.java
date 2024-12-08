package io.lettuce.core;

import io.lettuce.core.event.DefaultEventBus;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ReauthenticateFailedEvent;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.protocol.RedisCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RedisAuthenticationHandlerTest {

    private RedisChannelWriter channelWriter;

    EventBus eventBus;

    ConnectionState connectionState;

    @BeforeEach
    void setUp() {
        eventBus = new DefaultEventBus(Schedulers.immediate());
        channelWriter = mock(RedisChannelWriter.class);
        connectionState = mock(ConnectionState.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    void subscribeWithStreamingCredentialsProviderInvokesReauth() {
        MyStreamingRedisCredentialsProvider credentialsProvider = new MyStreamingRedisCredentialsProvider();

        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(channelWriter, credentialsProvider, connectionState,
                eventBus, false);

        // Subscribe to the provider
        handler.subscribe();
        credentialsProvider.emitCredentials("newuser", "newpassword".toCharArray());

        ArgumentCaptor<RedisCommand<Object, Object, Object>> captor = ArgumentCaptor.forClass(RedisCommand.class);
        verify(channelWriter).write(captor.capture());

        RedisCommand<Object, Object, Object> capturedCommand = captor.getValue();
        assertThat(capturedCommand.getType()).isEqualTo(CommandType.AUTH);
        assertThat(capturedCommand.getArgs().toCommandString()).contains("newuser");
        assertThat(capturedCommand.getArgs().toCommandString()).contains("newpassword");

        credentialsProvider.shutdown();
    }

    @Test
    void shouldHandleErrorInCredentialsStream() {
        MyStreamingRedisCredentialsProvider credentialsProvider = new MyStreamingRedisCredentialsProvider();

        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(channelWriter, credentialsProvider, connectionState,
                eventBus, false);

        verify(channelWriter, times(0)).write(any(RedisCommand.class)); // No command should be sent

        // Verify the event was published
        StepVerifier.create(eventBus.get()).then(() -> {
            handler.subscribe();
            credentialsProvider.tryEmitError(new RuntimeException("Test error"));
        }).expectNextMatches(event -> event instanceof ReauthenticateFailedEvent).thenCancel().verify(Duration.ofSeconds(1));

        credentialsProvider.shutdown();
    }

    @Test
    void shouldNotSubscribeIfConnectionIsNotSupported() {
        StreamingCredentialsProvider credentialsProvider = mock(StreamingCredentialsProvider.class);

        when(connectionState.getNegotiatedProtocolVersion()).thenReturn(ProtocolVersion.RESP2);
        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(channelWriter, credentialsProvider, connectionState,
                eventBus, true);

        // Subscribe to the provider (it should not subscribe due to unsupported connection)
        handler.subscribe();

        // Ensure credentials() was not called
        verify(credentialsProvider, times(0)).credentials();
    }

    @Test
    void testIsSupportedConnectionWithRESP2ProtocolOnPubSubConnection() {
        RedisChannelWriter writer = mock(RedisChannelWriter.class);

        ConnectionState connectionState = mock(ConnectionState.class);
        when(connectionState.getNegotiatedProtocolVersion()).thenReturn(ProtocolVersion.RESP2);

        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(writer, mock(RedisCredentialsProvider.class),
                connectionState, mock(EventBus.class), true);

        assertFalse(handler.isSupportedConnection());
    }

    @Test
    void testIsSupportedConnectionWithNonPubSubConnection() {
        RedisChannelWriter writer = mock(RedisChannelWriter.class);
        ConnectionState connectionState = mock(ConnectionState.class);
        when(connectionState.getNegotiatedProtocolVersion()).thenReturn(ProtocolVersion.RESP2);

        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(writer, mock(RedisCredentialsProvider.class),
                connectionState, mock(EventBus.class), false);

        assertTrue(handler.isSupportedConnection());
    }

    @Test
    void testIsSupportedConnectionWithRESP3ProtocolOnPubSubConnection() {

        RedisChannelWriter writer = mock(RedisChannelWriter.class);
        ConnectionState connectionState = mock(ConnectionState.class);
        when(connectionState.getNegotiatedProtocolVersion()).thenReturn(ProtocolVersion.RESP3);

        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(writer, mock(RedisCredentialsProvider.class),
                connectionState, mock(EventBus.class), true);

        assertTrue(handler.isSupportedConnection());
    }

}
