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
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.metrics.CommandLatencyRecorder;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.CommandType;
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

    private void reply(String response) throws Exception {
        handler.channelRead(context, Unpooled.wrappedBuffer(response.getBytes(StandardCharsets.US_ASCII)));
    }

}
