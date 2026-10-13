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
package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisException;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.metrics.CommandLatencyRecorder;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ConnectionIntent;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.Endpoint;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * Protocol unit tests for actual cluster command redirections, without a Redis cluster.
 */
@Tag(UNIT_TEST)
class ClusterCommandQueueLatencyUnitTests {

    private final CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);

    private final ChannelHandlerContext context = mock(ChannelHandlerContext.class);

    private final Channel channel = mock(Channel.class);

    private final ChannelPromise promise = mock(ChannelPromise.class);

    private final RedisChannelWriter retry = mock(RedisChannelWriter.class);

    private CommandHandler handler;

    private DefaultEndpoint endpoint;

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
        endpoint = new DefaultEndpoint(ClientOptions.create(), resources);
        when(retry.write(any(RedisCommand.class))).thenAnswer(invocation -> {
            RedisCommand<?, ?, ?> command = invocation.getArgument(0);
            command.getOutput().setError((String) null);
            handler.write(context, command, promise);
            return command;
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        handler.channelUnregistered(context);
        endpoint.close();
    }

    @ParameterizedTest
    @ValueSource(strings = { "MOVED", "ASK" })
    void shouldRecordOneEndToEndSampleAfterActualRedirection(String error) throws Exception {
        AsyncCommand<String, String, String> userCommand = new AsyncCommand<>(
                new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8)));
        ClusterCommand<String, String, String> command = new ClusterCommand<>(userCommand, retry, 5);
        // The actual endpoint supplies submission instrumentation before protocol dispatch.
        endpoint.write(command);
        handler.write(context, command, promise);

        reply("-" + error + " 1 127.0.0.1:6380\r\n");

        assertThat(command.isDone()).isFalse();
        assertThat(userCommand).isNotDone();
        verify(retry).write(command);
        verify(recorder).recordCommandLatency(any(), any(), any(RedisCommand.class), anyLong(), anyLong());
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());

        reply("+OK\r\n");

        assertThat(command.isDone()).isTrue();
        assertThat(userCommand).isCompletedWithValue("OK");
        verify(recorder, times(2)).recordCommandLatency(any(), any(), any(RedisCommand.class), anyLong(), anyLong());
        ArgumentCaptor<Long> queueLatency = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> endToEndLatency = ArgumentCaptor.forClass(Long.class);
        verify(recorder).recordCommandQueueLatency(any(), any(), any(RedisCommand.class), queueLatency.capture(),
                endToEndLatency.capture());
        assertThat(queueLatency.getValue()).isGreaterThanOrEqualTo(0);
        assertThat(endToEndLatency.getValue()).isGreaterThanOrEqualTo(queueLatency.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = { "MOVED", "ASK" })
    void shouldRecordExactlyOneSampleWhenRedirectCompletesOnAnotherEventLoop(String error) throws Exception {
        ClientResources resources = mock(ClientResources.class);
        when(resources.commandLatencyRecorder()).thenReturn(recorder);
        when(resources.tracing()).thenReturn(Tracing.disabled());

        Channel otherChannel = mock(Channel.class);
        ChannelHandlerContext otherContext = mock(ChannelHandlerContext.class);
        ChannelPromise otherPromise = mock(ChannelPromise.class);
        when(otherContext.channel()).thenReturn(otherChannel);
        when(otherContext.alloc()).thenReturn(ByteBufAllocator.DEFAULT);
        when(otherChannel.config()).thenReturn(mock(ChannelConfig.class));
        when(otherChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 6380));
        when(otherChannel.localAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 2345));
        when(otherPromise.isVoid()).thenReturn(true);

        CommandHandler otherHandler = new CommandHandler(ClientOptions.create(), resources, mock(Endpoint.class));
        otherHandler.channelRegistered(otherContext);
        try {
            AsyncCommand<String, String, String> future = new AsyncCommand<>(
                    new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8)));
            ClusterCommand<String, String, String> command = new ClusterCommand<>(future, retry, 5);
            endpoint.write(command);
            handler.write(context, command, promise);

            AtomicReference<Throwable> workerFailure = new AtomicReference<>();
            doAnswer(invocation -> {
                RedisCommand<?, ?, ?> redirected = invocation.getArgument(0);
                Thread worker = new Thread(() -> {
                    try {
                        redirected.getOutput().setError((String) null);
                        otherHandler.write(otherContext, redirected, otherPromise);
                        otherHandler.channelRead(otherContext,
                                Unpooled.wrappedBuffer("+OK\r\n".getBytes(StandardCharsets.US_ASCII)));
                    } catch (Throwable t) {
                        workerFailure.set(t);
                    }
                });
                worker.start();
                worker.join(10_000);
                assertThat(worker.isAlive()).isFalse();
                assertThat(workerFailure.get()).isNull();
                return redirected;
            }).when(retry).write(any(RedisCommand.class));

            reply("-" + error + " 1 127.0.0.1:6380\r\n");

            assertThat(future).isCompletedWithValue("OK");
            verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(RedisCommand.class), anyLong(), anyLong());
        } finally {
            otherHandler.channelUnregistered(otherContext);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "MOVED", "ASK" })
    void shouldNotCountRedirectWhenTargetWriteFailsWithoutReply(String error) throws Exception {
        doAnswer(invocation -> {
            throw new RedisException("Target connection is not available");
        }).when(retry).write(any(RedisCommand.class));

        AsyncCommand<String, String, String> future = new AsyncCommand<>(
                new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8)));
        ClusterCommand<String, String, String> command = new ClusterCommand<>(future, retry, 5);
        endpoint.write(command);
        handler.write(context, command, promise);

        reply("-" + error + " 1 127.0.0.1:6380\r\n");

        assertThat(command.isDone()).isTrue();
        assertThat(future.isCompletedExceptionally()).isTrue();
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @ParameterizedTest
    @ValueSource(strings = { "MOVED", "ASK" })
    void shouldCountFinalDecodedRedirectErrorWhenRetriesExhausted(String error) throws Exception {
        AsyncCommand<String, String, String> future = new AsyncCommand<>(
                new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8)));
        ClusterCommand<String, String, String> command = new ClusterCommand<>(future, retry, 0);
        endpoint.write(command);
        handler.write(context, command, promise);

        reply("-" + error + " 1 127.0.0.1:6380\r\n");

        assertThat(command.isDone()).isTrue();
        verify(retry, never()).write(any(RedisCommand.class));
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(RedisCommand.class), anyLong(), anyLong());
    }

    @ParameterizedTest
    @CsvSource({ "MOVED,sync", "ASK,sync", "MOVED,async", "ASK,async", "MOVED,reject", "ASK,reject", "MOVED,write",
            "ASK,write" })
    void distributionWriterFailureWithoutFinalReplyMustNotRecord(String error, String failure) throws Exception {
        ClientResources resources = mock(ClientResources.class);
        when(resources.eventBus()).thenReturn(mock(EventBus.class));
        DefaultEndpoint defaultWriter = mock(DefaultEndpoint.class);
        when(defaultWriter.getClientResources()).thenReturn(resources);
        ClientOptions options = ClientOptions.builder()
                .disconnectedBehavior("reject".equals(failure) ? ClientOptions.DisconnectedBehavior.REJECT_COMMANDS
                        : ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS)
                .build();
        ClusterDistributionChannelWriter writer = new ClusterDistributionChannelWriter(defaultWriter, options,
                mock(ClusterEventListener.class));
        PooledClusterConnectionProvider provider = mock(PooledClusterConnectionProvider.class);
        writer.setClusterConnectionProvider(provider);
        writer.setPartitions(new Partitions());
        CompletableFuture<StatefulRedisConnection<String, String>> connectionFuture = new CompletableFuture<>();
        when(provider.getConnectionAsync(any(ConnectionIntent.class), anyString(), anyInt())).thenReturn(connectionFuture);
        RedisException cause = new RedisException("Target connection failed");
        RedisChannelWriter target = mock(RedisChannelWriter.class);
        if ("sync".equals(failure)) {
            connectionFuture.completeExceptionally(cause);
        } else if ("write".equals(failure)) {
            StatefulRedisConnectionImpl<String, String> connection = mock(StatefulRedisConnectionImpl.class);
            when(connection.getChannelWriter()).thenReturn(target);
            doAnswer(invocation -> {
                throw cause;
            }).when(target).write(any(RedisCommand.class));
            doAnswer(invocation -> {
                throw cause;
            }).when(target).write(anyCollection());
            connectionFuture.complete(connection);
        }
        AsyncCommand<String, String, String> future = new AsyncCommand<>(new Command<>(CommandType.GET,
                new StatusOutput<>(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8).addKey("key")));
        ClusterCommand<String, String, String> command = new ClusterCommand<>(future, writer, 5);
        endpoint.write(command);
        handler.write(context, command, promise);

        reply("-" + error + " 1 127.0.0.1:6380\r\n");

        verify(provider).getConnectionAsync(ConnectionIntent.WRITE, "127.0.0.1", 6380);
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
        if ("async".equals(failure)) {
            assertThat(future).isNotDone();
            connectionFuture.completeExceptionally(cause);
        }
        if ("write".equals(failure)) {
            if ("ASK".equals(error)) {
                verify(target).write(anyCollection());
            } else {
                verify(target).write(any(RedisCommand.class));
            }
        }
        assertThat(future).isCompletedExceptionally();
        assertThat(command.isTerminalResponseDecoded()).isFalse();
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    @ParameterizedTest
    @ValueSource(strings = { "MOVED", "ASK" })
    void sourceClaimMustUseFinalDecodeWhenTargetHasNotRecordedYet(String error) throws Exception {
        ClientResources resources = mock(ClientResources.class);
        when(resources.commandLatencyRecorder()).thenReturn(recorder);
        when(resources.tracing()).thenReturn(Tracing.disabled());
        Channel otherChannel = mock(Channel.class);
        ChannelHandlerContext otherContext = mock(ChannelHandlerContext.class);
        when(otherContext.channel()).thenReturn(otherChannel);
        when(otherContext.alloc()).thenReturn(ByteBufAllocator.DEFAULT);
        when(otherChannel.config()).thenReturn(mock(ChannelConfig.class));
        when(otherChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 6380));
        when(otherChannel.localAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 2345));
        CountDownLatch targetCompleted = new CountDownLatch(1);
        CountDownLatch releaseTarget = new CountDownLatch(1);
        CommandHandler otherHandler = new CommandHandler(ClientOptions.create(), resources, mock(Endpoint.class)) {

            @Override
            protected void complete(RedisCommand<?, ?, ?> command) {
                super.complete(command);
                targetCompleted.countDown();
                try {
                    assertThat(releaseTarget.await(10, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }

        };
        otherHandler.channelRegistered(otherContext);
        AtomicReference<Thread> worker = new AtomicReference<>();
        AtomicReference<Throwable> workerFailure = new AtomicReference<>();
        try {
            AsyncCommand<String, String, String> future = new AsyncCommand<>(
                    new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8)));
            ClusterCommand<String, String, String> command = new ClusterCommand<>(future, retry, 5);
            endpoint.write(command);
            handler.write(context, command, promise);
            doAnswer(invocation -> {
                RedisCommand<?, ?, ?> redirected = invocation.getArgument(0);
                Thread thread = new Thread(() -> {
                    try {
                        redirected.getOutput().setError((String) null);
                        otherHandler.write(otherContext, redirected, promise);
                        otherHandler.channelRead(otherContext,
                                Unpooled.wrappedBuffer("+OK\r\n".getBytes(StandardCharsets.US_ASCII)));
                    } catch (Throwable t) {
                        workerFailure.set(t);
                    }
                });
                worker.set(thread);
                thread.start();
                assertThat(targetCompleted.await(10, TimeUnit.SECONDS)).isTrue();
                return redirected;
            }).when(retry).write(any(RedisCommand.class));

            reply("-" + error + " 1 127.0.0.1:6380\r\n");

            assertThat(future).isCompletedWithValue("OK");
            ArgumentCaptor<Long> completion = ArgumentCaptor.forClass(Long.class);
            verify(recorder, times(2)).recordCommandLatency(any(), any(), any(RedisCommand.class), anyLong(),
                    completion.capture());
            ArgumentCaptor<Long> queue = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> endToEnd = ArgumentCaptor.forClass(Long.class);
            verify(recorder).recordCommandQueueLatency(any(), any(), any(), queue.capture(), endToEnd.capture());
            // Queue + the source attempt's completion is the earlier MOVED/ASK decode, never the final response.
            assertThat(endToEnd.getValue()).isGreaterThan(queue.getValue() + completion.getAllValues().get(0));
        } finally {
            releaseTarget.countDown();
            if (worker.get() != null) {
                worker.get().join(10_000);
                assertThat(worker.get().isAlive()).isFalse();
            }
            otherHandler.channelUnregistered(otherContext);
        }
        assertThat(workerFailure.get()).isNull();
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    private void reply(String response) throws Exception {
        handler.channelRead(context, Unpooled.wrappedBuffer(response.getBytes(StandardCharsets.US_ASCII)));
    }

}
