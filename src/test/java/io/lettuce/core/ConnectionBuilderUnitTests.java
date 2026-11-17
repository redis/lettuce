/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.test.ReflectionTestUtils.getField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.event.EventBus;
import io.lettuce.core.protocol.ConnectionWatchdog;
import io.lettuce.core.protocol.Endpoint;
import io.lettuce.core.protocol.MaintenanceAwareConnectionWatchdog;
import io.lettuce.core.protocol.MaintenanceAwareExpiryWriter;
import io.lettuce.core.protocol.MaintenanceNotificationListener;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.Delay;
import io.netty.bootstrap.Bootstrap;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Unit tests for {@link ConnectionBuilder}.
 */
@Tag(UNIT_TEST)
class ConnectionBuilderUnitTests {

    private final MaintenanceAwareExpiryWriter expiryWriter = mock(MaintenanceAwareExpiryWriter.class);

    @Test
    void shouldFindMaintenanceNotificationListenerWhenOutermost() {
        assertThat(ConnectionBuilder.findMaintenanceNotificationListener(expiryWriter)).isSameAs(expiryWriter);
    }

    @Test
    void shouldFindMaintenanceNotificationListenerBehindCommandListenerWriter() {

        RedisChannelWriter writer = new CommandListenerWriter(expiryWriter, Collections.emptyList());

        assertThat(ConnectionBuilder.findMaintenanceNotificationListener(writer)).isSameAs(expiryWriter);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFindMaintenanceNotificationListenerBehindNestedDelegates() {

        RedisChannelWriter outer = mock(RedisChannelWriter.class, withSettings().extraInterfaces(Delegating.class));
        when(((Delegating<RedisChannelWriter>) outer).getDelegate())
                .thenReturn(new CommandListenerWriter(expiryWriter, Collections.emptyList()));

        assertThat(ConnectionBuilder.findMaintenanceNotificationListener(outer)).isSameAs(expiryWriter);
    }

    @Test
    void shouldNotFindMaintenanceNotificationListenerWithoutOne() {

        RedisChannelWriter endpoint = mock(RedisChannelWriter.class);

        assertThat(ConnectionBuilder.findMaintenanceNotificationListener(endpoint)).isNull();
        assertThat(ConnectionBuilder
                .findMaintenanceNotificationListener(new CommandListenerWriter(endpoint, Collections.emptyList()))).isNull();
    }

    @Test
    void shouldRegisterMaintenanceNotificationListenersOnWatchdog() {

        MaintenanceNotificationListener listener = mock(MaintenanceNotificationListener.class);
        ClientResources clientResources = mock(ClientResources.class);
        when(clientResources.reconnectDelay()).thenReturn(Delay.constant(Duration.ofMillis(1)));
        when(clientResources.timer()).thenReturn(mock(Timer.class));
        when(clientResources.eventExecutorGroup()).thenReturn(mock(EventExecutorGroup.class));
        when(clientResources.eventBus()).thenReturn(mock(EventBus.class));

        RedisChannelHandler<?, ?> connection = mock(RedisChannelHandler.class);
        when(connection.getChannelWriter()).thenReturn(expiryWriter);

        ConnectionBuilder builder = ConnectionBuilder.connectionBuilder().clientResources(clientResources)
                .clientOptions(ClientOptions.builder().maintNotificationsConfig(MaintNotificationsConfig.enabled()).build())
                .bootstrap(new Bootstrap()).endpoint(mock(Endpoint.class)).connection(connection)
                .socketAddressSupplier(() -> null).maintenanceNotificationListener(listener);

        ConnectionWatchdog watchdog = builder.createConnectionWatchdog();

        assertThat(watchdog).isInstanceOf(MaintenanceAwareConnectionWatchdog.class);
        List<Object> listeners = getField(watchdog, "listeners");
        assertThat(listeners).containsExactly(expiryWriter, listener);
    }

}
