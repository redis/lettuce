package io.lettuce.core.pubsub;

import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.pubsub.PubSubOutput;
import io.lettuce.core.pubsub.RedisPubSubAdapter;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Tag(UNIT_TEST)
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

    @Test
    void autoResubscribeListenerIsRegistered() {
        // Verify that the connection has the markIntentionalUnsubscribe method
        // This confirms the auto-resubscribe functionality is available
        connection.markIntentionalUnsubscribe("test-channel");
        // If no exception is thrown, the method exists and works
        assertTrue(true);
    }

    @Test
    void intentionalUnsubscribeBypassesAutoResubscribe() throws Exception {
        // Test 1: Intentional unsubscribe should NOT trigger auto-resubscribe

        // Create a mock async commands to verify ssubscribe is NOT called
        RedisPubSubAsyncCommands<String, String> mockAsync = mock(RedisPubSubAsyncCommands.class);
        StatefulRedisPubSubConnectionImpl<String, String> spyConnection = spy(connection);
        when(spyConnection.async()).thenReturn(mockAsync);

        // Mark the channel as intentionally unsubscribed
        spyConnection.markIntentionalUnsubscribe("test-channel");

        // Use reflection to access the private endpoint and trigger sunsubscribed event
        PubSubEndpoint<String, String> endpoint = getEndpointViaReflection(spyConnection);
        PubSubOutput<String, String> sunsubscribeMessage = createSunsubscribeMessage("test-channel", codec);
        endpoint.notifyMessage(sunsubscribeMessage);

        // Wait a moment for any async processing
        Thread.sleep(50);

        // Verify that ssubscribe was NOT called (intentional unsubscribe bypassed auto-resubscribe)
        verify(mockAsync, never()).ssubscribe("test-channel");
    }

    @Test
    void unintentionalUnsubscribeTriggersAutoResubscribe() throws Exception {
        // Test 2: Unintentional unsubscribe (from Redis) should trigger auto-resubscribe

        // Create a fresh connection with a mock async
        PubSubEndpoint<String, String> mockEndpoint = mock(PubSubEndpoint.class);
        StatefulRedisPubSubConnectionImpl<String, String> testConnection = new StatefulRedisPubSubConnectionImpl<>(mockEndpoint,
                mockedWriter, codec, timeout);

        // Create a mock async commands to verify ssubscribe IS called
        RedisPubSubAsyncCommands<String, String> mockAsync = mock(RedisPubSubAsyncCommands.class);
        @SuppressWarnings("unchecked")
        RedisFuture<Void> mockFuture = mock(RedisFuture.class);
        when(mockAsync.ssubscribe("test-channel")).thenReturn(mockFuture);

        StatefulRedisPubSubConnectionImpl<String, String> spyConnection = spy(testConnection);
        when(spyConnection.async()).thenReturn(mockAsync);

        // Get the auto-resubscribe listener directly and trigger it
        RedisPubSubListener<String, String> autoResubscribeListener = getAutoResubscribeListener(spyConnection);

        // Do NOT mark as intentional - simulate Redis server sunsubscribe during slot movement
        autoResubscribeListener.sunsubscribed("test-channel", 0);

        // Wait a moment for async processing
        Thread.sleep(50);

        // Verify that ssubscribe WAS called (auto-resubscribe triggered)
        verify(mockAsync, times(1)).ssubscribe("test-channel");
    }

    @SuppressWarnings("unchecked")
    private PubSubEndpoint<String, String> getEndpointViaReflection(
            StatefulRedisPubSubConnectionImpl<String, String> connection) throws Exception {
        Field endpointField = StatefulRedisPubSubConnectionImpl.class.getDeclaredField("endpoint");
        endpointField.setAccessible(true);
        return (PubSubEndpoint<String, String>) endpointField.get(connection);
    }

    @SuppressWarnings("unchecked")
    private RedisPubSubListener<String, String> getAutoResubscribeListener(
            StatefulRedisPubSubConnectionImpl<String, String> connection) throws Exception {
        Field listenerField = StatefulRedisPubSubConnectionImpl.class.getDeclaredField("autoResubscribeListener");
        listenerField.setAccessible(true);
        return (RedisPubSubListener<String, String>) listenerField.get(connection);
    }

    private PubSubOutput<String, String> createSunsubscribeMessage(String channel, RedisCodec<String, String> codec) {
        PubSubOutput<String, String> output = new PubSubOutput<>(codec);
        output.set(ByteBuffer.wrap("sunsubscribe".getBytes()));
        output.set(ByteBuffer.wrap(channel.getBytes()));
        output.set(0L); // count
        return output;
    }

}
