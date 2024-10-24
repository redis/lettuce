package io.lettuce.core.pubsub;

import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

class StatefulRedisPubSubConnectionImplUnitTests {

    private StatefulRedisPubSubConnectionImpl connection;

    private final RedisCodec<String, String> codec = StringCodec.UTF8;

    private final Duration timeout = Duration.ofSeconds(5);

    PubSubEndpoint mockedEndpoint;

    RedisChannelWriter mockedWriter;

    @BeforeEach
    void setup() {
        mockedEndpoint = mock(PubSubEndpoint.class);
        mockedWriter = mock(RedisChannelWriter.class);

        ClientResources mockedReseources = mock(ClientResources.class);
        Tracing mockedTracing = mock(Tracing.class);
        when(mockedReseources.tracing()).thenReturn(mockedTracing);
        when(mockedTracing.isEnabled()).thenReturn(Boolean.FALSE);
        when(mockedWriter.getClientResources()).thenReturn(mockedReseources);

        connection = new StatefulRedisPubSubConnectionImpl(mockedEndpoint, mockedWriter, codec, timeout);
    }

    @Test
    void addListener() {
        RedisPubSubListener listener = mock(RedisPubSubListener.class);

        connection.addListener(listener);

        verify(mockedEndpoint).addListener(listener);
    }

    @Test
    void removeListener() {
        RedisPubSubListener listener = mock(RedisPubSubListener.class);

        connection.addListener(listener);
        connection.removeListener(listener);

        verify(mockedEndpoint).removeListener(listener);
    }

    @Test
    void removeListenerIgnoreMissingListeners() {
        RedisPubSubListener listener = mock(RedisPubSubListener.class);

        connection.removeListener(listener);

        verify(mockedEndpoint).removeListener(listener);
    }

    @Test
    void resubscribeChannelSubscription() {
        when(mockedEndpoint.hasChannelSubscriptions()).thenReturn(true);
        when(mockedEndpoint.getChannels()).thenReturn(new HashSet<>(Arrays.asList(new String[] { "channel1", "channel2" })));
        when(mockedEndpoint.hasPatternSubscriptions()).thenReturn(false);
        when(mockedEndpoint.hasShardChannelSubscriptions()).thenReturn(false);

        List<RedisFuture<Void>> subscriptions = connection.resubscribe();
        RedisFuture<Void> commandFuture = subscriptions.get(0);

        assertEquals(1, subscriptions.size());
        assertInstanceOf(AsyncCommand.class, commandFuture);
    }

    @Test
    void resubscribeShardChannelSubscription() {
        when(mockedEndpoint.hasShardChannelSubscriptions()).thenReturn(true);
        when(mockedEndpoint.getShardChannels())
                .thenReturn(new HashSet<>(Arrays.asList(new String[] { "shard_channel1", "shard_channel2" })));
        when(mockedEndpoint.hasChannelSubscriptions()).thenReturn(false);
        when(mockedEndpoint.hasPatternSubscriptions()).thenReturn(false);

        List<RedisFuture<Void>> subscriptions = connection.resubscribe();
        RedisFuture<Void> commandFuture = subscriptions.get(0);

        assertEquals(1, subscriptions.size());
        assertInstanceOf(AsyncCommand.class, commandFuture);
    }

    @Test
    void resubscribeChannelAndPatternAndShardChanelSubscription() {
        when(mockedEndpoint.hasChannelSubscriptions()).thenReturn(true);
        when(mockedEndpoint.hasPatternSubscriptions()).thenReturn(true);
        when(mockedEndpoint.hasShardChannelSubscriptions()).thenReturn(true);
        when(mockedEndpoint.getChannels()).thenReturn(new HashSet<>(Arrays.asList(new String[] { "channel1", "channel2" })));
        when(mockedEndpoint.getPatterns()).thenReturn(new HashSet<>(Arrays.asList(new String[] { "bcast*", "echo" })));
        when(mockedEndpoint.getShardChannels())
                .thenReturn(new HashSet<>(Arrays.asList(new String[] { "shard_channel1", "shard_channel2" })));
        List<RedisFuture<Void>> subscriptions = connection.resubscribe();

        assertEquals(3, subscriptions.size());
        assertInstanceOf(AsyncCommand.class, subscriptions.get(0));
        assertInstanceOf(AsyncCommand.class, subscriptions.get(1));
        assertInstanceOf(AsyncCommand.class, subscriptions.get(1));
    }

}
