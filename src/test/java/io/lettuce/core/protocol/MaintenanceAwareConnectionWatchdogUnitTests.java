/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.test.ReflectionTestUtils.getField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static io.lettuce.test.ReflectionTestUtils.setField;

import static org.mockito.Mockito.*;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionBuilder;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.protocol.MaintenanceAwareConnectionWatchdog.RebindAwareAddressSupplier;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.Delay;
import io.lettuce.test.MutableClock;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.EventExecutorGroup;

import io.netty.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

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
    private MaintenanceAwareClusterComponent clusterComponent;

    @Mock
    private Queue<RedisCommand<?, ?, ?>> commandStack;

    @Mock
    private RedisChannelWriter channelWriter;

    @Mock
    private ClientResources clientResources;

    @Mock
    private TimeoutOptions timeoutOptions;

    @Mock
    private TimeoutOptions.TimeoutSource timeoutSource;

    @Mock
    private io.netty.util.Timeout relaxTimeout;

    @Mock
    private ReconnectionHandler reconnectionHandler;

    private RebindAwareAddressSupplier rebindAwareAddressSupplier;

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
        // Spy the rebindAwareAddressSupplier field using ReflectionTestUtils
        rebindAwareAddressSupplier = Mockito.spy((RebindAwareAddressSupplier) getField(watchdog, "rebindAwareAddressSupplier"));
        setField(watchdog, "rebindAwareAddressSupplier", rebindAwareAddressSupplier);
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
        List<Object> content = movingPushContent(1, 15, "127.0.0.1:6380");

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
        verify(rebindAwareAddressSupplier).rebind(eq(Duration.ofSeconds(15)), eq(new InetSocketAddress("127.0.0.1", 6380)));
        verify(channel).close();
        verify(rebindAttribute).set(RebindState.COMPLETED);
        verify(component1, never()).onRebindStarted(any(), any()); // Not called when stack is empty
    }

    @Test
    void testOnPushMessageMovingWithNonEmptyStack() {
        // Given
        List<Object> content = movingPushContent(1, 15, "127.0.0.1:6380");

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
        verify(channel, never()).close();
        verify(component1).onRebindStarted(any(), any()); // Called when stack is not empty
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
        verify(component1, never()).onRebindStarted(any(), any());
    }

    @Test
    void testOnPushMessageMigrating() {
        // Given
        when(pushMessage.getType()).thenReturn("MIGRATING");
        when(pushMessage.getContent()).thenReturn(migratingPushContent(1, 15, "[\"1\",\"2\",\"3\"]"));

        watchdog.setMaintenanceEventListener(component1);
        watchdog.setMaintenanceEventListener(component2);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onMigrateStarted(eq("[\"1\",\"2\",\"3\"]"));
        verify(component2).onMigrateStarted(eq("[\"1\",\"2\",\"3\"]"));
    }

    @Test
    void testOnPushMessageMigrated() {
        // Given
        when(pushMessage.getType()).thenReturn("MIGRATED");
        when(pushMessage.getContent()).thenReturn(migratedPushContent(1, "[\"1\",\"2\",\"3\"]"));

        watchdog.setMaintenanceEventListener(component1);
        watchdog.setMaintenanceEventListener(component2);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onMigrateCompleted(eq("[\"1\",\"2\",\"3\"]"));
        verify(component2).onMigrateCompleted(eq("[\"1\",\"2\",\"3\"]"));
    }

    @Test
    void testOnPushMessageFailingOver() {
        // Given
        when(pushMessage.getType()).thenReturn("FAILING_OVER");
        when(pushMessage.getContent()).thenReturn(failingoverPushContent(1, 15, "[\"1\",\"2\",\"3\"]"));

        watchdog.setMaintenanceEventListener(component1);
        watchdog.setMaintenanceEventListener(component2);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onFailoverStarted(eq("[\"1\",\"2\",\"3\"]"));
        verify(component2).onFailoverStarted(eq("[\"1\",\"2\",\"3\"]"));
    }

    @Test
    void testOnPushMessageFailingOverWithInvalidContent() {
        // Given
        List<Object> content = Arrays.asList("FAILING_OVER", "reason"); // Missing shards
        when(pushMessage.getType()).thenReturn("FAILING_OVER");
        when(pushMessage.getContent()).thenReturn(content);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onFailoverStarted(null);
    }

    @Test
    void testOnPushMessageFailingOverWithNonStringShards() {
        // Given
        List<Object> content = Arrays.asList("FAILING_OVER", 1, 15, 123); // Non-string shards
        when(pushMessage.getType()).thenReturn("FAILING_OVER");
        when(pushMessage.getContent()).thenReturn(content);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onFailoverStarted(null);
    }

    @Test
    void testOnPushMessageFailedOver() {
        // Given
        when(pushMessage.getType()).thenReturn("FAILED_OVER");
        when(pushMessage.getContent()).thenReturn(failedoverPushContent(1, "4,5,6"));

        watchdog.setMaintenanceEventListener(component1);
        watchdog.setMaintenanceEventListener(component2);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onFailoverCompleted(eq("4,5,6"));
        verify(component2).onFailoverCompleted(eq("4,5,6"));
    }

    @Test
    void testOnPushMessageFailedOverWithInvalidContent() {
        // Given
        List<Object> content = Collections.singletonList("FAILED_OVER"); // Missing shards
        when(pushMessage.getType()).thenReturn("FAILED_OVER");
        when(pushMessage.getContent()).thenReturn(content);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(component1).onFailoverCompleted(null);
    }

    @Test
    void testOnPushMessageFailedOverWithNonStringShards() {
        // Given
        List<Object> content = Arrays.asList("FAILED_OVER", 1, 0, 456); // Non-string shards
        when(pushMessage.getType()).thenReturn("FAILED_OVER");
        when(pushMessage.getContent()).thenReturn(content);

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
        List<Object> content = movingPushContent(1, 15, "invalid-address-format");

        when(pushMessage.getType()).thenReturn("MOVING");
        when(pushMessage.getContent()).thenReturn(content);

        // Set up channel field using ReflectionTestUtils
        setField(watchdog, "channel", channel);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then - should not proceed with rebind due to invalid address
        verify(rebindAttribute, never()).set(any());
        verify(component1, never()).onRebindStarted(any(), any());
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
        verify(component1, never()).onRebindStarted(any(), any());
    }

    @Test
    void testSetMaintenanceEventListener() {
        // When
        watchdog.setMaintenanceEventListener(component1);
        watchdog.setMaintenanceEventListener(component2);

        // Then - verify components are stored (tested indirectly through notification methods)
        when(pushMessage.getType()).thenReturn("MIGRATING");
        when(pushMessage.getContent()).thenReturn(migratingPushContent(1, 15, "[\"1\",\"2\",\"3\"]"));
        watchdog.onPushMessage(pushMessage);

        verify(component1).onMigrateStarted(eq("[\"1\",\"2\",\"3\"]"));
        verify(component2).onMigrateStarted(eq("[\"1\",\"2\",\"3\"]"));
    }

    @Test
    void testRebindAttributeKey() {
        // Test that the REBIND_ATTRIBUTE key is properly defined
        assertThat(MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE).isNotNull();
        assertThat(MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE.name()).isEqualTo("rebindAddress");
    }

    @Test
    void testRebindAwareAddressSupplierWithFixedClock() {
        // Given
        Instant fixedTime = Instant.parse("2023-01-01T10:00:00Z");
        Clock fixedClock = Clock.fixed(fixedTime, ZoneId.systemDefault());
        RebindAwareAddressSupplier supplier = new RebindAwareAddressSupplier(fixedClock);

        SocketAddress originalAddress = new InetSocketAddress("localhost", 6379);
        SocketAddress rebindAddress = new InetSocketAddress("127.0.0.1", 6380);
        Mono<SocketAddress> originalSupplier = Mono.just(originalAddress);

        // When - rebind with 30 seconds duration
        supplier.rebind(Duration.ofSeconds(30), rebindAddress);

        // Then - should return rebind address since we're within the cutoff time
        Mono<SocketAddress> wrappedSupplier = supplier.wrappedSupplier(originalSupplier);

        StepVerifier.create(wrappedSupplier).expectNext(rebindAddress).verifyComplete();
    }

    @Test
    void testRebindAwareAddressSupplierWithNullRebindAddress() {
        // Given
        MutableClock clock = new MutableClock(Instant.parse("2023-01-01T10:00:00Z"));
        RebindAwareAddressSupplier supplier = new RebindAwareAddressSupplier(clock);

        SocketAddress originalAddress = new InetSocketAddress("localhost", 6379);
        // Null rebind address - should return original address
        SocketAddress rebindAddress = null;
        Mono<SocketAddress> originalSupplier = Mono.just(originalAddress);

        // When - rebind with 30 seconds duration
        supplier.rebind(Duration.ofSeconds(30), rebindAddress);

        // Should return original address since rebind address is null
        Mono<SocketAddress> wrappedSupplier = supplier.wrappedSupplier(originalSupplier);

        StepVerifier.create(wrappedSupplier).expectNext(originalAddress).verifyComplete();

        // Step 5: Advance clock past expiration (31 seconds)
        clock.tick(Duration.ofSeconds(31));

        // Step 6: New subscription to same wrappedSupplier should return original address again
        StepVerifier.create(wrappedSupplier).expectNext(originalAddress).verifyComplete();
    }

    @Test
    void testRebindAwareAddressSupplierExpirationWithFixedClock() {
        // Given - Create supplier with a mutable clock that we can advance
        MutableClock clock = new MutableClock(Instant.parse("2023-01-01T10:00:00Z"));

        RebindAwareAddressSupplier supplier = new RebindAwareAddressSupplier(clock);
        SocketAddress originalAddress = new InetSocketAddress("localhost", 6379);
        SocketAddress rebindAddress = new InetSocketAddress("127.0.0.1", 6380);
        Mono<SocketAddress> originalSupplier = Mono.just(originalAddress);

        // Step 1: Create wrapped supplier once (same instance used throughout)
        Mono<SocketAddress> wrappedSupplier = supplier.wrappedSupplier(originalSupplier);

        // Step 2: First subscription should return original address (no rebind set yet)
        StepVerifier.create(wrappedSupplier).expectNext(originalAddress).verifyComplete();

        // Step 3: Invoke rebind with 30 seconds duration
        supplier.rebind(Duration.ofSeconds(30), rebindAddress);

        // Step 4: New subscription to same wrappedSupplier should return rebind address
        StepVerifier.create(wrappedSupplier).expectNext(rebindAddress).verifyComplete();

        // Step 5: Advance clock past expiration (31 seconds)
        clock.tick(Duration.ofSeconds(31));

        // Step 6: New subscription to same wrappedSupplier should return original address again
        StepVerifier.create(wrappedSupplier).expectNext(originalAddress).verifyComplete();
    }

    @Test
    void testRebindAwareAddressSupplierWithNullState() {
        // Given
        Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        RebindAwareAddressSupplier supplier = new RebindAwareAddressSupplier(fixedClock);

        SocketAddress originalAddress = new InetSocketAddress("localhost", 6379);
        Mono<SocketAddress> originalSupplier = Mono.just(originalAddress);

        // When - no rebind has been set
        Mono<SocketAddress> wrappedSupplier = supplier.wrappedSupplier(originalSupplier);

        // Then - should return original address
        StepVerifier.create(wrappedSupplier).expectNext(originalAddress).verifyComplete();

    }

    @Test
    void testMovingEventWithNullEndpoint() {
        // Given
        List<Object> content = movingPushContent(123L, 30L, null);

        when(pushMessage.getType()).thenReturn("MOVING");
        when(pushMessage.getContent()).thenReturn(content);
        when(channel.attr(MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE)).thenReturn(rebindAttribute);
        when(channel.pipeline()).thenReturn(pipeline);
        when(pipeline.get(CommandHandler.class)).thenReturn(commandHandler);
        when(commandHandler.getStack()).thenReturn(commandStack);
        when(commandStack.isEmpty()).thenReturn(false);

        EventLoop eventLoop = mock(EventLoop.class);
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        when(channel.eventLoop()).thenReturn(eventLoop);

        // Capture arguments passed to schedule(...)
        ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> unitCaptor = ArgumentCaptor.forClass(TimeUnit.class);
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);

        when(eventLoop.schedule(taskCaptor.capture(), delayCaptor.capture(), unitCaptor.capture())).thenAnswer(invocation -> {
            // For test, execute immediately
            taskCaptor.getValue().run();
            return future;
        });

        // Set up channel field using ReflectionTestUtils
        setField(watchdog, "channel", channel);

        watchdog.setMaintenanceEventListener(component1);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then
        verify(eventLoop).schedule(any(Runnable.class), anyLong(), any());
        assertThat(delayCaptor.getValue()).isEqualTo(15000L); // expected delay
        assertThat(unitCaptor.getValue()).isEqualTo(TimeUnit.MILLISECONDS);
        verify(rebindAttribute).set(RebindState.STARTED);
        verify(channel, never()).close();
        verify(component1).onRebindStarted(any(), any()); // Called when stack is not empty
    }

    /**
     * A shard is being migrated from node A to node A'. The migration starts with a {@code SMIGRATING} announcing the affected
     * slots; while it runs, the old endpoint keeps forwarding traffic through the DMC proxy, so the connection stays usable and
     * only needs relaxed timeouts.
     * <p>
     * Registers a real {@link MaintenanceAwareExpiryWriter} rather than a mock, so the assertion covers the relaxation actually
     * being applied and not merely the callback being dispatched.
     * </p>
     */
    @Test
    void testOnPushMessageSlotsMigratingRelaxesTimeouts() {
        // Given
        MaintenanceAwareExpiryWriter expiryWriter = expiryWriter();

        when(pushMessage.getType()).thenReturn("SMIGRATING");
        when(pushMessage.getContent()).thenReturn(smigratingPushContent(1, "0-16383"));

        setField(watchdog, "channel", channel);

        watchdog.setMaintenanceEventListener(expiryWriter);
        watchdog.setMaintenanceClusterEventListener(expiryWriter);

        assertThat((Boolean) getField(expiryWriter, "relaxTimeouts")).isFalse();

        // When
        watchdog.onPushMessage(pushMessage);

        // Then - timeouts are relaxed, but the connection is not rebound yet
        assertThat((Boolean) getField(expiryWriter, "relaxTimeouts")).isTrue();
        verify(rebindAttribute, never()).set(any());
        verify(channel, never()).close();
    }

    /**
     * Once {@code SMIGRATED} arrives, relaxation must survive the handoff: the completion schedules relaxation to be disabled
     * after a grace period, but the rebind that follows cancels that task while commands are still in flight.
     */
    @Test
    void testOnPushMessageSlotsMigratedKeepsTimeoutsRelaxedDuringHandoff() {
        // Given - a migration is already under way, so relaxation is on
        MaintenanceAwareExpiryWriter expiryWriter = expiryWriter();
        expiryWriter.onSlotMigrateStarted("0-16383");

        when(pushMessage.getType()).thenReturn("SMIGRATED");
        when(pushMessage.getContent())
                .thenReturn(smigratedPushContent(4, new String[] { "127.0.0.1:6379", "127.0.0.1:6380", "0-16383" }));
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 6379));
        when(channel.attr(MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE)).thenReturn(rebindAttribute);
        when(channel.pipeline()).thenReturn(pipeline);
        when(pipeline.get(CommandHandler.class)).thenReturn(commandHandler);
        when(commandHandler.getStack()).thenReturn(commandStack);
        // Commands are still in flight, so the channel is not closed yet
        when(commandStack.isEmpty()).thenReturn(false);

        setField(watchdog, "channel", channel);

        watchdog.setMaintenanceEventListener(expiryWriter);
        watchdog.setMaintenanceClusterEventListener(expiryWriter);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then - the pending disable was cancelled by the rebind and relaxation is still in effect
        assertThat((Boolean) getField(expiryWriter, "relaxTimeouts")).isTrue();
        verify(relaxTimeout, atLeastOnce()).cancel();
        verify(channel, never()).close();
    }

    /**
     * Build a real {@link MaintenanceAwareExpiryWriter} so tests can assert on relaxation state instead of on a mocked
     * {@link MaintenanceAwareComponent} callback.
     */
    private MaintenanceAwareExpiryWriter expiryWriter() {

        when(clientOptions.getTimeoutOptions()).thenReturn(timeoutOptions);
        when(timeoutOptions.isTimeoutCommands()).thenReturn(true);
        when(timeoutOptions.getSource()).thenReturn(timeoutSource);
        when(timeoutOptions.isApplyConnectionTimeout()).thenReturn(false);
        when(timeoutOptions.getRelaxedTimeout()).thenReturn(Duration.ofMillis(500));
        when(timeoutSource.getTimeUnit()).thenReturn(TimeUnit.MILLISECONDS);
        when(clientResources.eventExecutorGroup()).thenReturn(reconnectWorkers);
        when(clientResources.timer()).thenReturn(timer);
        when(timer.newTimeout(any(TimerTask.class), anyLong(), any(TimeUnit.class))).thenReturn(relaxTimeout);

        return new MaintenanceAwareExpiryWriter(channelWriter, clientOptions, clientResources);
    }

    /**
     * All slots have finished migrating from node A to node A'. The {@code SMIGRATED} topology names A as the source and A' as
     * the destination, so the connection - which is attached to A - must be handed off to A'.
     */
    @Test
    void testOnPushMessageSlotsMigratedRebindsToNewEndpoint() {
        // Given - this connection is attached to node A
        InetSocketAddress nodeA = new InetSocketAddress("127.0.0.1", 6379);
        InetSocketAddress nodeAPrime = new InetSocketAddress("127.0.0.1", 6380);

        when(pushMessage.getType()).thenReturn("SMIGRATED");
        when(pushMessage.getContent())
                .thenReturn(smigratedPushContent(2, new String[] { "127.0.0.1:6379", "127.0.0.1:6380", "0-16383" }));
        when(channel.remoteAddress()).thenReturn(nodeA);
        when(channel.attr(MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE)).thenReturn(rebindAttribute);
        when(channel.pipeline()).thenReturn(pipeline);
        when(pipeline.get(CommandHandler.class)).thenReturn(commandHandler);
        when(commandHandler.getStack()).thenReturn(commandStack);
        when(commandStack.isEmpty()).thenReturn(true);
        when(channel.close()).thenReturn(mock(ChannelFuture.class));

        setField(watchdog, "channel", channel);

        watchdog.setMaintenanceClusterEventListener(clusterComponent);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then - rebound to A', and the drained channel is closed so the reconnect picks the new address up
        verify(clusterComponent).onSlotMigrateCompleted("0-16383");
        verify(rebindAttribute).set(RebindState.STARTED);
        verify(rebindAwareAddressSupplier).rebind(any(Duration.class), eq(nodeAPrime));
        verify(channel).close();
        verify(rebindAttribute).set(RebindState.COMPLETED);
    }

    /**
     * The handoff is only effective if the reconnect actually resolves to node A'. Setting up the rebind is not enough - the
     * redirect window has to still be open when the address supplier is next subscribed to, otherwise the reconnect falls back
     * to node A and the migration is undone.
     */
    @Test
    void testOnPushMessageSlotsMigratedRedirectsReconnectToNewEndpoint() {
        // Given - this connection is attached to node A
        InetSocketAddress nodeA = new InetSocketAddress("127.0.0.1", 6379);
        InetSocketAddress nodeAPrime = new InetSocketAddress("127.0.0.1", 6380);

        when(pushMessage.getType()).thenReturn("SMIGRATED");
        when(pushMessage.getContent())
                .thenReturn(smigratedPushContent(3, new String[] { "127.0.0.1:6379", "127.0.0.1:6380", "0-16383" }));
        when(channel.remoteAddress()).thenReturn(nodeA);
        when(channel.attr(MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE)).thenReturn(rebindAttribute);
        when(channel.pipeline()).thenReturn(pipeline);
        when(pipeline.get(CommandHandler.class)).thenReturn(commandHandler);
        when(commandHandler.getStack()).thenReturn(commandStack);
        when(commandStack.isEmpty()).thenReturn(true);
        when(channel.close()).thenReturn(mock(ChannelFuture.class));

        setField(watchdog, "channel", channel);

        // When
        watchdog.onPushMessage(pushMessage);

        // Then - the next reconnect resolves to A', not back to A
        StepVerifier.create(rebindAwareAddressSupplier.wrappedSupplier(Mono.just(nodeA))).expectNext(nodeAPrime)
                .verifyComplete();
    }

    /**
     * SMIGRATING <seq_number> <slots>: The listed slots are migrating away from this node.
     *
     * @param seqNumber unique sequence number, can be use to match requests handled by different connections
     * @param slots comma-separated list of slots and slot ranges
     */
    private static List<Object> smigratingPushContent(long seqNumber, String slots) {
        return Arrays.asList("SMIGRATING", seqNumber, StringCodec.UTF8.encodeKey(slots));
    }

    /**
     * SMIGRATED <seq_number> [[<source> <destination> <slots>], ...]: The listed slot ranges finished migrating.
     *
     * @param seqNumber unique sequence number, can be use to match requests handled by different connections
     * @param migrations one {@code source, destination, slots} triple per migrated slot range
     */
    private static List<Object> smigratedPushContent(long seqNumber, String[]... migrations) {
        List<Object> entries = new ArrayList<>(migrations.length);

        for (String[] migration : migrations) {
            entries.add(Arrays.asList(StringCodec.UTF8.encodeKey(migration[0]), StringCodec.UTF8.encodeKey(migration[1]),
                    StringCodec.UTF8.encodeKey(migration[2])));
        }

        return Arrays.asList("SMIGRATED", seqNumber, entries);
    }

    /**
     * MOVING <seq_number> <time> <endpoint>: A specific endpoint is going to move to another node within <time> seconds
     *
     * @param seqNumber unique sequence number, can be use to match requests handled by different connections
     * @param time estimated operation completion time
     * @param addressAndPort address and port of the new endpoint
     */
    private static List<Object> movingPushContent(long seqNumber, long time, String addressAndPort) {
        if (addressAndPort == null) {
            return Arrays.asList("MOVING", seqNumber, time, null);
        } else {
            return Arrays.asList("MOVING", seqNumber, time, StringCodec.UTF8.encodeKey(addressAndPort));
        }
    }

    /**
     * MIGRATING <seq_number> <time> <shard_id-s>: A shard migration is going to start within <time> seconds.
     *
     * @param seqNumber unique sequence number, can be use to match requests handled by different connections
     * @param time operation will start after <time> seconds
     * @param shards comma-separated list of shard IDs
     */
    private static List<Object> migratingPushContent(long seqNumber, long time, String shards) {
        ByteBuffer shardsBuffer = StringCodec.UTF8.encodeKey(shards);
        return Arrays.asList("MIGRATING", seqNumber, time, shardsBuffer);
    }

    /**
     * MIGRATED <seq_number> <shard_id-s>: A shard migration ended.
     *
     * @param seqNumber unique sequence number, can be use to match requests handled by different connections
     * @param shards address and port of the new endpoint
     */
    private static List<Object> migratedPushContent(long seqNumber, String shards) {
        ByteBuffer shardsBuffer = StringCodec.UTF8.encodeKey(shards);
        return Arrays.asList("MIGRATED", seqNumber, shardsBuffer);
    }

    /**
     * MIGRATING <seq_number> <time> <shard_id-s>: A shard migration is going to start within <time> seconds.
     *
     * @param seqNumber unique sequence number, can be use to match requests handled by different connections
     * @param time operation will start after <time> seconds
     * @param shards comma-separated list of shard IDs
     */
    private static List<Object> failingoverPushContent(long seqNumber, long time, String shards) {
        ByteBuffer shardsBuffer = StringCodec.UTF8.encodeKey(shards);
        return Arrays.asList("FAILING_OVER", seqNumber, time, shardsBuffer);
    }

    /**
     * MIGRATED <seq_number> <shard_id-s>: A shard migration ended.
     *
     * @param seqNumber unique sequence number, can be use to match requests handled by different connections
     */
    private static List<Object> failedoverPushContent(long seqNumber, String shards) {
        ByteBuffer shardsBuffer = StringCodec.UTF8.encodeKey(shards);
        return Arrays.asList("FAILED_OVER", seqNumber, shardsBuffer);
    }

}
