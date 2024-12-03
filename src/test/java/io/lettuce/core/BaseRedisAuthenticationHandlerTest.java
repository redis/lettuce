package io.lettuce.core;

import io.lettuce.core.event.DefaultEventBus;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

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

    @BeforeEach
    void setUp() {
        EventBus eventBus = new DefaultEventBus(Schedulers.immediate());
        connection = mock(RedisChannelHandler.class);
        channelWriter = mock(RedisChannelWriter.class);
        when(connection.getChannelWriter()).thenReturn(channelWriter);
        handler = new BaseRedisAuthenticationHandler<RedisChannelHandler<?, ?>>(connection, eventBus) {

        };
    }

    @SuppressWarnings("unchecked")
    @Test
    void subscribeWithStreamingCredentialsProviderInvokesReauth() {
        MyStreamingRedisCredentialsProvider credentialsProvider = new MyStreamingRedisCredentialsProvider();

        // Subscribe to the provider
        handler.subscribe(credentialsProvider);
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

        // Subscribe to the provider and simulate an error
        handler.subscribe(credentialsProvider);
        credentialsProvider.tryEmitError(new RuntimeException("Test error"));

        verify(connection.getChannelWriter(), times(0)).write(any(RedisCommand.class)); // No command should be sent

        credentialsProvider.shutdown();
    }

    @Test
    void shouldNotSubscribeIfConnectionIsNotSupported() {
        EventBus eventBus = new DefaultEventBus(Schedulers.immediate());
        StreamingCredentialsProvider credentialsProvider = mock(StreamingCredentialsProvider.class);

        BaseRedisAuthenticationHandler<?> handler = new BaseRedisAuthenticationHandler<RedisChannelHandler<?, ?>>(connection,
                eventBus) {

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
