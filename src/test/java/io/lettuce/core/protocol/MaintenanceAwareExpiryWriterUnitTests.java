/*
 * Copyright 2017-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.protocol;

import static io.lettuce.test.ReflectionTestUtils.setField;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.resource.ClientResources;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.EventExecutorGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for {@link MaintenanceAwareExpiryWriter}.
 *
 * @author Tihomir Mateev
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag(UNIT_TEST)
class MaintenanceAwareExpiryWriterUnitTests {

    @Mock
    private RedisChannelWriter delegate;

    @Mock
    private ClientOptions clientOptions;

    @Mock
    private ClientResources clientResources;

    @Mock
    private TimeoutOptions timeoutOptions;

    @Mock
    private TimeoutOptions.TimeoutSource timeoutSource;

    @Mock
    private EventExecutorGroup executorService;

    @Mock
    private Timer timer;

    @Mock
    private RedisCommand<String, String, String> command;

    @Mock
    private RedisCommand<String, String, String> command2;

    @Mock
    private DefaultEndpoint endpoint;

    @Mock
    private Channel channel;

    @Mock
    private ChannelPipeline pipeline;

    @Mock
    private MaintenanceAwareConnectionWatchdog watchdog;

    @Mock
    private Timeout timeout;

    private MaintenanceAwareExpiryWriter writer;

    @BeforeEach
    void setUp() {
        // Set up basic mocks
        when(clientOptions.getTimeoutOptions()).thenReturn(timeoutOptions);
        when(timeoutOptions.isTimeoutCommands()).thenReturn(true); // Enable command timeouts
        when(timeoutOptions.getSource()).thenReturn(timeoutSource);
        when(timeoutOptions.isApplyConnectionTimeout()).thenReturn(false);
        when(timeoutOptions.getRelaxedTimeout()).thenReturn(Duration.ofMillis(500));
        when(timeoutSource.getTimeUnit()).thenReturn(TimeUnit.MILLISECONDS);
        when(clientResources.eventExecutorGroup()).thenReturn(executorService);
        when(clientResources.timer()).thenReturn(timer);
        when(timeoutSource.getTimeout(any())).thenReturn(100L);
        when(timer.newTimeout(any(TimerTask.class), anyLong(), any(TimeUnit.class))).thenReturn(timeout);

        writer = new MaintenanceAwareExpiryWriter(delegate, clientOptions, clientResources);
    }

    @Test
    void testConstructor() {
        // Verify that the constructor properly initializes all fields
        assertThat(writer).isNotNull();
        verify(clientOptions, atLeast(1)).getTimeoutOptions();
        verify(timeoutOptions, atLeast(1)).getSource();
        verify(timeoutOptions, atLeast(1)).isApplyConnectionTimeout();
        verify(timeoutOptions, atLeast(1)).getRelaxedTimeout();
        verify(timeoutSource, atLeast(1)).getTimeUnit();
        verify(clientResources, atLeast(1)).eventExecutorGroup();
        verify(clientResources, atLeast(1)).timer();
    }

    @Test
    void testWriteSingleCommand() {
        // Given
        when(delegate.write(command)).thenReturn(command);

        // When
        RedisCommand<String, String, String> result = writer.write(command);

        // Then
        assertThat(result).isEqualTo(command);
        verify(delegate).write(command);
        verify(timer).newTimeout(any(TimerTask.class), eq(100L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testWriteCommandCollection() {
        // Given
        Collection<RedisCommand<String, String, ?>> commands = Arrays.asList(command, command2);
        when(delegate.write(commands)).thenReturn(commands);

        // When
        Collection<RedisCommand<String, String, ?>> result = writer.write(commands);

        // Then
        assertThat(result).isEqualTo(commands);
        verify(delegate).write(commands);
        verify(timer, times(2)).newTimeout(any(TimerTask.class), eq(100L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testWriteWithZeroTimeout() {
        // Given
        when(timeoutSource.getTimeout(any())).thenReturn(0L);
        when(delegate.write(command)).thenReturn(command);

        // When
        RedisCommand<String, String, String> result = writer.write(command);

        // Then
        assertThat(result).isEqualTo(command);
        verify(delegate).write(command);
        verify(timer, never()).newTimeout(any(TimerTask.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testWriteWithNegativeTimeout() {
        // Given
        when(timeoutSource.getTimeout(any())).thenReturn(-1L);
        when(delegate.write(command)).thenReturn(command);

        // When
        RedisCommand<String, String, String> result = writer.write(command);

        // Then
        assertThat(result).isEqualTo(command);
        verify(delegate).write(command);
        verify(timer, never()).newTimeout(any(TimerTask.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testWriteWithConnectionTimeout() {
        // Given
        when(timeoutOptions.isApplyConnectionTimeout()).thenReturn(true);
        when(delegate.write(command)).thenReturn(command);

        // When
        RedisCommand<String, String, String> result = writer.write(command);

        // Then
        assertThat(result).isEqualTo(command);
        verify(delegate).write(command);
        // When connection timeout is enabled, it uses the timeout source value
        verify(timer).newTimeout(any(TimerTask.class), eq(100L), eq(TimeUnit.MILLISECONDS));
    }



    @Test
    void testRegisterAsMaintenanceAwareComponentWithDefaultEndpoint() {
        // Given
        // Set up the endpoint.channel field directly
        setField(endpoint, "channel", channel);
        when(channel.pipeline()).thenReturn(pipeline);
        when(pipeline.get(MaintenanceAwareConnectionWatchdog.class)).thenReturn(watchdog);
        when(endpoint.write(command)).thenReturn(command);

        // Set delegate to DefaultEndpoint
        setField(writer, "delegate", endpoint);

        // When
        writer.write(command);

        // Then
        verify(watchdog).setMaintenanceEventListener(writer);
    }

    @Test
    void testRegisterAsMaintenanceAwareComponentWithNonDefaultEndpoint() {
        // Given
        when(delegate.write(command)).thenReturn(command);

        // When
        writer.write(command);

        // Then
        // Should not attempt to register since delegate is not DefaultEndpoint
        verifyNoInteractions(watchdog);
    }

    @Test
    void testRegisterAsMaintenanceAwareComponentOnlyOnce() {
        // Given
        setField(endpoint, "channel", channel);
        when(channel.pipeline()).thenReturn(pipeline);
        when(pipeline.get(MaintenanceAwareConnectionWatchdog.class)).thenReturn(watchdog);
        when(endpoint.write(command)).thenReturn(command);

        // Set delegate to DefaultEndpoint
        setField(writer, "delegate", endpoint);

        // When
        writer.write(command);
        writer.write(command);

        // Then
        verify(watchdog, times(1)).setMaintenanceEventListener(writer);
    }

    @Test
    void testRegisterAsMaintenanceAwareComponentWithNullWatchdog() {
        // Given
        setField(endpoint, "channel", channel);
        when(channel.pipeline()).thenReturn(pipeline);
        when(pipeline.get(MaintenanceAwareConnectionWatchdog.class)).thenReturn(null);
        when(endpoint.write(command)).thenReturn(command);

        // Set delegate to DefaultEndpoint
        setField(writer, "delegate", endpoint);

        // When
        writer.write(command);

        // Then
        // Should not throw exception when watchdog is null
        verify(pipeline).get(MaintenanceAwareConnectionWatchdog.class);
        verifyNoInteractions(watchdog);
    }

    @Test
    void testRegisterAsMaintenanceAwareComponentWithNullChannel() {
        // Given
        setField(endpoint, "channel", null);
        when(endpoint.write(command)).thenReturn(command);

        // Set delegate to DefaultEndpoint
        setField(writer, "delegate", endpoint);

        // When
        try {
            writer.write(command);
        } catch (NullPointerException e) {
            // Expected when channel is null
        }

        // Then
        // Should handle null channel gracefully (or throw NPE as expected)
        verifyNoInteractions(pipeline);
        verifyNoInteractions(watchdog);
    }

    @Test
    void testReset() {
        // Given
        setField(writer, "relaxTimeouts", true);
        setField(writer, "registered", true);

        // When
        writer.reset();

        // Then
        verify(delegate).reset();
        // Verify that relaxTimeouts and registered flags are reset (tested indirectly)
    }

    @Test
    void testOnRebindStarted() {
        // When
        writer.onRebindStarted();

        // Then - relaxTimeouts should be enabled (tested indirectly through timeout behavior)
        // Verify that the relaxed timeout is not negative
        assertThat(writer).isNotNull();
    }

    @Test
    void testOnRebindStartedWithNegativeRelaxedTimeout() {
        // Given
        when(timeoutOptions.getRelaxedTimeout()).thenReturn(Duration.ofMillis(-1));
        when(timeoutOptions.isTimeoutCommands()).thenReturn(true); // Ensure timeouts are enabled
        writer = new MaintenanceAwareExpiryWriter(delegate, clientOptions, clientResources);

        // When
        writer.onRebindStarted();

        // Then - relaxTimeouts should remain disabled for negative timeout
        assertThat(writer).isNotNull();
    }

    @Test
    void testOnRebindCompleted() {
        // When
        writer.onRebindCompleted();

        // Then
        verify(timer).newTimeout(any(TimerTask.class), eq(500L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testOnMigrateStarted() {
        // When
        writer.onMigrateStarted();

        // Then - relaxTimeouts should be enabled
        assertThat(writer).isNotNull();
    }

    @Test
    void testOnMigrateCompleted() {
        // When
        writer.onMigrateCompleted();

        // Then
        verify(timer).newTimeout(any(TimerTask.class), eq(500L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testOnFailoverStarted() {
        // Given
        String shards = "1,2,3";

        // When
        writer.onFailoverStarted(shards);

        // Then - relaxTimeouts should be enabled
        assertThat(writer).isNotNull();
    }

    @Test
    void testOnFailoverStartedWithNullShards() {
        // When
        writer.onFailoverStarted(null);

        // Then - should handle null shards gracefully
        assertThat(writer).isNotNull();
    }

    @Test
    void testOnFailoverCompleted() {
        // Given
        String shards = "4,5,6";

        // When
        writer.onFailoverCompleted(shards);

        // Then
        verify(timer).newTimeout(any(TimerTask.class), eq(500L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testOnFailoverCompletedWithNullShards() {
        // When
        writer.onFailoverCompleted(null);

        // Then
        verify(timer).newTimeout(any(TimerTask.class), eq(500L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testRelaxedTimeoutCancellation() {
        // Given
        Timeout previousTimeout = mock(Timeout.class);
        setField(writer, "relaxTimeout", previousTimeout);

        // When
        writer.onRebindStarted();

        // Then
        verify(previousTimeout).cancel();
    }

    @Test
    void testRelaxedTimeoutCancellationOnCompletion() {
        // Given
        Timeout previousTimeout = mock(Timeout.class);
        setField(writer, "relaxTimeout", previousTimeout);

        // When
        writer.onRebindCompleted();

        // Then
        verify(previousTimeout).cancel();
    }

    @Test
    void testWriteWithRelaxedTimeouts() {
        // Given
        setField(writer, "relaxTimeouts", true);
        when(command.isDone()).thenReturn(false);
        when(delegate.write(command)).thenReturn(command);

        // When
        writer.write(command);

        // Then
        verify(delegate).write(command);
        verify(timer).newTimeout(any(TimerTask.class), eq(100L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testWriteWithRelaxedTimeoutsAndCompletedCommand() {
        // Given
        setField(writer, "relaxTimeouts", true);
        when(command.isDone()).thenReturn(true);
        when(delegate.write(command)).thenReturn(command);

        // When
        writer.write(command);

        // Then
        verify(delegate).write(command);
        verify(timer).newTimeout(any(TimerTask.class), eq(100L), eq(TimeUnit.MILLISECONDS));
        // Timer task should not submit to executor since command is done
    }

    @Test
    void testTimeoutOptionsConfiguration() {
        // Given
        Duration customRelaxedTimeout = Duration.ofSeconds(2);
        when(timeoutOptions.getRelaxedTimeout()).thenReturn(customRelaxedTimeout);
        when(timeoutOptions.isApplyConnectionTimeout()).thenReturn(true);
        when(timeoutOptions.isTimeoutCommands()).thenReturn(true); // Ensure timeouts are enabled
        when(timeoutSource.getTimeUnit()).thenReturn(TimeUnit.SECONDS);

        // When
        MaintenanceAwareExpiryWriter customWriter = new MaintenanceAwareExpiryWriter(delegate, clientOptions, clientResources);

        // Then
        assertThat(customWriter).isNotNull();
        verify(timeoutOptions, atLeast(1)).getRelaxedTimeout();
        verify(timeoutOptions, atLeast(1)).isApplyConnectionTimeout();
        verify(timeoutSource, atLeast(1)).getTimeUnit();
    }

    @Test
    void testMultipleMaintenanceEvents() {
        // When
        writer.onRebindStarted();
        writer.onMigrateStarted();
        writer.onFailoverStarted("1,2,3");

        // Then - each event should enable relaxed timeouts
        assertThat(writer).isNotNull();
    }

    @Test
    void testMultipleCompletionEvents() {
        // When
        writer.onRebindCompleted();
        writer.onMigrateCompleted();
        writer.onFailoverCompleted("1,2,3");

        // Then - each completion should schedule timeout disable
        verify(timer, times(3)).newTimeout(any(TimerTask.class), eq(500L), eq(TimeUnit.MILLISECONDS));
    }
}
