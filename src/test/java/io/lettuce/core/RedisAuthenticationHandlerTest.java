package io.lettuce.core;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.event.DefaultEventBus;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ReauthenticateFailedEvent;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
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

    private StatefulRedisConnectionImpl<String, String> connection;

    ClientResources resources;

    EventBus eventBus;

    ConnectionState connectionState;

    @BeforeEach
    void setUp() {
        eventBus = new DefaultEventBus(Schedulers.immediate());
        connection = mock(StatefulRedisConnectionImpl.class);
        resources = mock(ClientResources.class);
        when(resources.eventBus()).thenReturn(eventBus);

        connectionState = mock(ConnectionState.class);
        when(connection.getResources()).thenReturn(resources);
        when(connection.getConnectionState()).thenReturn(connectionState);
    }

    @SuppressWarnings("unchecked")
    @Test
    void subscribeWithStreamingCredentialsProviderInvokesReauth() {
        MyStreamingRedisCredentialsProvider credentialsProvider = new MyStreamingRedisCredentialsProvider();

        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(connection, credentialsProvider, false);

        // Subscribe to the provider
        handler.subscribe();
        credentialsProvider.emitCredentials("newuser", "newpassword".toCharArray());

        ArgumentCaptor<RedisCredentials> captor = ArgumentCaptor.forClass(RedisCredentials.class);
        verify(connection).setCredentials(captor.capture());

        RedisCredentials credentials = captor.getValue();
        assertThat(credentials.getUsername()).isEqualTo("newuser");
        assertThat(credentials.getPassword()).isEqualTo("newpassword".toCharArray());

        credentialsProvider.shutdown();
    }

    @Test
    void shouldHandleErrorInCredentialsStream() {
        MyStreamingRedisCredentialsProvider credentialsProvider = new MyStreamingRedisCredentialsProvider();

        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(connection, credentialsProvider, false);

        verify(connection, times(0)).dispatch(any(RedisCommand.class)); // No command should be sent

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

        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(connection, credentialsProvider, true);

        // Subscribe to the provider (it should not subscribe due to unsupported connection)
        handler.subscribe();

        // Ensure credentials() was not called
        verify(credentialsProvider, times(0)).credentials();
    }

    @Test
    void testIsSupportedConnectionWithRESP2ProtocolOnPubSubConnection() {

        when(connectionState.getNegotiatedProtocolVersion()).thenReturn(ProtocolVersion.RESP2);

        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(connection, mock(RedisCredentialsProvider.class),
                true);

        assertFalse(handler.isSupportedConnection());
    }

    @Test
    void testIsSupportedConnectionWithNonPubSubConnection() {

        when(connectionState.getNegotiatedProtocolVersion()).thenReturn(ProtocolVersion.RESP2);

        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(connection, mock(RedisCredentialsProvider.class),
                false);

        assertTrue(handler.isSupportedConnection());
    }

    @Test
    void testIsSupportedConnectionWithRESP3ProtocolOnPubSubConnection() {

        when(connectionState.getNegotiatedProtocolVersion()).thenReturn(ProtocolVersion.RESP3);

        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(connection, mock(RedisCredentialsProvider.class),
                true);

        assertTrue(handler.isSupportedConnection());
    }

}
