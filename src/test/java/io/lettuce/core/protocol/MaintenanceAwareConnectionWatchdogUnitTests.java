/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static io.lettuce.test.ReflectionTestUtils.setField;

import static org.mockito.Mockito.*;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionBuilder;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.resource.Delay;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutorGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

/**
 * Unit tests for {@link MaintenanceAwareConnectionWatchdog}.
 *
 * @author Tihomir Mateev
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag(UNIT_TEST)
class MaintenanceAwareConnectionWatchdogUnitTests {

    @Mock
    private Delay reconnectDelay;

    @Mock
    private ClientOptions clientOptions;

    @Mock
    private Timer timer;

    @Mock
    private EventExecutorGroup reconnectWorkers;

    @Mock
    private Mono<SocketAddress> socketAddressSupplier;

    @Mock
    private ReconnectionListener reconnectionListener;

    @Mock
    private ConnectionFacade connectionFacade;

    @Mock
    private EventBus eventBus;

    @Mock
    private Endpoint endpoint;

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private Channel channel;

    @Mock
    private ChannelPipeline pipeline;

    @Mock
    private CommandHandler commandHandler;

    @Mock
    private Attribute<RebindState> rebindAttribute;

    @Mock
    private PushMessage pushMessage;

    @Mock
    private MaintenanceAwareComponent component1;

    @Mock
    private MaintenanceAwareComponent component2;

    @Mock
    private Queue<RedisCommand<?, ?, ?>> commandStack;

    @Mock
    private ReconnectionHandler reconnectionHandler;

    private MaintenanceAwareConnectionWatchdog watchdog;

    @BeforeEach
    void setUp() {
        // Create a real Bootstrap with minimal configuration to avoid mocking final classes
        Bootstrap realBootstrap = new Bootstrap();
        realBootstrap.attr(ConnectionBuilder.REDIS_URI, "redis://localhost:6379");

        when(endpoint.getId()).thenReturn("test-endpoint");

        watchdog = new MaintenanceAwareConnectionWatchdog(reconnectDelay, clientOptions, realBootstrap, timer, reconnectWorkers,
                socketAddressSupplier, reconnectionListener, connectionFacade, eventBus, endpoint);

        // Set up the reconnectionHandler field using ReflectionTestUtils
        setField(watchdog, "reconnectionHandler", reconnectionHandler);
    }

    @Test
    void testChannelActive() throws Exception {
        // Given
        when(ctx.channel()).thenReturn(channel);
        when(ctx.pipeline()).thenReturn(pipeline);
        when(channel.pipeline()).thenReturn(pipeline);
        when(pipeline.get(CommandHandler.class)).thenReturn(commandHandler);
        when(commandHandler.getEndpoint()).thenReturn(endpoint);
        when(endpoint.getPushListeners()).thenReturn(Collections.emptyList());
        when(channel.remoteAddress()).thenReturn(mock(SocketAddress.class));

        // When
        watchdog.channelActive(ctx);

        // Then
        verify(endpoint).addListener(watchdog);
    }

    @Test
    void testChannelActiveWhenAlreadyRegistered() throws Exception {
        // Given
        when(ctx.channel()).thenReturn(channel);
        when(ctx.pipeline()).thenReturn(pipeline);
        when(channel.pipeline()).thenReturn(pipeline);
        when(pipeline.get(CommandHandler.class)).thenReturn(commandHandler);
        when(commandHandler.getEndpoint()).thenReturn(endpoint);
        when(endpoint.getPushListeners()).thenReturn(Collections.singletonList(watchdog));
        when(channel.remoteAddress()).thenReturn(mock(SocketAddress.class));

        // When
        watchdog.channelActive(ctx);

        // Then
        verify(endpoint, never()).addListener(watchdog);
    }

    @Test
    void testChannelReadCompleteWithRebindCompleted() throws Exception {
        // Given
        when(ctx.channel()).thenReturn(channel);
        when(channel.isActive()).thenReturn(true);
        when(channel.hasAttr(MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE)).thenReturn(true);
        when(channel.attr(MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE)).thenReturn(rebindAttribute);
        when(rebindAttribute.get()).thenReturn(RebindState.COMPLETED);
        when(channel.close()).thenReturn(mock(ChannelFuture.class));

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.channelReadComplete(ctx);

        // Then
        verify(channel).close();
        verify(component1).onRebindCompleted();
    }

    @Test
    void testChannelReadCompleteWithoutRebindCompleted() throws Exception {
        // Given
        when(ctx.channel()).thenReturn(channel);
        when(channel.isActive()).thenReturn(true);
        when(channel.hasAttr(MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE)).thenReturn(true);
        when(channel.attr(MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE)).thenReturn(rebindAttribute);
        when(rebindAttribute.get()).thenReturn(RebindState.STARTED);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.channelReadComplete(ctx);

        // Then
        verify(channel, never()).close();
        verify(component1, never()).onRebindCompleted();
    }

    @Test
    void testOnPushMessageMovingWithEmptyStack() {
        // Given
        String addressAndPort = "127.0.0.1:6380";
        ByteBuffer addressBuffer = StringCodec.UTF8.encodeKey(addressAndPort);
        List<Object> content = Arrays.asList("MOVING", "slot", addressBuffer);

        when(pushMessage.getType()).thenReturn("MOVING");
        when(pushMessage.getContent()).thenReturn(content);
        when(channel.attr(MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE)).thenReturn(rebindAttribute);
        when(channel.pipeline()).thenReturn(pipeline);
        when(pipeline.get(CommandHandler.class)).thenReturn(commandHandler);
        when(commandHandler.getStack()).thenReturn(commandStack);
        when(commandStack.isEmpty()).thenReturn(true);
        when(channel.close()).thenReturn(mock(ChannelFuture.class));

        // Set up channel field using ReflectionTestUtils
        setField(watchdog, "channel", channel);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(rebindAttribute).set(RebindState.STARTED);
        verify(reconnectionHandler).setSocketAddressSupplier(any(InetSocketAddress.class));
        verify(channel).close();
        verify(rebindAttribute).set(RebindState.COMPLETED);
        verify(component1, never()).onRebindStarted(); // Not called when stack is empty
    }

    @Test
    void testOnPushMessageMovingWithNonEmptyStack() {
        // Given
        String addressAndPort = "127.0.0.1:6380";
        ByteBuffer addressBuffer = StringCodec.UTF8.encodeKey(addressAndPort);
        List<Object> content = Arrays.asList("MOVING", "slot", addressBuffer);

        when(pushMessage.getType()).thenReturn("MOVING");
        when(pushMessage.getContent()).thenReturn(content);
        when(channel.attr(MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE)).thenReturn(rebindAttribute);
        when(channel.pipeline()).thenReturn(pipeline);
        when(pipeline.get(CommandHandler.class)).thenReturn(commandHandler);
        when(commandHandler.getStack()).thenReturn(commandStack);
        when(commandStack.isEmpty()).thenReturn(false);

        // Set up channel field using ReflectionTestUtils
        setField(watchdog, "channel", channel);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(rebindAttribute).set(RebindState.STARTED);
        verify(reconnectionHandler).setSocketAddressSupplier(any(InetSocketAddress.class));
        verify(channel, never()).close();
        verify(component1).onRebindStarted(); // Called when stack is not empty
    }

    @Test
    void testOnPushMessageMovingWithInvalidContent() {
        // Given
        List<Object> content = Arrays.asList("MOVING", "slot"); // Missing address

        when(pushMessage.getType()).thenReturn("MOVING");
        when(pushMessage.getContent()).thenReturn(content);

        // Set up channel field using ReflectionTestUtils
        setField(watchdog, "channel", channel);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(rebindAttribute, never()).set(any());
        verify(reconnectionHandler, never()).setSocketAddressSupplier(any());
        verify(component1, never()).onRebindStarted();
    }

    @Test
    void testOnPushMessageMigrating() {
        // Given
        when(pushMessage.getType()).thenReturn("MIGRATING");

        watchdog.setMaintenanceEventListener(component1);
        watchdog.setMaintenanceEventListener(component2);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onMigrateStarted();
        verify(component2).onMigrateStarted();
    }

    @Test
    void testOnPushMessageMigrated() {
        // Given
        when(pushMessage.getType()).thenReturn("MIGRATED");

        watchdog.setMaintenanceEventListener(component1);
        watchdog.setMaintenanceEventListener(component2);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onMigrateCompleted();
        verify(component2).onMigrateCompleted();
    }

    @Test
    void testOnPushMessageFailingOver() {
        // Given
        List<Object> content = Arrays.asList("FAILING_OVER", "reason", "1,2,3");
        when(pushMessage.getType()).thenReturn("FAILING_OVER");
        when(pushMessage.getContent(any())).thenReturn(content);

        watchdog.setMaintenanceEventListener(component1);
        watchdog.setMaintenanceEventListener(component2);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onFailoverStarted("1,2,3");
        verify(component2).onFailoverStarted("1,2,3");
    }

    @Test
    void testOnPushMessageFailingOverWithInvalidContent() {
        // Given
        List<Object> content = Arrays.asList("FAILING_OVER", "reason"); // Missing shards
        when(pushMessage.getType()).thenReturn("FAILING_OVER");
        when(pushMessage.getContent(any())).thenReturn(content);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onFailoverStarted(null);
    }

    @Test
    void testOnPushMessageFailingOverWithNonStringShards() {
        // Given
        List<Object> content = Arrays.asList("FAILING_OVER", "reason", 123); // Non-string shards
        when(pushMessage.getType()).thenReturn("FAILING_OVER");
        when(pushMessage.getContent(any())).thenReturn(content);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onFailoverStarted(null);
    }

    @Test
    void testOnPushMessageFailedOver() {
        // Given
        List<Object> content = Arrays.asList("FAILED_OVER", "4,5,6");
        when(pushMessage.getType()).thenReturn("FAILED_OVER");
        when(pushMessage.getContent(any())).thenReturn(content);

        watchdog.setMaintenanceEventListener(component1);
        watchdog.setMaintenanceEventListener(component2);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onFailoverCompleted("4,5,6");
        verify(component2).onFailoverCompleted("4,5,6");
    }

    @Test
    void testOnPushMessageFailedOverWithInvalidContent() {
        // Given
        List<Object> content = Collections.singletonList("FAILED_OVER"); // Missing shards
        when(pushMessage.getType()).thenReturn("FAILED_OVER");
        when(pushMessage.getContent(any())).thenReturn(content);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onFailoverCompleted(null);
    }

    @Test
    void testOnPushMessageFailedOverWithNonStringShards() {
        // Given
        List<Object> content = Arrays.asList("FAILED_OVER", 456); // Non-string shards
        when(pushMessage.getType()).thenReturn("FAILED_OVER");
        when(pushMessage.getContent(any())).thenReturn(content);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onFailoverCompleted(null);
    }

    @Test
    void testOnPushMessageUnknownType() {
        // Given
        when(pushMessage.getType()).thenReturn("UNKNOWN_TYPE");

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then - no methods should be called for unknown message types
        verifyNoInteractions(component1);
    }

    @Test
    void testOnPushMessageMovingWithInvalidAddressFormat() {
        // Given
        String invalidAddress = "invalid-address-format";
        ByteBuffer addressBuffer = StringCodec.UTF8.encodeKey(invalidAddress);
        List<Object> content = Arrays.asList("MOVING", "slot", addressBuffer);

        when(pushMessage.getType()).thenReturn("MOVING");
        when(pushMessage.getContent()).thenReturn(content);

        // Set up channel field using ReflectionTestUtils
        setField(watchdog, "channel", channel);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then - should not proceed with rebind due to invalid address
        verify(rebindAttribute, never()).set(any());
        verify(reconnectionHandler, never()).setSocketAddressSupplier(any());
        verify(component1, never()).onRebindStarted();
    }

    @Test
    void testOnPushMessageMovingWithNonByteBufferAddress() {
        // Given
        List<Object> content = Arrays.asList("MOVING", "slot", "not-a-bytebuffer");

        when(pushMessage.getType()).thenReturn("MOVING");
        when(pushMessage.getContent()).thenReturn(content);

        // Set up channel field using ReflectionTestUtils
        setField(watchdog, "channel", channel);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then - should not proceed with rebind due to invalid address type
        verify(rebindAttribute, never()).set(any());
        verify(reconnectionHandler, never()).setSocketAddressSupplier(any());
        verify(component1, never()).onRebindStarted();
    }

    @Test
    void testSetMaintenanceEventListener() {
        // When
        watchdog.setMaintenanceEventListener(component1);
        watchdog.setMaintenanceEventListener(component2);

        // Then - verify components are stored (tested indirectly through notification methods)
        when(pushMessage.getType()).thenReturn("MIGRATING");
        watchdog.onPushMessage(pushMessage);

        verify(component1).onMigrateStarted();
        verify(component2).onMigrateStarted();
    }

    @Test
    void testRebindAttributeKey() {
        // Test that the REBIND_ATTRIBUTE key is properly defined
        assertThat(MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE).isNotNull();
        assertThat(MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE.name()).isEqualTo("rebindAddress");
    }

}
