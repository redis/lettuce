/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisException;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.metrics.CommandLatencyRecorder;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.TraceContext;
import io.lettuce.core.tracing.TraceContextProvider;
import io.lettuce.core.tracing.Tracer;
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

    private ClientResources resources;

    private DefaultEndpoint endpoint;

    private CommandHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        resources = mock(ClientResources.class);
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
        handler.flush(context);

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
    void cancelledDuringBatchPreparationDoesNotStartAnOrphanTracingSpan() throws Exception {
        Tracing tracing = mock(Tracing.class);
        TraceContextProvider provider = mock(TraceContextProvider.class);
        TraceContext traceContext = mock(TraceContext.class);
        Tracer tracer = mock(Tracer.class);
        Tracer.Span span = mock(Tracer.Span.class);
        when(tracing.isEnabled()).thenReturn(true);
        when(tracing.initialTraceContextProvider()).thenReturn(provider);
        when(provider.getTraceContext()).thenReturn(traceContext);
        when(tracing.getTracerProvider()).thenReturn(() -> tracer);
        when(tracer.nextSpan(traceContext)).thenReturn(span);
        when(tracing.createEndpoint(any())).thenReturn(mock(Tracing.Endpoint.class));
        when(span.name(any())).thenReturn(span);

        // Use enabled tracing, unlike the default test fixture.
        when(resources.tracing()).thenReturn(tracing);
        CommandHandler tracingHandler = new CommandHandler(ClientOptions.create(), resources, endpoint);
        tracingHandler.channelRegistered(context);

        Command<String, String, String> cancelled = new Command<>(CommandType.APPEND,
                new StatusOutput<>(StringCodec.UTF8), null) {
            @Override
            public CommandOutput<String, String, String> getOutput() {
                cancel();
                return super.getOutput();
            }
        };
        Command<String, String, String> accepted = command();

        tracingHandler.write(context, Arrays.asList(cancelled, accepted), promise);

        assertThat(cancelled.isCancelled()).isTrue();
        assertThat(tracingHandler.getStack()).hasSize(1);
        verify(tracer, times(1)).nextSpan(traceContext);
        verify(span).start(accepted);
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
        handler.flush(context);

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
        handler.flush(context);

        assertThat(recordings.get()).isEqualTo(2);
        WithLatency firstNetworkTiming = (WithLatency) handler.getStack().poll();
        WithLatency secondNetworkTiming = (WithLatency) handler.getStack().poll();
        assertThat(firstNetworkTiming.getSent()).isGreaterThanOrEqualTo(secondRecorderFinishedAt.get());
        assertThat(secondNetworkTiming.getSent()).isGreaterThanOrEqualTo(secondRecorderFinishedAt.get());
    }

    @Test
    void separatelyWrittenBatchCommandsStartNetworkTimersTogetherAtFlush() throws Exception {
        Command<String, String, String> first = command();
        Command<String, String, String> second = command();
        AtomicInteger recordings = new AtomicInteger();
        AtomicLong finalRecordingFinishedAt = new AtomicLong();

        doAnswer(invocation -> {
            if (recordings.incrementAndGet() == 2) {
                finalRecordingFinishedAt.set(System.nanoTime());
            }
            return null;
        }).when(recorder).recordCommandQueueLatency(any(), any(), any(), anyLong());

        endpoint.write(Arrays.asList(first, second));
        // Matches DefaultEndpoint.writeToChannelAndFlush(Collection): individual writes, one flush.
        handler.write(context, first, promise);
        handler.write(context, second, promise);
        WithLatency firstTiming = (WithLatency) handler.getStack().peek();
        assertThat(firstTiming.getSent()).isZero();

        handler.flush(context);
        assertThat(recordings.get()).isEqualTo(2);
        assertThat(firstTiming.getSent()).isGreaterThanOrEqualTo(finalRecordingFinishedAt.get());
    }

    @Test
    void cancellationInRecorderOccursAfterWriteHasBeenSubmitted() throws Exception {
        Command<String, String, String> command = command();
        doAnswer(invocation -> {
            command.cancel();
            return null;
        }).when(recorder).recordCommandQueueLatency(any(), any(), any(), anyLong());

        endpoint.write(command);
        handler.write(context, command, promise);

        verify(context).write(command, promise);
        verify(recorder).recordCommandQueueLatency(any(), any(), any(), anyLong());
    }

    @Test
    void cancellationInBatchRecorderOccursAfterBatchWriteHasBeenSubmitted() throws Exception {
        Command<String, String, String> first = command();
        Command<String, String, String> second = command();
        AtomicInteger recordings = new AtomicInteger();
        doAnswer(invocation -> {
            if (recordings.incrementAndGet() == 2) {
                second.cancel();
            }
            return null;
        }).when(recorder).recordCommandQueueLatency(any(), any(), any(), anyLong());

        endpoint.write(Arrays.asList(first, second));
        handler.write(context, Arrays.asList(first, second), promise);

        assertThat(handler.getStack()).hasSize(2);
        assertThat(recordings.get()).isEqualTo(2);
        verify(context).write(any(), any());
    }


    @Test
    void failedSingleWriteDoesNotGetNetworkTimestampOnLaterFlush() throws Exception {
        Command<String, String, String> accepted = command();
        Command<String, String, String> rejected = command();
        endpoint.write(Arrays.asList(accepted, rejected));

        handler.write(context, accepted, promise);
        WithLatency acceptedTiming = (WithLatency) handler.getStack().peek();

        doAnswer(invocation -> {
            throw new IllegalStateException("transport refused write");
        }).when(context).write(org.mockito.ArgumentMatchers.same(rejected), any());

        assertThatThrownBy(() -> handler.write(context, rejected, promise))
                .isInstanceOf(IllegalStateException.class);
        WithLatency rejectedTiming = (WithLatency) handler.getStack().toArray()[1];
        assertThat(acceptedTiming.getSent()).isZero();
        assertThat(rejectedTiming.getSent()).isZero();

        handler.flush(context);

        assertThat(acceptedTiming.getSent()).isPositive();
        assertThat(rejectedTiming.getSent()).isZero();
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(), anyLong());
    }

    @Test
    void failedBatchWriteDoesNotTimestampItsCommandsOrEarlierSuccessfulWrites() throws Exception {
        Command<String, String, String> accepted = command();
        Command<String, String, String> rejectedOne = command();
        Command<String, String, String> rejectedTwo = command();
        endpoint.write(Arrays.asList(accepted, rejectedOne, rejectedTwo));

        handler.write(context, accepted, promise);
        WithLatency acceptedTiming = (WithLatency) handler.getStack().peek();

        doAnswer(invocation -> {
            throw new IllegalStateException("batch transport refused write");
        }).when(context).write(org.mockito.ArgumentMatchers.argThat(value -> value instanceof java.util.Collection), any());

        assertThatThrownBy(() -> handler.write(context, Arrays.asList(rejectedOne, rejectedTwo), promise))
                .isInstanceOf(IllegalStateException.class);

        Object[] stacked = handler.getStack().toArray();
        WithLatency firstRejectedTiming = (WithLatency) stacked[1];
        WithLatency secondRejectedTiming = (WithLatency) stacked[2];

        handler.flush(context);

        assertThat(acceptedTiming.getSent()).isPositive();
        assertThat(firstRejectedTiming.getSent()).isZero();
        assertThat(secondRejectedTiming.getSent()).isZero();
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(), anyLong());
    }

    @Test
    void successfulWriteAfterFailedBatchStillGetsNetworkTimestamp() throws Exception {
        Command<String, String, String> rejectedOne = command();
        Command<String, String, String> rejectedTwo = command();
        Command<String, String, String> acceptedLater = command();
        endpoint.write(Arrays.asList(rejectedOne, rejectedTwo, acceptedLater));

        doAnswer(invocation -> {
            throw new IllegalStateException("batch transport refused write");
        }).when(context).write(org.mockito.ArgumentMatchers.argThat(value -> value instanceof java.util.Collection), any());

        assertThatThrownBy(() -> handler.write(context, Arrays.asList(rejectedOne, rejectedTwo), promise))
                .isInstanceOf(IllegalStateException.class);
        handler.write(context, acceptedLater, promise);

        Object[] stacked = handler.getStack().toArray();
        handler.flush(context);
        assertThat(((WithLatency) stacked[0]).getSent()).isZero();
        assertThat(((WithLatency) stacked[1]).getSent()).isZero();
        assertThat(((WithLatency) stacked[2]).getSent()).isPositive();
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(), anyLong());
    }

    @Test
    void noQueueSampleWhenTransportWriteThrows() throws Exception {
        Command<String, String, String> command = command();
        doAnswer(invocation -> {
            throw new IllegalStateException("outbound write rejected");
        }).when(context).write(any(), any());

        endpoint.write(command);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> handler.write(context, command, promise))
                .isInstanceOf(IllegalStateException.class);

        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong());
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

    @Test
    void outboundEncodingIsNotCountedAsQueueLatency() throws Exception {
        Command<String, String, String> command = command();
        AtomicLong writeStartedAt = new AtomicLong();
        doAnswer(invocation -> {
            writeStartedAt.set(System.nanoTime());
            Thread.sleep(75);
            return null;
        }).when(context).write(any(), any());

        long beforeAcceptance = System.nanoTime();
        endpoint.write(command);
        handler.write(context, command, promise);

        ArgumentCaptor<Long> recorded = ArgumentCaptor.forClass(Long.class);
        verify(recorder).recordCommandQueueLatency(any(), any(), any(), recorded.capture());
        assertThat(writeStartedAt.get()).isPositive();
        // The acceptance timestamp is no earlier than beforeAcceptance and dispatch
        // starts before the synchronous outbound encoder is invoked.
        assertThat(recorded.getValue()).isLessThanOrEqualTo(writeStartedAt.get() - beforeAcceptance);
    }

    @Test
    void batchEncodingIsNotCountedAsQueueLatency() throws Exception {
        Command<String, String, String> first = command();
        Command<String, String, String> second = command();
        AtomicLong writeStartedAt = new AtomicLong();
        doAnswer(invocation -> {
            writeStartedAt.set(System.nanoTime());
            Thread.sleep(75);
            return null;
        }).when(context).write(any(), any());

        long beforeAcceptance = System.nanoTime();
        endpoint.write(Arrays.asList(first, second));
        handler.write(context, Arrays.asList(first, second), promise);

        ArgumentCaptor<Long> recorded = ArgumentCaptor.forClass(Long.class);
        verify(recorder, times(2)).recordCommandQueueLatency(any(), any(), any(), recorded.capture());
        assertThat(writeStartedAt.get()).isPositive();
        for (Long queueLatency : recorded.getAllValues()) {
            assertThat(queueLatency).isLessThanOrEqualTo(writeStartedAt.get() - beforeAcceptance);
        }
    }

    @Test
    void rejectedPlainCommandDoesNotRetainAnAcceptanceStamp() throws Exception {
        ClientOptions bounded = ClientOptions.builder().requestQueueSize(1).build();
        DefaultEndpoint boundedEndpoint = new DefaultEndpoint(bounded, resources);
        CommandHandler boundedHandler = new CommandHandler(bounded, resources, boundedEndpoint);
        boundedHandler.channelRegistered(context);

        boundedHandler.write(context, command(), promise); // Fill the handler's bounded stack.
        Command<String, String, String> rejected = command();
        boundedEndpoint.write(rejected);

        assertThatThrownBy(() -> boundedHandler.write(context, rejected, promise)).isInstanceOf(RedisException.class);
        assertThat(boundedEndpoint.takeQueueLatencyStart(rejected)).isNull();
    }

    @Test
    void rejectedBatchDoesNotRetainPlainCommandAcceptanceStamp() throws Exception {
        ClientOptions bounded = ClientOptions.builder().requestQueueSize(1).build();
        DefaultEndpoint boundedEndpoint = new DefaultEndpoint(bounded, resources);
        CommandHandler boundedHandler = new CommandHandler(bounded, resources, boundedEndpoint);
        boundedHandler.channelRegistered(context);

        boundedHandler.write(context, command(), promise);
        Command<String, String, String> rejected = command();
        boundedEndpoint.write(rejected);

        assertThatThrownBy(() -> boundedHandler.write(context, Collections.singleton(rejected), promise))
                .isInstanceOf(RedisException.class);
        assertThat(boundedEndpoint.takeQueueLatencyStart(rejected)).isNull();
    }

    @Test
    void outputLessCommandRecordsQueueLatencyOnDispatch() throws Exception {
        Command<String, String, String> fireAndForget = new Command<>(CommandType.PING, null);
        endpoint.write(fireAndForget);

        handler.write(context, fireAndForget, promise);

        verify(context).write(fireAndForget, promise);
        verify(recorder).recordCommandQueueLatency(any(), any(), org.mockito.ArgumentMatchers.same(fireAndForget),
                anyLong());
        assertThat(endpoint.takeQueueLatencyStart(fireAndForget)).isNull();
    }

    @Test
    void outputLessBatchCommandRecordsQueueLatencyOnDispatch() throws Exception {
        Command<String, String, String> fireAndForget = new Command<>(CommandType.PING, null);
        Command<String, String, String> regular = command();
        endpoint.write(Arrays.asList(fireAndForget, regular));

        handler.write(context, Arrays.asList(fireAndForget, regular), promise);

        verify(recorder).recordCommandQueueLatency(any(), any(), org.mockito.ArgumentMatchers.same(fireAndForget),
                anyLong());
        verify(recorder, times(2)).recordCommandQueueLatency(any(), any(), any(), anyLong());
    }

    @Test
    void cancelledOutputLessCommandDoesNotRecordBeforeDispatch() throws Exception {
        Command<String, String, String> fireAndForget = new Command<>(CommandType.PING, null);
        endpoint.write(fireAndForget);
        fireAndForget.cancel();

        handler.write(context, fireAndForget, promise);

        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong());
        assertThat(endpoint.takeQueueLatencyStart(fireAndForget)).isNull();
    }

    @Test
    void duplicatePlainCommandInBatchReleasesAcceptanceStamp() throws Exception {
        Command<String, String, String> duplicate = command();
        endpoint.write(Arrays.asList(duplicate, duplicate));

        handler.write(context, Arrays.asList(duplicate, duplicate), promise);

        assertThat(duplicate.exception).isInstanceOf(RedisException.class);
        assertThat(endpoint.takeQueueLatencyStart(duplicate)).isNull();
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong());
        verify(context, never()).write(any(), any());
    }

    private Command<String, String, String> command() {
        return new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
    }

}
