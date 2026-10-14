/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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

    private DefaultEndpoint endpoint;

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
        endpoint = new DefaultEndpoint(ClientOptions.create(), resources);
        handler = new CommandHandler(ClientOptions.create(), resources, endpoint);
        handler.channelRegistered(context);
    }

    @Test
    void recordsAtDispatchBeforeAnyReplyAndKeepsExistingNetworkMetrics() throws Exception {
        Command<String, String, String> command = command();
        endpoint.write(command);

        handler.write(context, command, promise);

        verify(recorder).recordCommandQueueLatency(any(), any(), any(), anyLong());
        assertThat(command.isDone()).isFalse();

        handler.channelRead(context, Unpooled.wrappedBuffer("+OK\r\n".getBytes(StandardCharsets.US_ASCII)));
        verify(recorder).recordCommandLatency(any(), any(), any(RedisCommand.class), anyLong(), anyLong());
    }

    @Test
    void inFlightReplayDoesNotRecordASecondSample() throws Exception {
        Command<String, String, String> command = command();
        endpoint.write(command);

        handler.write(context, command, promise);
        handler.getStack().poll(); // transport replay on another connection, not a new endpoint acceptance
        handler.write(context, command, promise);

        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(), anyLong());
    }

    @Test
    void aNewAcceptedAttemptRecordsAnotherSample() throws Exception {
        Command<String, String, String> command = command();
        endpoint.write(command);
        handler.write(context, command, promise);
        handler.getStack().poll();

        endpoint.write(command); // MOVED/ASK goes through DefaultEndpoint.write again
        handler.write(context, command, promise);

        verify(recorder, times(2)).recordCommandQueueLatency(any(), any(), any(), anyLong());
    }

    @Test
    void cancelledBeforeDispatchDoesNotRecordAQueueSample() throws Exception {
        Command<String, String, String> command = command();
        endpoint.write(command);
        command.cancel();

        handler.write(context, command, promise);

        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong());
    }

    @Test
    void slowQueueRecorderDoesNotInflateNetworkLatency() throws Exception {
        Command<String, String, String> command = command();
        AtomicLong recorderFinishedAt = new AtomicLong();
        doAnswer(invocation -> {
            Thread.sleep(10);
            recorderFinishedAt.set(System.nanoTime());
            return null;
        }).when(recorder).recordCommandQueueLatency(any(), any(), any(), anyLong());

        endpoint.write(command);
        handler.write(context, command, promise);

        WithLatency networkTiming = (WithLatency) handler.getStack().peek();
        assertThat(recorderFinishedAt.get()).isNotZero();
        assertThat(networkTiming.getSent()).isGreaterThanOrEqualTo(recorderFinishedAt.get());
    }

    @Test
    void batchNetworkTimersStartAfterEveryQueueCallback() throws Exception {
        Command<String, String, String> first = command();
        Command<String, String, String> second = command();
        AtomicInteger recordings = new AtomicInteger();
        AtomicLong secondRecorderFinishedAt = new AtomicLong();

        doAnswer(invocation -> {
            if (recordings.incrementAndGet() == 2) {
                secondRecorderFinishedAt.set(System.nanoTime());
            }
            return null;
        }).when(recorder).recordCommandQueueLatency(any(), any(), any(), anyLong());

        endpoint.write(Arrays.asList(first, second));
        handler.write(context, Arrays.asList(first, second), promise);

        assertThat(recordings.get()).isEqualTo(2);
        WithLatency firstNetworkTiming = (WithLatency) handler.getStack().poll();
        WithLatency secondNetworkTiming = (WithLatency) handler.getStack().poll();
        assertThat(firstNetworkTiming.getSent()).isGreaterThanOrEqualTo(secondRecorderFinishedAt.get());
        assertThat(secondNetworkTiming.getSent()).isGreaterThanOrEqualTo(secondRecorderFinishedAt.get());
    }

    @Test
    void cancelledDecoratedCommandIsSkippedEvenWhenOuterIsNotDone() throws Exception {
        AsyncCommand<String, String, String> inner = new AsyncCommand<>(command());
        CommandWrapper<String, String, String> outer = new CommandWrapper<String, String, String>(inner) {

            @Override
            public boolean isDone() {
                return false;
            }
        };

        endpoint.write(outer);
        inner.cancel();
        handler.write(context, outer, promise);

        assertThat(handler.getStack()).isEmpty();
        assertThat(endpoint.takeQueueLatencyStart(outer)).isNull();
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong());
        verify(context, never()).write(any(), any());
    }

    @Test
    void skippedCancelledCommandClearsItsAcceptanceTimestamp() throws Exception {
        Command<String, String, String> command = command();
        endpoint.write(command);
        command.cancel();

        handler.write(context, command, promise);

        assertThat(endpoint.takeQueueLatencyStart(command)).isNull();
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong());
    }

    @Test
    void cancelledCommandSkippedFromBatchClearsItsTimestamp() throws Exception {
        Command<String, String, String> cancelled = command();
        Command<String, String, String> active = command();
        endpoint.write(Arrays.asList(cancelled, active));
        cancelled.cancel();

        handler.write(context, Arrays.asList(cancelled, active), promise);

        assertThat(endpoint.takeQueueLatencyStart(cancelled)).isNull();
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(), anyLong());
    }

    private Command<String, String, String> command() {
        return new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
    }

}
