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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.metrics.CommandLatencyRecorder;
import io.lettuce.core.pubsub.PubSubCommandHandler;
import io.lettuce.core.pubsub.PubSubEndpoint;
import io.lettuce.core.pubsub.PubSubOutput;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/** Regression coverage for RESP3 Pub/Sub acknowledgements decoded as pushes, not normal command responses. */
@Tag(UNIT_TEST)
class PubSubQueueLatencyUnitTests {

    private final CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);

    private final ChannelHandlerContext context = mock(ChannelHandlerContext.class);

    private final Channel channel = mock(Channel.class);

    private final ChannelPromise promise = mock(ChannelPromise.class);

    private PubSubCommandHandler<String, String> handler;

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
        PubSubEndpoint<String, String> endpoint = mock(PubSubEndpoint.class);
        when(endpoint.getPushListeners()).thenReturn(Collections.emptyList());
        handler = new PubSubCommandHandler<>(ClientOptions.create(), resources, StringCodec.UTF8, endpoint);
        handler.channelRegistered(context);
    }

    @AfterEach
    void tearDown() throws Exception {
        handler.channelUnregistered(context);
    }

    @ParameterizedTest
    @EnumSource(value = CommandType.class, names = { "SUBSCRIBE", "UNSUBSCRIBE", "PSUBSCRIBE", "PUNSUBSCRIBE", "SSUBSCRIBE",
            "SUNSUBSCRIBE" })
    void shouldRecordResp3PushAcknowledgementExactlyOnce(CommandType type) throws Exception {
        Command<String, String, String> command = command(type);
        AsyncCommand.markEnqueued(command, System.nanoTime() - 100_000);
        handler.write(context, command, promise);
        reply(type);

        assertThat(command.isDone()).isTrue();
        assertThat(handler.getStack()).isEmpty();
        verify(recorder, times(1)).recordCommandQueueLatency(any(), any(), any(RedisCommand.class), anyLong(), anyLong());
    }

    @Test
    void shouldNotRecordCancelledResp3PushAcknowledgement() throws Exception {
        AsyncCommand<String, String, String> command = new AsyncCommand<>(command(CommandType.SUBSCRIBE));
        AsyncCommand.markEnqueued(command, System.nanoTime() - 100_000);
        handler.write(context, command, promise);
        command.cancel();
        reply(CommandType.SUBSCRIBE);

        assertThat(command.isCancelled()).isTrue();
        verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
    }

    private Command<String, String, String> command(CommandType type) {
        return new Command<>(type, new PubSubOutput<>(StringCodec.UTF8));
    }

    private void reply(CommandType type) throws Exception {
        String response = ">3\r\n+" + type.name().toLowerCase(Locale.ROOT) + "\r\n$3\r\nfoo\r\n:1\r\n";
        handler.channelRead(context, Unpooled.wrappedBuffer(response.getBytes(StandardCharsets.US_ASCII)));
    }

}
