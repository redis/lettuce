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
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.metrics.CommandLatencyRecorder;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

@Tag(UNIT_TEST)
class CommandHandlerQueueLatencyUnitTests {

    private final CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);

    private final ChannelHandlerContext context = mock(ChannelHandlerContext.class);

    private final Channel channel = mock(Channel.class);

    private final ChannelPromise promise = mock(ChannelPromise.class);

    private CommandHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        ClientResources resources = mock(ClientResources.class);
        when(resources.commandLatencyRecorder()).thenReturn(recorder);
        when(resources.tracing()).thenReturn(Tracing.disabled());
        when(recorder.isEnabled()).thenReturn(true);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(context.channel()).thenReturn(channel);
        when(context.alloc()).thenReturn(ByteBufAllocator.DEFAULT);
        when(channel.config()).thenReturn(mock(ChannelConfig.class));
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 6379));
        when(channel.localAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 1234));
        when(promise.isVoid()).thenReturn(true);
        handler = new CommandHandler(ClientOptions.create(), resources, mock(Endpoint.class));
        handler.channelRegistered(context);
    }

    @AfterEach
    void tearDown() throws Exception {
        handler.channelUnregistered(context);
    }

    @Test
    void shouldRecordQueueingAndEndToEndAfterAsyncCompletion() throws Exception {
        AsyncCommand<String, String, String> command = new AsyncCommand<>(command());
        long submittedAt = System.nanoTime() - 1000;
        AsyncCommand.markEnqueued(command, submittedAt);
        AsyncCommand.markSent(command, submittedAt + 250);

        handler.write(context, command, promise);
        reply("+OK\r\n");

        assertThat(command).isCompletedWithValue("OK");
        assertQueueMetrics(250);
        verify(recorder).recordCommandLatency(any(), any(), any(RedisCommand.class), anyLong(), anyLong());
    }

    @Test
    void shouldRecordDecoratedPlainCommandUsedByReactiveDispatch() throws Exception {
        CommandWrapper<String, String, String> decorated = new CommandWrapper<>(new CommandWrapper<>(command()));
        long submittedAt = System.nanoTime() - 1000;
        AsyncCommand.markEnqueued(decorated, submittedAt);
        AsyncCommand.markSent(decorated, submittedAt + 350);

        handler.write(context, decorated, promise);
        reply("+OK\r\n");

        assertThat(decorated.isDone()).isTrue();
        assertQueueMetrics(350);
    }

    @Test
    void shouldRecordOnlyFinalResponseWhenCompletionRetriesCommand() throws Exception {
        RedisCommand<String, String, String> underlying = command();
        CommandWrapper<String, String, String> retrying = new CommandWrapper<String, String, String>(underlying) {

            private boolean retried;

            @Override
            public void complete() {
                if (!retried) {
                    retried = true;
                    try {
                        handler.write(context, this, promise);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                    return;
                }
                super.complete();
            }

        };
        long submittedAt = System.nanoTime() - 1000;
        AsyncCommand.markEnqueued(retrying, submittedAt);
        AsyncCommand.markSent(retrying, submittedAt + 150);
        handler.write(context, retrying, promise);

        reply("+RETRY\r\n");

        assertThat(underlying.isDone()).isFalse();
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
        reply("+OK\r\n");

        assertThat(underlying.isDone()).isTrue();
        assertQueueMetrics(150);
        verify(recorder, times(2)).recordCommandLatency(any(), any(), any(RedisCommand.class), anyLong(), anyLong());
    }

    @Test
    void shouldNotRecordQueueMetricsForCommandCancelledAfterSend() throws Exception {
        AsyncCommand<String, String, String> command = new AsyncCommand<>(command());
        AsyncCommand.markEnqueued(command, System.nanoTime() - 1000);
        handler.write(context, command, promise);
        command.cancel();

        reply("+OK\r\n");

        assertThat(command).isCancelled();
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void shouldNotRecordQueueMetricsWhenAsyncCancellationWinsDuringCompletion() throws Exception {
        AsyncCommand<String, String, String> command = new AsyncCommand<String, String, String>(command()) {

            @Override
            public void complete() {
                // Deterministically cancel after the handler's cancellation snapshot and before future completion.
                cancel();
                super.complete();
            }

        };
        AsyncCommand.markEnqueued(command, System.nanoTime() - 1000);
        handler.write(context, command, promise);

        reply("+OK\r\n");

        assertThat(command).isCancelled();
        verify(recorder).recordCommandLatency(any(), any(), any(RedisCommand.class), anyLong(), anyLong());
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void shouldNotRecordQueueMetricsWhenPlainCommandCompletionOverwritesCancellation() throws Exception {
        RedisCommand<String, String, String> command = command();
        AsyncCommand.markEnqueued(command, System.nanoTime() - 1000);
        handler.write(context, command, promise);
        command.cancel();

        reply("+OK\r\n");

        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void shouldNotRecordDiscardedTransactionWhenQueuedReplyArrivesAfterLocalDiscard() throws Exception {
        AsyncCommand<String, String, String> userCommand = new AsyncCommand<>(command());
        TransactionalCommand<String, String, String> transaction = new TransactionalCommand<>(userCommand);
        AsyncCommand.markEnqueued(transaction, System.nanoTime() - 1000);
        handler.write(context, transaction, promise);

        // DISCARD completes pending transaction entries locally before their QUEUED reply is decoded.
        transaction.complete();
        assertThat(userCommand).isNotDone();
        reply("+QUEUED\r\n");

        assertThat(transaction).isDone();
        assertThat(userCommand).isDone();
        verify(recorder).recordCommandLatency(any(), any(), any(RedisCommand.class), anyLong(), anyLong());
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void shouldPreserveLegacyMetricsForCommandWithoutSubmissionTimestamp() throws Exception {
        handler.write(context, command(), promise);

        reply("+OK\r\n");

        verify(recorder).recordCommandLatency(any(), any(), any(RedisCommand.class), anyLong(), anyLong());
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void shouldAcceptNegativeNanoTimeOrigins() throws Exception {
        RedisCommand<String, String, String> command = command();
        AsyncCommand.markEnqueued(command, -200);
        AsyncCommand.markSent(command, -150);
        handler.write(context, command, promise);

        reply("+OK\r\n");

        assertQueueMetrics(50);
    }

    @Test
    void shouldHandleSignedClockWrapWhenComputingQueueDuration() throws Exception {
        RedisCommand<String, String, String> command = command();
        // A valid short queue interval crosses the signed long boundary.
        AsyncCommand.markEnqueued(command, Long.MAX_VALUE - 25);
        AsyncCommand.markSent(command, Long.MIN_VALUE + 24);
        handler.write(context, command, promise);

        reply("+OK\r\n");

        // The actual decoder clock is outside this synthetic < 2^63 end-to-end interval.
        // Rejecting the end-to-end duration must never produce a negative timer observation.
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
        assertThat(AsyncCommand.firstSent(command) - AsyncCommand.queuedAt(command)).isEqualTo(50);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void shouldAttributeReplayMetricsToFirstSendConnection(boolean async) throws Exception {
        RedisCommand<String, String, String> command = async ? new AsyncCommand<>(command()) : command();
        InetSocketAddress firstLocal = new InetSocketAddress("127.0.0.1", 1234);
        InetSocketAddress firstRemote = new InetSocketAddress("127.0.0.1", 6379);
        AsyncCommand.markEnqueued(command, System.nanoTime() - 100_000);

        handler.write(context, command, promise);
        long firstSent = AsyncCommand.firstSent(command);
        assertThat(firstSent).isNotZero();

        // Simulate an in-flight command moved to a different channel after reconnect or redirect.
        handler.getStack().poll();
        when(channel.localAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 2345));
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 6380));
        handler.write(context, command, promise);
        reply("+OK\r\n");

        assertThat(command.isDone()).isTrue();
        assertThat(AsyncCommand.firstSent(command)).isEqualTo(firstSent);
        verify(recorder).recordCommandQueueLatency(eq(firstLocal), eq(firstRemote), any(RedisCommand.class), anyLong(),
                anyLong());
    }

    @Test
    void shouldRecordCompletedProtectedModeErrorButNotPendingCommands() throws Exception {
        AsyncCommand<String, String, String> denied = new AsyncCommand<>(command());
        AsyncCommand<String, String, String> pending = new AsyncCommand<>(command());
        AsyncCommand.markEnqueued(denied, System.nanoTime() - 100_000);
        AsyncCommand.markEnqueued(pending, System.nanoTime() - 100_000);
        handler.write(context, denied, promise);
        handler.write(context, pending, promise);

        reply("-DENIED Redis is running in protected mode\r\n");

        assertThat(denied.isCompletedExceptionally()).isTrue();
        assertThat(pending.isCompletedExceptionally()).isTrue();
        assertThat(handler.getStack()).isEmpty();
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(RedisCommand.class), anyLong(), anyLong());
    }

    @Test
    void shouldCountSuccessfulReactiveCompletionEvenIfDeliveryCancelsUnderlyingCommand() throws Exception {
        RedisCommand<String, String, String> underlying = command();
        CommandWrapper<String, String, String> reactive = new CommandWrapper<String, String, String>(underlying) {

            @Override
            protected void doOnComplete() {
                // RedisPublisher's immediate subscriber cancels the underlying command after onNext.
                getDelegate().cancel();
            }

        };
        AsyncCommand.markEnqueued(reactive, System.nanoTime() - 100_000);
        handler.write(context, reactive, promise);

        reply("+OK\r\n");

        assertThat(underlying.isCancelled()).isTrue();
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(RedisCommand.class), anyLong(), anyLong());
    }

    private Command<String, String, String> command() {
        return new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
    }

    private void reply(String response) throws Exception {
        handler.channelRead(context, Unpooled.wrappedBuffer(response.getBytes(StandardCharsets.US_ASCII)));
    }

    private void assertQueueMetrics(long expectedQueueLatency) {
        ArgumentCaptor<Long> queueLatency = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> endToEndLatency = ArgumentCaptor.forClass(Long.class);
        verify(recorder).recordCommandQueueLatency(any(), any(), any(RedisCommand.class), queueLatency.capture(),
                endToEndLatency.capture());
        assertThat(queueLatency.getValue()).isEqualTo(expectedQueueLatency);
        assertThat(endToEndLatency.getValue()).isGreaterThanOrEqualTo(expectedQueueLatency);
    }

}
