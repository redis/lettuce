/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.*;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.CircuitBreaker.CircuitBreakerConfig;
import io.lettuce.core.failover.metrics.MetricsSnapshot;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.resource.TestClientResources;
import io.netty.channel.ChannelFuture;

/**
 * Unit tests for {@link DatabasePubSubEndpointImpl} focusing on:
 * <ul>
 * <li>DatabaseCommandTracker integration in PubSub endpoint</li>
 * <li>Command write delegation to tracker</li>
 * <li>Circuit breaker integration for PubSub commands</li>
 * <li>Channel lifecycle management in PubSub context</li>
 * <li>Timeout exception tracking for PubSub commands</li>
 * </ul>
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Tag("unit")
class DatabasePubSubEndpointTrackerTests {

    private ClientResources clientResources;

    private ClientOptions clientOptions;

    private DatabasePubSubEndpointImpl<String, String> endpoint;

    @BeforeEach
    void setUp() {
        clientResources = TestClientResources.get();
        clientOptions = ClientOptions.builder().build();
        endpoint = new DatabasePubSubEndpointImpl<>(clientOptions, clientResources);
        // Set a mock connection facade to avoid NPE during tearDown
        endpoint.setConnectionFacade(mock(ConnectionFacade.class));
    }

    @AfterEach
    void tearDown() {
        if (endpoint != null) {
            endpoint.close();
        }
    }

    private CircuitBreakerConfig getCBConfig(float failureRateThreshold, int minimumNumberOfFailures) {
        return CircuitBreakerConfig.builder().failureRateThreshold(failureRateThreshold)
                .minimumNumberOfFailures(minimumNumberOfFailures).build();
    }

    // ============ DatabaseCommandTracker Integration Tests ============

    @Nested
    @DisplayName("DatabaseCommandTracker Integration Tests")
    @Tag("unit")
    class DatabaseCommandTrackerIntegrationTests {

        @Test
        @DisplayName("Should delegate single command write to tracker when circuit breaker is bound")
        void shouldDelegateSingleCommandWriteToTrackerWhenCircuitBreakerIsBound() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.PUBLISH, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            RedisCommand<String, String, String> result = endpoint.write(asyncCommand);

            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(CompleteableCommand.class);
        }

        @Test
        @DisplayName("Should delegate batch command write to tracker when circuit breaker is bound")
        void shouldDelegateBatchCommandWriteToTrackerWhenCircuitBreakerIsBound() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            List<RedisCommand<String, String, ?>> commands = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Command<String, String, String> command = new Command<>(CommandType.PUBLISH,
                        new StatusOutput<>(StringCodec.UTF8));
                commands.add(new AsyncCommand<>(command));
            }

            Collection<RedisCommand<String, String, ?>> result = endpoint.write(commands);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("Should attach callback to PubSub commands when circuit breaker is bound")
        void shouldAttachCallbackToPubSubCommandsWhenCircuitBreakerIsBound() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SUBSCRIBE,
                    new StatusOutput<>(StringCodec.UTF8));

            AtomicInteger onCompleteCallCount = new AtomicInteger(0);
            AsyncCommand<String, String, String> spyCommand = new AsyncCommand<String, String, String>(command) {

                @Override
                public void onComplete(java.util.function.BiConsumer<? super String, Throwable> action) {
                    onCompleteCallCount.incrementAndGet();
                    super.onComplete(action);
                }

            };

            endpoint.write(spyCommand);

            // Verify onComplete was called (callback attached)
            assertThat(onCompleteCallCount.get()).isEqualTo(1);
        }

    }

    // ============ Circuit Breaker Integration Tests ============

    @Nested
    @DisplayName("Circuit Breaker Integration Tests")
    @Tag("unit")
    class CircuitBreakerIntegrationTests {

        @Test
        @DisplayName("Should track timeout exceptions for PubSub commands")
        void shouldTrackTimeoutExceptionsForPubSubCommands() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.PUBLISH, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete with timeout exception
            asyncCommand.completeExceptionally(new RedisCommandTimeoutException("timeout"));

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should not track non-timeout exceptions via tracker callback for PubSub commands")
        void shouldNotTrackNonTimeoutExceptionsViaTrackerCallbackForPubSubCommands() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.PUBLISH, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete with non-timeout exception (should be tracked by MultiDbOutboundAdapter)
            asyncCommand.completeExceptionally(new RedisConnectionException("connection failed"));

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            // Should NOT be recorded by tracker's onComplete callback
            assertThat(snapshot.getFailureCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should complete PubSub commands exceptionally when circuit breaker is open")
        void shouldCompletePubSubCommandsExceptionallyWhenCircuitBreakerIsOpen() {
            CircuitBreakerImpl circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 2));
            endpoint.bind(circuitBreaker);

            // Force circuit breaker to OPEN
            circuitBreaker.getGeneration().recordResult(new RedisCommandTimeoutException("timeout"));
            circuitBreaker.getGeneration().recordResult(new RedisCommandTimeoutException("timeout"));
            circuitBreaker.evaluateMetrics();

            assertThat(circuitBreaker.isClosed()).isFalse();

            Command<String, String, String> command = new Command<>(CommandType.PUBLISH, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            assertThat(asyncCommand.isDone()).isTrue();
            assertThat(asyncCommand.isCompletedExceptionally()).isTrue();
            assertThatThrownBy(() -> asyncCommand.get()).hasCauseInstanceOf(RedisCircuitBreakerException.class);
        }

        @Test
        @DisplayName("Should complete all PubSub commands in batch exceptionally when circuit breaker is open")
        void shouldCompleteAllPubSubCommandsInBatchExceptionallyWhenCircuitBreakerIsOpen() {
            CircuitBreakerImpl circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 2));
            endpoint.bind(circuitBreaker);

            // Force circuit breaker to OPEN
            circuitBreaker.getGeneration().recordResult(new RedisCommandTimeoutException("timeout"));
            circuitBreaker.getGeneration().recordResult(new RedisCommandTimeoutException("timeout"));
            circuitBreaker.evaluateMetrics();

            assertThat(circuitBreaker.isClosed()).isFalse();

            List<AsyncCommand<String, String, String>> commands = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Command<String, String, String> command = new Command<>(CommandType.PUBLISH,
                        new StatusOutput<>(StringCodec.UTF8));
                commands.add(new AsyncCommand<>(command));
            }

            endpoint.write(new ArrayList<>(commands));

            commands.forEach(cmd -> {
                assertThat(cmd.isDone()).isTrue();
                assertThat(cmd.isCompletedExceptionally()).isTrue();
                assertThatThrownBy(() -> cmd.get()).hasCauseInstanceOf(RedisCircuitBreakerException.class);
            });
        }

    }

    // ============ Channel Lifecycle Tests ============

    @Nested
    @DisplayName("Channel Lifecycle Tests")
    @Tag("unit")
    class ChannelLifecycleTests {

        @Test
        @DisplayName("Should notify tracker when PubSub channel becomes active")
        void shouldNotifyTrackerWhenPubSubChannelBecomesActive() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            io.netty.channel.Channel mockChannel = mock(io.netty.channel.Channel.class);
            io.netty.channel.ChannelPipeline mockPipeline = mock(io.netty.channel.ChannelPipeline.class);
            io.netty.channel.ChannelHandlerContext mockContext = mock(io.netty.channel.ChannelHandlerContext.class);
            ChannelFuture mockCloseFuture = mock(ChannelFuture.class);

            when(mockChannel.pipeline()).thenReturn(mockPipeline);
            when(mockChannel.close()).thenReturn(mockCloseFuture);
            when(mockCloseFuture.isSuccess()).thenReturn(true);
            // Simulate immediate completion of the close future
            when(mockCloseFuture.addListener(any())).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                io.netty.util.concurrent.GenericFutureListener<ChannelFuture> listener = invocation.getArgument(0);
                listener.operationComplete(mockCloseFuture);
                return mockCloseFuture;
            });
            when(mockPipeline.context(CommandHandler.class)).thenReturn(mockContext);
            when(mockContext.name()).thenReturn("CommandHandler#0");

            endpoint.notifyChannelActive(mockChannel);

            verify(mockPipeline).addAfter(eq("CommandHandler#0"), eq(MultiDbOutboundHandler.HANDLER_NAME), any());
        }

        @Test
        @DisplayName("Should notify tracker when PubSub channel becomes inactive")
        void shouldNotifyTrackerWhenPubSubChannelBecomesInactive() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            io.netty.channel.Channel mockChannel = mock(io.netty.channel.Channel.class);
            io.netty.channel.ChannelPipeline mockPipeline = mock(io.netty.channel.ChannelPipeline.class);

            when(mockChannel.pipeline()).thenReturn(mockPipeline);

            endpoint.notifyChannelInactive(mockChannel);

            verify(mockPipeline).remove(MultiDbOutboundHandler.class);
        }

    }

}
