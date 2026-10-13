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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisReactiveCommandsImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.metrics.CommandLatencyRecorder;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.output.MultiOutput;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import reactor.core.Disposable;

@Tag(UNIT_TEST)
class ReactiveCommandQueueLatencyUnitTests {

    private final CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);

    private final ChannelHandlerContext context = mock(ChannelHandlerContext.class);

    private final Channel channel = mock(Channel.class);

    private final ChannelPromise promise = mock(ChannelPromise.class);

    private CommandHandler handler;

    private RedisReactiveCommandsImpl<String, String> reactive;

    private Runnable beforeComplete = () -> {
    };

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
        handler = new CommandHandler(ClientOptions.create(), resources, mock(Endpoint.class)) {

            @Override
            protected void complete(RedisCommand<?, ?, ?> command) {
                beforeComplete.run();
                super.complete(command);
            }

        };
        handler.channelRegistered(context);
        StatefulRedisConnection<String, String> connection = mock(StatefulRedisConnection.class);
        when(connection.getOptions()).thenReturn(ClientOptions.create());
        when(connection.getResources()).thenReturn(resources);
        doAnswer(invocation -> {
            RedisCommand<?, ?, ?> command = invocation.getArgument(0);
            AsyncCommand.markEnqueued(command, System.nanoTime() - 100_000);
            handler.write(context, command, promise);
            return command;
        }).when(connection).dispatch(any(RedisCommand.class));
        reactive = new RedisReactiveCommandsImpl<>(connection, StringCodec.UTF8);
    }

    @AfterEach
    void tearDown() throws Exception {
        handler.channelUnregistered(context);
    }

    @Test
    void actualMonoShouldRecordDecodedValue() throws Exception {
        List<String> values = new ArrayList<>();
        reactive.echo("OK").subscribe(values::add);
        reply("$2\r\nOK\r\n");
        assertThat(values).containsExactly("OK");
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void earlyCancelledMonoShouldNotRecordNormalReply() throws Exception {
        List<String> values = new ArrayList<>();
        Disposable subscription = reactive.echo("OK").subscribe(values::add);
        subscription.dispose();
        reply("$2\r\nOK\r\n");
        assertThat(values).isEmpty();
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void earlyCancelledMonoShouldNotRecordProtectedModeReply() throws Exception {
        List<String> values = new ArrayList<>();
        Disposable subscription = reactive.echo("OK").subscribe(values::add);
        subscription.dispose();
        reply("-DENIED Redis is running in protected mode\r\n");
        assertThat(values).isEmpty();
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void cancellationWinningBeforeValueDeliveryShouldNotRecord() throws Exception {
        List<String> values = new ArrayList<>();
        Disposable subscription = reactive.echo("OK").subscribe(values::add);
        beforeComplete = subscription::dispose;
        reply("$2\r\nOK\r\n");
        assertThat(values).isEmpty();
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void streamingFluxTakeOneBeforeCompleteDecodeShouldNotRecord() throws Exception {
        List<String> values = new ArrayList<>();
        reactive.lrange("key", 0, -1).take(1).subscribe(values::add);
        reply("*2\r\n$1\r\na\r\n");
        assertThat(values).containsExactly("a");
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
        reply("$1\r\nb\r\n");
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void actualDissolvedFluxTakeOneShouldRecordFullyDecodedReply() throws Exception {
        List<Object> values = new ArrayList<>();
        reactive.slowlogGet().take(1).subscribe(values::add);
        reply("*2\r\n:1\r\n:2\r\n");
        assertThat(values).containsExactly(1L);
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void fullStreamingFluxShouldRecord() throws Exception {
        List<String> values = new ArrayList<>();
        reactive.lrange("key", 0, -1).subscribe(values::add);
        reply("*2\r\n$1\r\na\r\n$1\r\nb\r\n");
        assertThat(values).containsExactly("a", "b");
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void actualRedisErrorShouldRecord() throws Exception {
        List<Throwable> errors = new ArrayList<>();
        reactive.echo("OK").subscribe(value -> {
        }, errors::add);
        reply("-ERR rejected\r\n");
        assertThat(errors).hasSize(1);
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void cancellationAfterAsyncFutureCompletionShouldNotLoseSample() throws Exception {
        AsyncCommand<String, String, String> command = new AsyncCommand<>(
                new Command<>(CommandType.ECHO, new StatusOutput<>(StringCodec.UTF8)));
        command.whenComplete((value, error) -> assertThat(command.cancel(false)).isFalse());
        AsyncCommand.markEnqueued(command, System.nanoTime() - 100_000);
        handler.write(context, command, promise);

        reply("+OK\r\n");

        assertThat(command).isCompletedWithValue("OK");
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void cancellationAfterTransactionalUserFutureCompletionShouldNotLoseSample() throws Exception {
        AsyncCommand<String, String, String> user = new AsyncCommand<>(
                new Command<>(CommandType.ECHO, new StatusOutput<>(StringCodec.UTF8)));
        user.whenComplete((value, error) -> assertThat(user.cancel(false)).isFalse());
        TransactionalCommand<String, String, String> transaction = new TransactionalCommand<>(user);
        AsyncCommand.markEnqueued(transaction, System.nanoTime() - 100_000);
        handler.write(context, transaction, promise);
        reply("+QUEUED\r\n");
        assertThat(user).isNotDone();
        MultiOutput<String, String> multi = new MultiOutput<>(StringCodec.UTF8);
        multi.add(transaction);
        handler.write(context, new AsyncCommand<>(new Command<>(CommandType.EXEC, multi)), promise);

        reply("*1\r\n+OK\r\n");

        assertThat(user).isCompletedWithValue("OK");
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void recorderFailureMustNotInterruptProtectedModeCleanup() throws Exception {
        AsyncCommand<String, String, String> denied = new AsyncCommand<>(
                new Command<>(CommandType.ECHO, new StatusOutput<>(StringCodec.UTF8)));
        AsyncCommand<String, String, String> pending = new AsyncCommand<>(
                new Command<>(CommandType.ECHO, new StatusOutput<>(StringCodec.UTF8)));
        AsyncCommand.markEnqueued(denied, System.nanoTime() - 100_000);
        AsyncCommand.markEnqueued(pending, System.nanoTime() - 100_000);
        handler.write(context, denied, promise);
        handler.write(context, pending, promise);
        doThrow(new IllegalStateException("Recorder failed")).when(recorder).recordCommandQueueLatency(any(), any(), any(),
                anyLong(), anyLong());
        try {
            reply("-DENIED Redis is running in protected mode\r\n");
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessage("Recorder failed");
            // Netty routes an exception thrown from channelRead back to this handler's exceptionCaught.
            handler.exceptionCaught(context, expected);
        }
        assertThat(denied).isCompletedExceptionally();
        assertThat(pending).isCompletedExceptionally();
        assertThat(handler.getStack()).isEmpty();
        verify(channel).disconnect();
    }

    private void reply(String response) throws Exception {
        handler.channelRead(context, Unpooled.wrappedBuffer(response.getBytes(StandardCharsets.US_ASCII)));
    }

}
