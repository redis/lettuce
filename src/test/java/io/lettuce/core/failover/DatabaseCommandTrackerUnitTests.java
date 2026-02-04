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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static io.lettuce.TestTags.UNIT_TEST;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.*;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.CircuitBreaker.CircuitBreakerConfig;
import io.lettuce.core.failover.metrics.MetricsSnapshot;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

/**
 * Unit tests for {@link DatabaseCommandTracker} focusing on:
 * <ul>
 * <li>Command write delegation to CommandWriter</li>
 * <li>Circuit breaker integration and state handling</li>
 * <li>Timeout exception tracking via onComplete callbacks</li>
 * <li>Channel registration and pipeline management</li>
 * <li>Single and batch command handling</li>
 * </ul>
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Tag(UNIT_TEST)
class DatabaseCommandTrackerUnitTests {

    private DatabaseCommandTracker.CommandWriter mockWriter;

    private DatabaseCommandTracker tracker;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        mockWriter = mock(DatabaseCommandTracker.CommandWriter.class);
        tracker = new DatabaseCommandTracker(mockWriter);
    }

    @AfterEach
    void tearDown() {
        circuitBreaker = null;
    }

    private CircuitBreakerConfig getCBConfig(float failureRateThreshold, int minimumNumberOfFailures) {
        return CircuitBreakerConfig.builder().failureRateThreshold(failureRateThreshold)
                .minimumNumberOfFailures(minimumNumberOfFailures).build();
    }

    // ============ Basic Write Delegation Tests ============

    @Nested
    @DisplayName("Write Delegation Tests")
    @Tag(UNIT_TEST)
    class WriteDelegationTests {

        @Test
        @DisplayName("Should delegate single command write when circuit breaker is null")
        void shouldDelegateSingleCommandWriteWhenCircuitBreakerIsNull() {
            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            when(mockWriter.writeOne(asyncCommand)).thenReturn(asyncCommand);

            RedisCommand<String, String, String> result = tracker.write(asyncCommand);

            assertThat(result).isSameAs(asyncCommand);
            verify(mockWriter).writeOne(asyncCommand);
        }

        @Test
        @DisplayName("Should delegate batch command write when circuit breaker is null")
        void shouldDelegateBatchCommandWriteWhenCircuitBreakerIsNull() {
            List<RedisCommand<String, String, ?>> commands = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                commands.add(new AsyncCommand<>(command));
            }

            when(mockWriter.writeMany(commands)).thenReturn(commands);

            Collection<RedisCommand<String, String, ?>> result = tracker.write(commands);

            assertThat(result).isSameAs(commands);
            verify(mockWriter).writeMany(commands);
        }

        @Test
        @DisplayName("Should delegate single command write when circuit breaker is closed")
        void shouldDelegateSingleCommandWriteWhenCircuitBreakerIsClosed() {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            tracker.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            when(mockWriter.writeOne(asyncCommand)).thenReturn(asyncCommand);

            RedisCommand<String, String, String> result = tracker.write(asyncCommand);

            assertThat(result).isSameAs(asyncCommand);
            verify(mockWriter).writeOne(asyncCommand);
        }

        @Test
        @DisplayName("Should delegate batch command write when circuit breaker is closed")
        void shouldDelegateBatchCommandWriteWhenCircuitBreakerIsClosed() {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            tracker.bind(circuitBreaker);

            List<RedisCommand<String, String, ?>> commands = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                commands.add(new AsyncCommand<>(command));
            }

            when(mockWriter.writeMany(commands)).thenReturn(commands);

            Collection<RedisCommand<String, String, ?>> result = tracker.write(commands);

            assertThat(result).isSameAs(commands);
            verify(mockWriter).writeMany(commands);
        }

    }

    // ============ Circuit Breaker Open State Tests ============

    @Nested
    @DisplayName("Circuit Breaker Open State Tests")
    @Tag(UNIT_TEST)
    class CircuitBreakerOpenStateTests {

        @Test
        @DisplayName("Should complete single command exceptionally when circuit breaker is open")
        void shouldCompleteSingleCommandExceptionallyWhenCircuitBreakerIsOpen() {
            CircuitBreakerImpl circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 2));
            tracker.bind(circuitBreaker);

            // Force circuit breaker to OPEN
            circuitBreaker.getGeneration().recordResult(new RedisCommandTimeoutException("timeout"));
            circuitBreaker.getGeneration().recordResult(new RedisCommandTimeoutException("timeout"));
            circuitBreaker.evaluateMetrics();

            assertThat(circuitBreaker.isClosed()).isFalse();

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            RedisCommand<String, String, String> result = tracker.write(asyncCommand);

            assertThat(result).isSameAs(asyncCommand);
            assertThat(asyncCommand.isDone()).isTrue();
            assertThat(asyncCommand.isCompletedExceptionally()).isTrue();
            assertThatThrownBy(asyncCommand::get).hasCauseInstanceOf(RedisCircuitBreakerException.class);

            // Should NOT delegate to writer when CB is open
            verify(mockWriter, never()).writeOne(any());
        }

        @Test
        @DisplayName("Should complete all commands exceptionally when circuit breaker is open for batch write")
        void shouldCompleteAllCommandsExceptionallyWhenCircuitBreakerIsOpenForBatchWrite() {
            CircuitBreakerImpl circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 2));
            tracker.bind(circuitBreaker);

            // Force circuit breaker to OPEN
            circuitBreaker.getGeneration().recordResult(new RedisCommandTimeoutException("timeout"));
            circuitBreaker.getGeneration().recordResult(new RedisCommandTimeoutException("timeout"));
            circuitBreaker.evaluateMetrics();

            assertThat(circuitBreaker.isClosed()).isFalse();

            List<AsyncCommand<String, String, String>> commands = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                commands.add(new AsyncCommand<>(command));
            }

            Collection<RedisCommand<String, String, ?>> result = tracker.write(new ArrayList<>(commands));

            assertThat(result).hasSize(5);
            commands.forEach(cmd -> {
                assertThat(cmd.isDone()).isTrue();
                assertThat(cmd.isCompletedExceptionally()).isTrue();
                assertThatThrownBy(cmd::get).hasCauseInstanceOf(RedisCircuitBreakerException.class);
            });

            // Should NOT delegate to writer when CB is open
            verify(mockWriter, never()).writeMany(any());
        }

    }

    // ============ Timeout Exception Tracking Tests ============

    @Nested
    @DisplayName("Timeout Exception Tracking Tests")
    @Tag(UNIT_TEST)
    class TimeoutExceptionTrackingTests {

        @Test
        @DisplayName("Should record timeout exception via onComplete callback for single command")
        void shouldRecordTimeoutExceptionViaOnCompleteCallbackForSingleCommand() {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            tracker.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            when(mockWriter.writeOne(asyncCommand)).thenReturn(asyncCommand);

            tracker.write(asyncCommand);

            // Complete with timeout exception
            asyncCommand.completeExceptionally(new RedisCommandTimeoutException("Command timed out"));

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(1);
            assertThat(snapshot.getSuccessCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should record timeout exceptions via onComplete callback for batch commands")
        void shouldRecordTimeoutExceptionsViaOnCompleteCallbackForBatchCommands() {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            tracker.bind(circuitBreaker);

            List<AsyncCommand<String, String, String>> commands = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                commands.add(new AsyncCommand<>(command));
            }

            when(mockWriter.writeMany(any())).thenReturn((Collection) commands);

            tracker.write(new ArrayList<>(commands));

            // Complete all with timeout exceptions
            commands.forEach(cmd -> cmd.completeExceptionally(new RedisCommandTimeoutException("timeout")));

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(5);
            assertThat(snapshot.getSuccessCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should NOT record non-timeout exceptions via onComplete callback")
        void shouldNotRecordNonTimeoutExceptionsViaOnCompleteCallback() {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            tracker.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            when(mockWriter.writeOne(asyncCommand)).thenReturn(asyncCommand);

            tracker.write(asyncCommand);

            // Complete with non-timeout exception (should be tracked by MultiDbOutboundHandler, not here)
            asyncCommand.completeExceptionally(new RuntimeException("Other error"));

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            // Should NOT be recorded by DatabaseCommandTracker's onComplete callback
            assertThat(snapshot.getFailureCount()).isEqualTo(0);
        }

    }

    // ============ Channel Registration Tests ============

    @Nested
    @DisplayName("Channel Registration Tests")
    @Tag(UNIT_TEST)
    class ChannelRegistrationTests {

        @Test
        @DisplayName("Should register handler to pipeline when channel is set after bind")
        void shouldRegisterHandlerToPipelineWhenChannelIsSetAfterBind() {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            tracker.bind(circuitBreaker);

            Channel mockChannel = mock(Channel.class);
            ChannelPipeline mockPipeline = mock(ChannelPipeline.class);
            ChannelHandlerContext mockContext = mock(ChannelHandlerContext.class);

            when(mockChannel.pipeline()).thenReturn(mockPipeline);
            when(mockPipeline.context(CommandHandler.class)).thenReturn(mockContext);
            when(mockContext.name()).thenReturn("CommandHandler#0");

            tracker.setChannel(mockChannel);

            verify(mockPipeline).addAfter(eq("CommandHandler#0"), eq(MultiDbOutboundHandler.HANDLER_NAME), any());
        }

        @Test
        @DisplayName("Should not register handler when channel is set before bind")
        void shouldNotRegisterHandlerWhenChannelIsSetBeforeBind() {
            Channel mockChannel = mock(Channel.class);
            ChannelPipeline mockPipeline = mock(ChannelPipeline.class);

            when(mockChannel.pipeline()).thenReturn(mockPipeline);

            // Set channel before bind - should not register handler
            tracker.setChannel(mockChannel);

            verify(mockPipeline, never()).addAfter(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should remove handler from pipeline when channel is reset")
        void shouldRemoveHandlerFromPipelineWhenChannelIsReset() {
            Channel mockChannel = mock(Channel.class);
            ChannelPipeline mockPipeline = mock(ChannelPipeline.class);

            when(mockChannel.pipeline()).thenReturn(mockPipeline);
            when(mockPipeline.get(MultiDbOutboundHandler.class)).thenReturn(new MultiDbOutboundHandler(null));

            tracker.resetChannel(mockChannel);

            verify(mockPipeline).get(MultiDbOutboundHandler.class);
            verify(mockPipeline).remove(MultiDbOutboundHandler.class);
        }

    }

    // ============ Exception Handling Tests ============

    @Nested
    @DisplayName("Exception Handling Tests")
    @Tag(UNIT_TEST)
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should record exception thrown during write")
        void shouldRecordExceptionThrownDuringWrite() {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            tracker.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            // Use a tracked exception type (RedisCommandTimeoutException is in the default tracked exceptions)
            RedisConnectionException writeException = new RedisConnectionException("Write failed");
            when(mockWriter.writeOne(asyncCommand)).thenThrow(writeException);

            assertThatThrownBy(() -> tracker.write(asyncCommand)).isSameAs(writeException);

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should record exception thrown during batch write")
        void shouldRecordExceptionThrownDuringBatchWrite() {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            tracker.bind(circuitBreaker);

            List<RedisCommand<String, String, ?>> commands = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                commands.add(new AsyncCommand<>(command));
            }

            // Use a tracked exception type (RedisCommandTimeoutException is in the default tracked exceptions)
            RedisConnectionException writeException = new RedisConnectionException("Batch write failed");
            when(mockWriter.writeMany(commands)).thenThrow(writeException);

            assertThatThrownBy(() -> tracker.write(commands)).isSameAs(writeException);

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(1);
        }

    }

    // ============ Success Tracking Tests ============

    @Nested
    @DisplayName("Success Tracking Tests")
    @Tag(UNIT_TEST)
    class SuccessTrackingTests {

        @Test
        @DisplayName("Should NOT record success via onComplete callback (handled by MultiDbOutboundHandler)")
        void shouldNotRecordSuccessViaOnCompleteCallback() {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            tracker.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            when(mockWriter.writeOne(asyncCommand)).thenReturn(asyncCommand);

            tracker.write(asyncCommand);

            // Complete successfully
            asyncCommand.complete();

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            // Success should NOT be recorded by DatabaseCommandTracker's onComplete callback
            // It should be recorded by MultiDbOutboundHandler
            assertThat(snapshot.getSuccessCount()).isEqualTo(0);
        }

    }

    // ============ Callback Attachment Tests ============

    @Nested
    @DisplayName("Callback Attachment Tests")
    @Tag(UNIT_TEST)
    class CallbackAttachmentTests {

        @Test
        @DisplayName("Should attach onComplete callback to single command when circuit breaker is bound")
        void shouldAttachOnCompleteCallbackToSingleCommand() {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            tracker.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));

            AtomicInteger onCompleteCallCount = new AtomicInteger(0);
            AsyncCommand<String, String, String> spyCommand = new AsyncCommand<String, String, String>(command) {

                @Override
                public void onComplete(java.util.function.BiConsumer<? super String, Throwable> action) {
                    onCompleteCallCount.incrementAndGet();
                    super.onComplete(action);
                }

            };

            when(mockWriter.writeOne(spyCommand)).thenReturn(spyCommand);

            tracker.write(spyCommand);

            // Verify onComplete was called (callback attached)
            assertThat(onCompleteCallCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should attach onComplete callbacks to all commands in batch")
        void shouldAttachOnCompleteCallbacksToAllCommandsInBatch() {
            circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            tracker.bind(circuitBreaker);

            AtomicInteger totalOnCompleteCalls = new AtomicInteger(0);
            List<AsyncCommand<String, String, String>> spyCommands = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> spyCommand = new AsyncCommand<String, String, String>(command) {

                    @Override
                    public void onComplete(java.util.function.BiConsumer<? super String, Throwable> action) {
                        totalOnCompleteCalls.incrementAndGet();
                        super.onComplete(action);
                    }

                };
                spyCommands.add(spyCommand);
            }

            when(mockWriter.writeMany(any())).thenReturn((Collection) spyCommands);

            tracker.write(new ArrayList<>(spyCommands));

            // Verify onComplete was called for all commands
            assertThat(totalOnCompleteCalls.get()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should not attach callback when circuit breaker is null")
        void shouldNotAttachCallbackWhenCircuitBreakerIsNull() {
            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));

            AtomicInteger onCompleteCallCount = new AtomicInteger(0);
            AsyncCommand<String, String, String> spyCommand = new AsyncCommand<String, String, String>(command) {

                @Override
                public void onComplete(java.util.function.BiConsumer<? super String, Throwable> action) {
                    onCompleteCallCount.incrementAndGet();
                    super.onComplete(action);
                }

            };

            when(mockWriter.writeOne(spyCommand)).thenReturn(spyCommand);

            tracker.write(spyCommand);

            // Verify onComplete was NOT called (no callback attached)
            assertThat(onCompleteCallCount.get()).isEqualTo(0);
        }

    }

}
