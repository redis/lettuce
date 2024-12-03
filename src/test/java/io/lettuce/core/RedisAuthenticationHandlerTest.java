package io.lettuce.core;

import io.lettuce.core.event.EventBus;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnectionImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class RedisAuthenticationHandlerTest {

    @Test
    void testIsSupportedConnectionWithRESP2ProtocolOnPubSubConnection() {
        StatefulRedisPubSubConnectionImpl<?, ?> connection = mock(StatefulRedisPubSubConnectionImpl.class,
                withSettings().extraInterfaces(StatefulRedisPubSubConnection.class));

        ConnectionState connectionState = mock(ConnectionState.class);
        when(connectionState.getNegotiatedProtocolVersion()).thenReturn(ProtocolVersion.RESP2);
        when(connection.getConnectionState()).thenReturn(connectionState);
        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(connection, mock(EventBus.class));

        assertFalse(handler.isSupportedConnection());
    }

    @Test
    void testIsSupportedConnectionWithNonPubSubConnection() {
        StatefulRedisConnectionImpl<?, ?> connection = mock(StatefulRedisConnectionImpl.class);
        ConnectionState connectionState = mock(ConnectionState.class);
        when(connectionState.getNegotiatedProtocolVersion()).thenReturn(ProtocolVersion.RESP2);
        when(connection.getConnectionState()).thenReturn(connectionState);
        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(connection, mock(EventBus.class));

        assertTrue(handler.isSupportedConnection());
    }

    @Test
    void testIsSupportedConnectionWithRESP3ProtocolOnPubSubConnection() {

        StatefulRedisPubSubConnectionImpl<?, ?> connection = mock(StatefulRedisPubSubConnectionImpl.class);
        ConnectionState connectionState = mock(ConnectionState.class);
        when(connectionState.getNegotiatedProtocolVersion()).thenReturn(ProtocolVersion.RESP3);
        when(connection.getConnectionState()).thenReturn(connectionState);
        RedisAuthenticationHandler handler = new RedisAuthenticationHandler(connection, mock(EventBus.class));

        assertTrue(handler.isSupportedConnection());
    }

}
