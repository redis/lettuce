/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.metrics.DefaultCommandLatencyCollector;
import io.lettuce.core.metrics.DefaultCommandLatencyCollectorOptions;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;
import io.lettuce.test.ReflectionTestUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;

/**
 * @author Mark Paluch
 * @author Giridhar Kannan
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PubSubCommandHandlerUnitTests {

    private Queue<RedisCommand<String, String, ?>> stack;

    private PubSubCommandHandler<String, String> sut;

    private final Command<String, String, String> command = new Command<>(CommandType.APPEND,
            new StatusOutput<>(StringCodec.UTF8), null);

    @Mock
    private ChannelHandlerContext context;

    @Mock
    private Channel channel;

    @Mock
    private ChannelConfig channelConfig;

    @Mock
    private ChannelPipeline pipeline;

    @Mock
    private EventLoop eventLoop;

    @Mock
    private ClientResources clientResources;

    @Mock
    private PubSubEndpoint<String, String> endpoint;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void before() {

        when(channel.config()).thenReturn(channelConfig);
        when(context.alloc()).thenReturn(ByteBufAllocator.DEFAULT);
        when(context.channel()).thenReturn(channel);
        when(channel.pipeline()).thenReturn(pipeline);
        when(channel.eventLoop()).thenReturn(eventLoop);
        when(eventLoop.submit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable r = (Runnable) invocation.getArguments()[0];
            r.run();
            return null;
        });

        when(clientResources.commandLatencyCollector())
                .thenReturn(new DefaultCommandLatencyCollector(DefaultCommandLatencyCollectorOptions.create()));
        when(clientResources.tracing()).thenReturn(Tracing.disabled());

        sut = new PubSubCommandHandler<>(ClientOptions.create(), clientResources, StringCodec.UTF8, endpoint);
        stack = (Queue) ReflectionTestUtils.getField(sut, "stack");
    }

    @Test
    void shouldCompleteCommandExceptionallyOnOutputFailure() throws Exception {

        sut.channelRegistered(context);
        sut.channelActive(context);
        stack.add(command);

        sut.channelRead(context, responseBytes(":1000\r\n"));

        assertThat((Object) ReflectionTestUtils.getField(command, "exception")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldDecodeRegularCommand() throws Exception {

        sut.channelRegistered(context);
        sut.channelActive(context);
        stack.add(command);

        sut.channelRead(context, responseBytes("+OK\r\n"));

        assertThat(command.get()).isEqualTo("OK");
    }

    @Test
    void shouldDecodeTwoCommands() throws Exception {

        Command<String, String, String> command1 = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8),
                null);
        Command<String, String, String> command2 = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8),
                null);

        sut.channelRegistered(context);
        sut.channelActive(context);
        stack.add(command1);
        stack.add(command2);

        sut.channelRead(context, responseBytes("+OK\r\n+YEAH\r\n"));

        assertThat(command1.get()).isEqualTo("OK");
        assertThat(command2.get()).isEqualTo("YEAH");
    }

    @Test
    void shouldPropagatePubSubResponseToOutput() throws Exception {

        Command<String, String, String> command1 = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8),
                null);

        sut.channelRegistered(context);
        sut.channelActive(context);
        stack.add(command1);

        sut.channelRead(context, responseBytes("*3\r\n$7\r\nmessage\r\n$3\r\nfoo\r\n$3\r\nbar\r\n"));

        assertThat(command1.isDone()).isFalse();

        verify(endpoint).notifyMessage(any());
    }

    @Test
    void shouldPropagateInterleavedPubSubResponseToOutput() throws Exception {

        Command<String, String, String> command1 = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8),
                null);
        Command<String, String, String> command2 = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8),
                null);

        sut.channelRegistered(context);
        sut.channelActive(context);
        stack.add(command1);
        stack.add(command2);

        sut.channelRead(context,
                responseBytes("+OK\r\n*4\r\n$8\r\npmessage\r\n$1\r\n*\r\n$3\r\nfoo\r\n$3\r\nbar\r\n+YEAH\r\n"));

        assertThat(command1.get()).isEqualTo("OK");
        assertThat(command2.get()).isEqualTo("YEAH");

        ArgumentCaptor<PubSubOutput> captor = ArgumentCaptor.forClass(PubSubOutput.class);
        verify(endpoint).notifyMessage(captor.capture());

        assertThat(captor.getValue().pattern()).isEqualTo("*");
        assertThat(captor.getValue().channel()).isEqualTo("foo");
        assertThat(captor.getValue().get()).isEqualTo("bar");
    }

    @Test
    void shouldNotPropagatePartialPubSubResponseToOutput() throws Exception {

        Command<String, String, String> command1 = new Command<>(CommandType.SUBSCRIBE, new PubSubOutput<>(StringCodec.UTF8),
                null);
        Command<String, String, String> command2 = new Command<>(CommandType.SUBSCRIBE, new PubSubOutput<>(StringCodec.UTF8),
                null);

        sut.channelRegistered(context);
        sut.channelActive(context);
        stack.add(command1);
        stack.add(command2);

        sut.channelRead(context, responseBytes("*3\r\n$9\r\nsubscribe\r\n$1\r\na\r\n:2\r\n*3\r\n$9\r\nsubscribe\r\n"));

        assertThat(command1.isDone()).isTrue();
        assertThat(command2.isDone()).isFalse();

        assertThat(stack).hasSize(1);

        ArgumentCaptor<PubSubOutput> captor = ArgumentCaptor.forClass(PubSubOutput.class);
        verify(endpoint).notifyMessage(captor.capture());

        assertThat(captor.getValue().channel()).isEqualTo("a");
        assertThat(captor.getValue().count()).isEqualTo(2);
    }

    @Test
    void shouldCompleteWithChunkedResponseOnStack() throws Exception {

        Command<String, String, String> command1 = new Command<>(CommandType.SUBSCRIBE, new PubSubOutput<>(StringCodec.UTF8),
                null);
        Command<String, String, String> command2 = new Command<>(CommandType.SUBSCRIBE, new PubSubOutput<>(StringCodec.UTF8),
                null);

        sut.channelRegistered(context);
        sut.channelActive(context);
        stack.add(command1);
        stack.add(command2);

        sut.channelRead(context, responseBytes("*3\r\n$9\r\nsubscribe\r\n$1\r\na\r\n:2\r\n*3\r\n$9\r\nsubscribe\r\n"));
        sut.channelRead(context, responseBytes("$1\r\nb\r\n:2\r\n"));

        assertThat(command1.isDone()).isTrue();
        assertThat(command2.isDone()).isTrue();

        assertThat(stack).isEmpty();

        ArgumentCaptor<PubSubOutput> captor = ArgumentCaptor.forClass(PubSubOutput.class);
        verify(endpoint, times(2)).notifyMessage(captor.capture());

        assertThat(captor.getAllValues().get(0).channel()).isEqualTo("a");
        assertThat(captor.getAllValues().get(1).channel()).isEqualTo("b");
    }

    @Test
    void shouldCompleteWithChunkedResponseOutOfBand() throws Exception {

        sut.channelRegistered(context);
        sut.channelActive(context);

        sut.channelRead(context, responseBytes("*3\r\n$9\r\nsubscribe\r\n$1\r\na\r\n:2\r\n*3\r\n$9\r\nsubscribe\r\n"));
        sut.channelRead(context, responseBytes("$1\r\nb\r\n:2\r\n"));

        ArgumentCaptor<PubSubOutput> captor = ArgumentCaptor.forClass(PubSubOutput.class);
        verify(endpoint, times(2)).notifyMessage(captor.capture());

        assertThat(captor.getAllValues().get(0).channel()).isEqualTo("a");
        assertThat(captor.getAllValues().get(1).channel()).isEqualTo("b");
    }

    @Test
    void shouldCompleteUnsubscribe() throws Exception {

        Command<String, String, String> subCmd = new Command<>(CommandType.SUBSCRIBE, new PubSubOutput<>(StringCodec.UTF8),
                null);
        Command<String, String, String> unSubCmd = new Command<>(CommandType.UNSUBSCRIBE, new PubSubOutput<>(StringCodec.UTF8),
                null);

        doAnswer((Answer<PubSubEndpoint<String, String>>) inv -> {
            PubSubOutput<String, String> out = inv.getArgument(0);
            if (out.type() == PubSubOutput.Type.message) {
                throw new NullPointerException("Expected exception");
            }
            return endpoint;
        }).when(endpoint).notifyMessage(any());

        sut.channelRegistered(context);
        sut.channelActive(context);

        stack.add(subCmd);
        stack.add(unSubCmd);
        ByteBuf buf = responseBytes("*3\r\n$9\r\nsubscribe\r\n$10\r\ntest_sub_0\r\n:1\r\n"
                + "*3\r\n$7\r\nmessage\r\n$10\r\ntest_sub_0\r\n$3\r\nabc\r\n"
                + "*3\r\n$11\r\nunsubscribe\r\n$10\r\ntest_sub_0\r\n:0\r\n");
        sut.channelRead(context, buf);
        sut.channelRead(context, responseBytes("*3\r\n$7\r\nmessage\r\n$10\r\ntest_sub_1\r\n$3\r\nabc\r\n"));

        assertThat(unSubCmd.isDone()).isTrue();
    }

    @Test
    void shouldCompleteWithChunkedResponseInterleavedSending() throws Exception {

        Command<String, String, String> command1 = new Command<>(CommandType.SUBSCRIBE, new PubSubOutput<>(StringCodec.UTF8),
                null);

        sut.channelRegistered(context);
        sut.channelActive(context);

        sut.channelRegistered(context);
        sut.channelActive(context);

        sut.channelRead(context, responseBytes("*3\r\n$7\r\nmessage\r\n$3"));
        stack.add(command1);
        sut.channelRead(context, responseBytes("\r\nfoo\r\n$3\r\nbar\r\n"));
        sut.channelRead(context, responseBytes("*3\r\n$9\r\nsubscribe\r\n$1\r\na\r\n:2"));
        sut.channelRead(context, responseBytes("\r\n"));

        assertThat(command1.isDone()).isTrue();
        assertThat(stack).isEmpty();

        ArgumentCaptor<PubSubOutput> captor = ArgumentCaptor.forClass(PubSubOutput.class);
        verify(endpoint, times(2)).notifyMessage(captor.capture());

        assertThat(captor.getAllValues().get(0).channel()).isEqualTo("foo");
        assertThat(captor.getAllValues().get(0).get()).isEqualTo("bar");
        assertThat(captor.getAllValues().get(1).channel()).isEqualTo("a");
    }

    private static ByteBuf responseBytes(String s) {
        return Unpooled.wrappedBuffer(s.getBytes());
    }
}
