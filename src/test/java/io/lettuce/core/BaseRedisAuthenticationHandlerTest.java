package io.lettuce.core;

import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseRedisAuthenticationHandlerTest {

    private BaseRedisAuthenticationHandler<RedisChannelHandler<?, ?>> handler;

    private RedisChannelHandler<?, ?> connection;

    private RedisChannelWriter channelWriter;

    private StreamingCredentialsProvider streamingCredentialsProvider;

    private Sinks.Many<RedisCredentials> sink;

    @BeforeEach
    void setUp() {

        connection = mock(RedisChannelHandler.class);
        channelWriter = mock(RedisChannelWriter.class);
        when(connection.getChannelWriter()).thenReturn(channelWriter);
        streamingCredentialsProvider = mock(StreamingCredentialsProvider.class);
        sink = Sinks.many().replay().latest();
        Flux<RedisCredentials> credentialsFlux = sink.asFlux();
        when(streamingCredentialsProvider.credentials()).thenReturn(credentialsFlux);
        handler = new BaseRedisAuthenticationHandler<RedisChannelHandler<?, ?>>(connection) {

            @Override
            protected boolean isSupportedConnection() {
                return true;
            }

        };
    }

    @SuppressWarnings("unchecked")
    @Test
    void subscribeWithStreamingCredentialsProviderInvokesReauth() {

        // Subscribe to the provider
        handler.subscribe(streamingCredentialsProvider);
        sink.tryEmitNext(RedisCredentials.just("newuser", "newpassword"));

        // Ensure credentials() method was invoked
        verify(streamingCredentialsProvider).credentials();

        // Verify that write() is invoked once
        verify(channelWriter, times(1)).write(any(RedisCommand.class));

        ArgumentCaptor<RedisCommand<Object, Object, Object>> captor = ArgumentCaptor.forClass(RedisCommand.class);
        verify(channelWriter).write(captor.capture());

        RedisCommand<Object, Object, Object> capturedCommand = captor.getValue();
        assertThat(capturedCommand.getType()).isEqualTo(CommandType.AUTH);
        assertThat(capturedCommand.getArgs().toCommandString()).contains("newuser");
        assertThat(capturedCommand.getArgs().toCommandString()).contains("newpassword");
    }

    @Test
    void shouldHandleErrorInCredentialsStream() {
        Sinks.Many<RedisCredentials> sink = Sinks.many().replay().latest();
        Flux<RedisCredentials> credentialsFlux = sink.asFlux();
        StreamingCredentialsProvider credentialsProvider = mock(StreamingCredentialsProvider.class);
        when(credentialsProvider.credentials()).thenReturn(credentialsFlux);

        // Subscribe to the provider and simulate an error
        handler.subscribe(credentialsProvider);
        sink.tryEmitError(new RuntimeException("Test error"));

        verify(connection.getChannelWriter(), times(0)).write(any(RedisCommand.class)); // No command should be sent
    }

    @Test
    void shouldNotSubscribeIfConnectionIsNotSupported() {
        Sinks.Many<RedisCredentials> sink = Sinks.many().replay().latest();
        Flux<RedisCredentials> credentialsFlux = sink.asFlux();
        StreamingCredentialsProvider credentialsProvider = mock(StreamingCredentialsProvider.class);
        when(credentialsProvider.credentials()).thenReturn(credentialsFlux);

        BaseRedisAuthenticationHandler<?> handler = new BaseRedisAuthenticationHandler<RedisChannelHandler<?, ?>>(connection) {

            @Override
            protected boolean isSupportedConnection() {
                // Simulate : Pub/Sub connections are not supported with RESP2
                return false;
            }

        };

        // Subscribe to the provider (it should not subscribe due to unsupported connection)
        handler.subscribe(credentialsProvider);

        // Ensure credentials() was not called
        verify(credentialsProvider, times(0)).credentials();
    }

}
