/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Queue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.metrics.DefaultCommandLatencyCollector;
import com.lambdaworks.redis.metrics.DefaultCommandLatencyCollectorOptions;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.resource.ClientResources;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubCommandHandlerTest {

    private Queue<RedisCommand<String, String, ?>> stack;

    private PubSubCommandHandler<String, String> sut;

    private final Command<String, String, String> command = new Command<>(CommandType.APPEND, new StatusOutput<>(
            new Utf8StringCodec()), null);

    @Mock
    private ChannelHandlerContext context;

    @Mock
    private Channel channel;

    @Mock
    private ChannelPipeline pipeline;

    @Mock
    private EventLoop eventLoop;

    @Mock
    private ClientResources clientResources;

    @Mock
    private RedisChannelHandler channelHandler;

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws Exception {

        when(context.channel()).thenReturn(channel);
        when(channel.pipeline()).thenReturn(pipeline);
        when(channel.eventLoop()).thenReturn(eventLoop);
        when(eventLoop.submit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable r = (Runnable) invocation.getArguments()[0];
            r.run();
            return null;
        });

        when(clientResources.commandLatencyCollector()).thenReturn(
                new DefaultCommandLatencyCollector(DefaultCommandLatencyCollectorOptions.create()));

        when(channel.writeAndFlush(any())).thenAnswer(invocation -> {

            if (invocation.getArguments()[0] instanceof RedisCommand) {
                stack.add((RedisCommand) invocation.getArguments()[0]);
            }

            if (invocation.getArguments()[0] instanceof Collection) {
                stack.addAll((Collection) invocation.getArguments()[0]);
            }

            return new DefaultChannelPromise(channel);
        });

        sut = new PubSubCommandHandler<>(ClientOptions.create(), clientResources, StringCodec.UTF8);
        sut.setRedisChannelHandler(channelHandler);
        stack = (Queue) ReflectionTestUtils.getField(sut, "stack");
    }

    @Test
    public void testChannelActiveFailureShouldCancelCommands() throws Exception {

        ClientOptions clientOptions = ClientOptions.builder().cancelCommandsOnReconnectFailure(true).build();

        sut = new PubSubCommandHandler<>(clientOptions, clientResources, StringCodec.UTF8);
        sut.setRedisChannelHandler(channelHandler);

        sut.channelRegistered(context);
        sut.write(command);

        reset(context);
        when(context.channel()).thenThrow(new RuntimeException());
        try {
            sut.channelActive(context);
            fail("Missing RuntimeException");
        } catch (RuntimeException e) {
        }

        assertThat(command.isCancelled()).isTrue();
    }

    @Test
    public void shouldCompleteCommandExceptionallyOnOutputFailure() throws Exception {

        sut.channelRegistered(context);
        sut.channelActive(context);
        sut.write(command);

        sut.channelRead(context, Unpooled.wrappedBuffer(":1000\r\n".getBytes()));

        assertThat(ReflectionTestUtils.getField(command, "exception")).isInstanceOf(IllegalStateException.class);
    }
}
