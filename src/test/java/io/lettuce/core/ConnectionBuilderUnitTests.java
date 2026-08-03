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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

import java.net.SocketAddress;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.lettuce.core.event.EventBus;
import io.lettuce.core.protocol.ConnectionWatchdog;
import io.lettuce.core.protocol.Endpoint;
import io.lettuce.core.protocol.MaintenanceAwareClusterComponent;
import io.lettuce.core.protocol.MaintenanceAwareComponent;
import io.lettuce.core.protocol.MaintenanceAwareConnectionWatchdog;
import io.lettuce.core.protocol.MaintenanceAwareExpiryWriter;
import io.lettuce.core.protocol.ReconnectionListener;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.Delay;
import io.netty.bootstrap.Bootstrap;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutorGroup;
import reactor.core.publisher.Mono;

/**
 * Unit tests for {@link ConnectionBuilder}, covering how maintenance event listeners are registered on the
 * {@link MaintenanceAwareConnectionWatchdog}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag(UNIT_TEST)
class ConnectionBuilderUnitTests {

    @Mock
    private ClientResources clientResources;

    @Mock
    private Delay reconnectDelay;

    @Mock
    private Timer timer;

    @Mock
    private EventExecutorGroup eventExecutorGroup;

    @Mock
    private EventBus eventBus;

    @Mock
    private ReconnectionListener reconnectionListener;

    @Mock
    private RedisChannelHandler<String, String> connection;

    @Mock
    private Endpoint endpoint;

    private ConnectionBuilder sut;

    @BeforeEach
    void before() {

        when(clientResources.reconnectDelay()).thenReturn(reconnectDelay);
        when(clientResources.timer()).thenReturn(timer);
        when(clientResources.eventExecutorGroup()).thenReturn(eventExecutorGroup);
        when(clientResources.eventBus()).thenReturn(eventBus);

        sut = ConnectionBuilder.connectionBuilder();
        sut.bootstrap(new Bootstrap());
        sut.socketAddressSupplier(Mono.just(mock(SocketAddress.class)));
        sut.clientResources(clientResources);
        sut.reconnectionListener(reconnectionListener);
        sut.connection(connection);
        sut.endpoint(endpoint);
        sut.clientOptions(ClientOptions.create());
    }

    @Test
    void shouldRegisterWriterAsMaintenanceAndClusterListener() {

        // A MaintenanceAwareExpiryWriter reacts to both connection-level maintenance and slot migrations
        MaintenanceAwareExpiryWriter writer = mock(MaintenanceAwareExpiryWriter.class);
        when(connection.getChannelWriter()).thenReturn(writer);

        ConnectionWatchdog watchdog = sut.createConnectionWatchdog();

        assertThat(componentListeners(watchdog)).containsExactly(writer);
        assertThat(clusterComponentListeners(watchdog)).containsExactly(writer);
    }

    @Test
    void shouldRegisterClusterWriterAsClusterListenerOnly() {

        // The cluster-level connection is fronted by ClusterDistributionChannelWriter
        RedisChannelWriter writer = mock(RedisChannelWriter.class,
                withSettings().extraInterfaces(MaintenanceAwareClusterComponent.class));
        when(connection.getChannelWriter()).thenReturn(writer);

        ConnectionWatchdog watchdog = sut.createConnectionWatchdog();

        assertThat(componentListeners(watchdog)).isEmpty();
        assertThat(clusterComponentListeners(watchdog)).containsExactly((MaintenanceAwareClusterComponent) writer);
    }

    @Test
    void shouldRegisterMaintenanceAwareEndpointAsClusterListener() {

        // Cluster node connections carry the slot notification on the endpoint (ClusterNodeEndpoint), not on the writer
        Endpoint clusterNodeEndpoint = mock(Endpoint.class,
                withSettings().extraInterfaces(MaintenanceAwareClusterComponent.class));
        sut.endpoint(clusterNodeEndpoint);
        when(connection.getChannelWriter()).thenReturn(mock(RedisChannelWriter.class));

        ConnectionWatchdog watchdog = sut.createConnectionWatchdog();

        assertThat(componentListeners(watchdog)).isEmpty();
        assertThat(clusterComponentListeners(watchdog)).containsExactly((MaintenanceAwareClusterComponent) clusterNodeEndpoint);
    }

    @Test
    void shouldNotRegisterListenersForPlainWriterAndEndpoint() {

        when(connection.getChannelWriter()).thenReturn(mock(RedisChannelWriter.class));

        ConnectionWatchdog watchdog = sut.createConnectionWatchdog();

        assertThat(componentListeners(watchdog)).isEmpty();
        assertThat(clusterComponentListeners(watchdog)).isEmpty();
    }

    @Test
    void shouldNotCreateMaintenanceAwareWatchdogWhenNotificationsDisabled() {

        sut.clientOptions(ClientOptions.builder().maintNotificationsConfig(MaintNotificationsConfig.disabled()).build());
        when(connection.getChannelWriter()).thenReturn(mock(MaintenanceAwareExpiryWriter.class));

        ConnectionWatchdog watchdog = sut.createConnectionWatchdog();

        assertThat(watchdog).isNotInstanceOf(MaintenanceAwareConnectionWatchdog.class);
        assertThatNoException().isThrownBy(() -> sut.createConnectionWatchdog());
    }

    private static Set<MaintenanceAwareComponent> componentListeners(ConnectionWatchdog watchdog) {
        return getField(watchdog, "componentListeners");
    }

    private static Set<MaintenanceAwareClusterComponent> clusterComponentListeners(ConnectionWatchdog watchdog) {
        return getField(watchdog, "clusterComponentListeners");
    }

}
