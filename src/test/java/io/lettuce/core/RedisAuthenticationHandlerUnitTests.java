package io.lettuce.core;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.DefaultEventBus;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ReauthenticationFailedEvent;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.protocol.CommandType.AUTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link RedisAuthenticationHandler}
 */
@Tag(UNIT_TEST)
public class RedisAuthenticationHandlerUnitTests {

    private StatefulRedisConnectionImpl<String, String> connection;

    RedisChannelWriter writer;

    ClientResources resources;

    EventBus eventBus;

    ConnectionState connectionState;

    @BeforeEach
    void setUp() {
        eventBus = new DefaultEventBus(Schedulers.immediate());
        writer = mock(RedisChannelWriter.class);
        connection = mock(StatefulRedisConnectionImpl.class);
        resources = mock(ClientResources.class);
        when(resources.eventBus()).thenReturn(eventBus);

        connectionState = mock(ConnectionState.class);
        when(connection.getResources()).thenReturn(resources);
        when(connection.getCodec()).thenReturn(StringCodec.UTF8);
        when(connection.getConnectionState()).thenReturn(connectionState);
        when(connection.getChannelWriter()).thenReturn(writer);
    }

    @SuppressWarnings("unchecked")
    @Test
    void subscribeWithStreamingCredentialsProviderInvokesReauth() {
        MyStreamingRedisCredentialsProvider credentialsProvider = new MyStreamingRedisCredentialsProvider();

        RedisAuthenticationHandler<String, String> handler = new RedisAuthenticationHandler<>(connection, credentialsProvider,
                false);

        // Subscribe to the provider
        handler.subscribe();
        credentialsProvider.emitCredentials("newuser", "newpassword".toCharArray());

        ArgumentCaptor<AsyncCommand<String, String, String>> captor = ArgumentCaptor.forClass(AsyncCommand.class);
        verify(writer).write(captor.capture());

        AsyncCommand<String, String, String> credentialsCommand = captor.getValue();
        assertThat(credentialsCommand.getType()).isEqualTo(AUTH);
        assertThat(credentialsCommand.getArgs().count()).isEqualTo(2);
        assertThat(credentialsCommand.getArgs().toCommandString()).isEqualTo("newuser" + " " + "newpassword");

        credentialsProvider.shutdown();
    }

    @Test
    void shouldHandleErrorInCredentialsStream() {
        MyStreamingRedisCredentialsProvider credentialsProvider = new MyStreamingRedisCredentialsProvider();

        RedisAuthenticationHandler<?, ?> handler = new RedisAuthenticationHandler<>(connection, credentialsProvider, false);

        verify(connection, times(0)).dispatch(any(RedisCommand.class)); // No command should be sent

        // Verify the event was published
        StepVerifier.create(eventBus.get()).then(() -> {
            handler.subscribe();
            credentialsProvider.tryEmitError(new RuntimeException("Test error"));
        }).expectNextMatches(event -> event instanceof ReauthenticationFailedEvent).thenCancel().verify(Duration.ofSeconds(1));

        credentialsProvider.shutdown();
    }

    @Test
    void shouldNotSubscribeIfConnectionIsNotSupported() {
        MyStreamingRedisCredentialsProvider credentialsProvider = new MyStreamingRedisCredentialsProvider();
        when(connectionState.getNegotiatedProtocolVersion()).thenReturn(ProtocolVersion.RESP2);

        RedisAuthenticationHandler<?, ?> handler = new RedisAuthenticationHandler<>(connection, credentialsProvider, true);

        // Subscribe to the provider (it should not subscribe due to unsupported connection)
        handler.subscribe();
        credentialsProvider.emitCredentials("newuser", "newpassword".toCharArray());

        // Ensure credentials() was not called
        verify(connection, times(0)).dispatch(any(RedisCommand.class)); // No command should be sent
    }

    @Test
    void testIsSupportedConnectionWithRESP2ProtocolOnPubSubConnection() {

        when(connectionState.getNegotiatedProtocolVersion()).thenReturn(ProtocolVersion.RESP2);

        RedisAuthenticationHandler<?, ?> handler = new RedisAuthenticationHandler<>(connection,
                mock(RedisCredentialsProvider.class), true);

        assertFalse(handler.isSupportedConnection());
    }

    @Test
    void testIsSupportedConnectionWithNonPubSubConnection() {

        when(connectionState.getNegotiatedProtocolVersion()).thenReturn(ProtocolVersion.RESP2);

        RedisAuthenticationHandler<?, ?> handler = new RedisAuthenticationHandler<>(connection,
                mock(RedisCredentialsProvider.class), false);

        assertTrue(handler.isSupportedConnection());
    }

    @Test
    void testIsSupportedConnectionWithRESP3ProtocolOnPubSubConnection() {

        when(connectionState.getNegotiatedProtocolVersion()).thenReturn(ProtocolVersion.RESP3);

        RedisAuthenticationHandler<?, ?> handler = new RedisAuthenticationHandler<>(connection,
                mock(RedisCredentialsProvider.class), true);

        assertTrue(handler.isSupportedConnection());
    }

    @Test
    public void testSetCredentialsWhenCredentialsAreNull() {
        RedisAuthenticationHandler<?, ?> handler = new RedisAuthenticationHandler<>(connection,
                mock(RedisCredentialsProvider.class), false);

        handler.setCredentials(null);

        verify(connection, times(0)).dispatch(any(RedisCommand.class)); // No command should be sent
    }

    @Test
    void testSetCredentialsDoesNotDispatchAuthIfInTransaction() {
        MyStreamingRedisCredentialsProvider credentialsProvider = new MyStreamingRedisCredentialsProvider();
        RedisAuthenticationHandler<?, ?> handler = new RedisAuthenticationHandler<>(connection, credentialsProvider, false);

        // Subscribe to the provider
        handler.subscribe();

        // Indicate a transaction is ongoing
        handler.startTransaction();

        // Attempt to authenticate
        credentialsProvider.emitCredentials("newuser", "newpassword".toCharArray());

        // verify that the AUTH command was not sent
        verify(connection, times(0)).dispatch(any(RedisCommand.class));

        // Indicate a transaction is ongoing
        handler.endTransaction();

        ArgumentCaptor<AsyncCommand<String, String, String>> captor = ArgumentCaptor.forClass(AsyncCommand.class);
        verify(writer).write(captor.capture());

        AsyncCommand<String, String, String> credentialsCommand = captor.getValue();
        assertThat(credentialsCommand.getType()).isEqualTo(AUTH);
        assertThat(credentialsCommand.getArgs().count()).isEqualTo(2);
        assertThat(credentialsCommand.getArgs().toCommandString()).isEqualTo("newuser" + " " + "newpassword");
    }

    public static <K, V, T> ArgumentMatcher<RedisCommand<K, V, T>> isAuthCommand(String expectedUsername,
            String expectedPassword) {
        return new ArgumentMatcher<RedisCommand<K, V, T>>() {

            @Override
            public boolean matches(RedisCommand command) {
                if (command.getType() != CommandType.AUTH) {
                    return false;
                }

                // Retrieve arguments (adjust based on your RedisCommand implementation)
                return command.getArgs().toCommandString().equals(expectedUsername + " " + expectedPassword);
            }

            @Override
            public String toString() {
                return String.format("Expected AUTH command with username=%s and password=%s", expectedUsername,
                        expectedPassword);
            }

        };
    }

}
