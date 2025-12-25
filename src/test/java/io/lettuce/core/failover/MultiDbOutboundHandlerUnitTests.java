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
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.CircuitBreaker.CircuitBreakerConfig;
import io.lettuce.core.failover.metrics.MetricsSnapshot;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.Future;

/**
 * Unit tests for {@link MultiDbOutboundHandler} focusing on:
 * <ul>
 * <li>Write operation interception and listener attachment</li>
 * <li>Success/failure tracking via ChannelPromise listeners</li>
 * <li>Command completion tracking via CompleteableCommand callbacks</li>
 * <li>Timeout exception filtering (should NOT be recorded by outbound handler)</li>
 * <li>Single and batch command handling</li>
 * </ul>
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Tag("unit")
class MultiDbOutboundHandlerUnitTests {

    private MultiDbOutboundHandler handler;

    private CircuitBreaker circuitBreaker;

    private ChannelHandlerContext mockContext;

    private ChannelPromise mockPromise;

    @BeforeEach
    void setUp() {
        mockContext = mock(ChannelHandlerContext.class);
        mockPromise = mock(ChannelPromise.class);
    }

    @AfterEach
    void tearDown() {
        circuitBreaker = null;
    }

    private CircuitBreakerConfig getCBConfig(float failureRateThreshold, int minimumNumberOfFailures) {
        return CircuitBreakerConfig.builder().failureRateThreshold(failureRateThreshold)
                .minimumNumberOfFailures(minimumNumberOfFailures).build();
    }

    // ============ Write Interception Tests ============

    @Nested
    @DisplayName("Write Interception Tests")
    class WriteInterceptionTests {

        @Test
        @DisplayName("Should attach listener to promise for single command write")
        void shouldAttachListenerToPromiseForSingleCommandWrite() throws Exception {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            handler = new MultiDbOutboundHandler(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            handler.write(mockContext, asyncCommand, mockPromise);

            // Verify listener was attached to promise
            verify(mockPromise).addListener(any());
            // Verify write was passed down the pipeline
            verify(mockContext).write(asyncCommand, mockPromise);
        }

        @Test
        @DisplayName("Should attach listeners to promise for batch command write")
        void shouldAttachListenersToPromiseForBatchCommandWrite() throws Exception {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            handler = new MultiDbOutboundHandler(circuitBreaker);

            List<RedisCommand<String, String, ?>> commands = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                commands.add(new AsyncCommand<>(command));
            }

            handler.write(mockContext, commands, mockPromise);

            // Verify listener was attached for each command (3 times)
            verify(mockPromise, times(3)).addListener(any());
            // Verify write was passed down the pipeline
            verify(mockContext).write(commands, mockPromise);
        }

        @Test
        @DisplayName("Should not attach listener when circuit breaker is null")
        void shouldNotAttachListenerWhenCircuitBreakerIsNull() throws Exception {
            handler = new MultiDbOutboundHandler(null);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            handler.write(mockContext, asyncCommand, mockPromise);

            // Verify NO listener was attached
            verify(mockPromise, never()).addListener(any());
            // Verify write was still passed down the pipeline
            verify(mockContext).write(asyncCommand, mockPromise);
        }

        @Test
        @DisplayName("Should pass through non-command messages without attaching listeners")
        void shouldPassThroughNonCommandMessagesWithoutAttachingListeners() throws Exception {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            handler = new MultiDbOutboundHandler(circuitBreaker);

            String nonCommandMessage = "not a command";

            handler.write(mockContext, nonCommandMessage, mockPromise);

            // Verify NO listener was attached
            verify(mockPromise, never()).addListener(any());
            // Verify write was passed down the pipeline
            verify(mockContext).write(nonCommandMessage, mockPromise);
        }

    }

    // ============ Write Failure Tracking Tests ============

    @Nested
    @DisplayName("Write Failure Tracking Tests")
    class WriteFailureTrackingTests {

        @Test
        @DisplayName("Should record failure when write promise fails")
        void shouldRecordFailureWhenWritePromiseFails() throws Exception {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            handler = new MultiDbOutboundHandler(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            // Capture the listener attached to the promise
            when(mockPromise.addListener(any())).thenAnswer(invocation -> {
                io.netty.util.concurrent.GenericFutureListener<?> listener = invocation.getArgument(0);

                // Simulate write failure
                Future<?> failedFuture = mock(Future.class);
                when(failedFuture.isSuccess()).thenReturn(false);
                when(failedFuture.cause()).thenReturn(new IOException("Write failed"));

                @SuppressWarnings("unchecked")
                io.netty.util.concurrent.GenericFutureListener<Future<? super Void>> typedListener = (io.netty.util.concurrent.GenericFutureListener<Future<? super Void>>) listener;
                typedListener.operationComplete((Future<? super Void>) failedFuture);

                return mockPromise;
            });

            handler.write(mockContext, asyncCommand, mockPromise);

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(1);
        }

    }

    // ============ Command Completion Tracking Tests ============

    @Nested
    @DisplayName("Command Completion Tracking Tests")
    class CommandCompletionTrackingTests {

        @Test
        @DisplayName("Should record success when command completes successfully after successful write")
        void shouldRecordSuccessWhenCommandCompletesSuccessfully() throws Exception {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            handler = new MultiDbOutboundHandler(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            // Capture the listener attached to the promise
            when(mockPromise.addListener(any())).thenAnswer(invocation -> {
                io.netty.util.concurrent.GenericFutureListener<?> listener = invocation.getArgument(0);

                // Simulate successful write
                Future<?> successFuture = mock(Future.class);
                when(successFuture.isSuccess()).thenReturn(true);

                @SuppressWarnings("unchecked")
                io.netty.util.concurrent.GenericFutureListener<Future<? super Void>> typedListener = (io.netty.util.concurrent.GenericFutureListener<Future<? super Void>>) listener;
                typedListener.operationComplete((Future<? super Void>) successFuture);

                return mockPromise;
            });

            handler.write(mockContext, asyncCommand, mockPromise);

            // Complete command successfully
            asyncCommand.complete();

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getSuccessCount()).isEqualTo(1);
            assertThat(snapshot.getFailureCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should record failure when command completes with non-timeout exception")
        void shouldRecordFailureWhenCommandCompletesWithNonTimeoutException() throws Exception {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            handler = new MultiDbOutboundHandler(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            // Capture the listener attached to the promise
            when(mockPromise.addListener(any())).thenAnswer(invocation -> {
                io.netty.util.concurrent.GenericFutureListener<?> listener = invocation.getArgument(0);

                // Simulate successful write
                Future<?> successFuture = mock(Future.class);
                when(successFuture.isSuccess()).thenReturn(true);

                @SuppressWarnings("unchecked")
                io.netty.util.concurrent.GenericFutureListener<Future<? super Void>> typedListener = (io.netty.util.concurrent.GenericFutureListener<Future<? super Void>>) listener;
                typedListener.operationComplete((Future<? super Void>) successFuture);

                return mockPromise;
            });

            handler.write(mockContext, asyncCommand, mockPromise);

            // Complete command with non-timeout exception
            asyncCommand.completeExceptionally(new RedisConnectionException("Connection failed"));

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(1);
            assertThat(snapshot.getSuccessCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should NOT record timeout exception (handled by DatabaseCommandTracker)")
        void shouldNotRecordTimeoutException() throws Exception {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            handler = new MultiDbOutboundHandler(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            // Capture the listener attached to the promise
            when(mockPromise.addListener(any())).thenAnswer(invocation -> {
                io.netty.util.concurrent.GenericFutureListener<?> listener = invocation.getArgument(0);

                // Simulate successful write
                Future<?> successFuture = mock(Future.class);
                when(successFuture.isSuccess()).thenReturn(true);

                @SuppressWarnings("unchecked")
                io.netty.util.concurrent.GenericFutureListener<Future<? super Void>> typedListener = (io.netty.util.concurrent.GenericFutureListener<Future<? super Void>>) listener;
                typedListener.operationComplete((Future<? super Void>) successFuture);

                return mockPromise;
            });

            handler.write(mockContext, asyncCommand, mockPromise);

            // Complete command with timeout exception
            asyncCommand.completeExceptionally(new RedisCommandTimeoutException("Command timed out"));

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            // Should NOT be recorded by MultiDbOutboundHandler (filtered out)
            assertThat(snapshot.getFailureCount()).isEqualTo(0);
            assertThat(snapshot.getSuccessCount()).isEqualTo(0);
        }

    }

}
