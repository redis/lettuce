/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisReactiveCommandsImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.metrics.CommandLatencyRecorder;
import io.lettuce.core.protocol.CommandHandler;
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
import reactor.core.Disposable;

@Tag(UNIT_TEST)
class ClusterReactiveCommandQueueLatencyUnitTests {

    private final CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);

    private final ChannelHandlerContext context = mock(ChannelHandlerContext.class);

    private final Channel channel = mock(Channel.class);

    private final ChannelPromise promise = mock(ChannelPromise.class);

    private final RedisChannelWriter retry = mock(RedisChannelWriter.class);

    private CommandHandler handler;

    private DefaultEndpoint endpoint;

    private RedisReactiveCommandsImpl<String, String> reactive;

    private ClientResources resources;

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
        handler = new CommandHandler(ClientOptions.create(), resources, mock(Endpoint.class));
        handler.channelRegistered(context);
        endpoint = new DefaultEndpoint(ClientOptions.create(), resources);
        StatefulRedisConnection<String, String> connection = mock(StatefulRedisConnection.class);
        when(connection.getOptions()).thenReturn(ClientOptions.create());
        when(connection.getResources()).thenReturn(resources);
        doAnswer(invocation -> {
            RedisCommand<String, String, String> original = invocation.getArgument(0);
            ClusterCommand<String, String, String> cluster = new ClusterCommand<>(original, retry, 5);
            endpoint.write(cluster);
            handler.write(context, cluster, promise);
            return original;
        }).when(connection).dispatch(any(RedisCommand.class));
        reactive = new RedisReactiveCommandsImpl<>(connection, StringCodec.UTF8);
    }

    @AfterEach
    void tearDown() throws Exception {
        handler.channelUnregistered(context);
        endpoint.close();
    }

    @ParameterizedTest
    @ValueSource(strings = { "MOVED", "ASK" })
    void cancelledBeforeFinalDecodeMustNotBeSampledByRedirectSource(String error) throws Exception {
        Channel targetChannel = mock(Channel.class);
        ChannelHandlerContext targetContext = mock(ChannelHandlerContext.class);
        when(targetContext.channel()).thenReturn(targetChannel);
        when(targetContext.alloc()).thenReturn(ByteBufAllocator.DEFAULT);
        when(targetChannel.config()).thenReturn(mock(ChannelConfig.class));
        when(targetChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 6380));
        when(targetChannel.localAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 2345));
        CommandHandler targetHandler = new CommandHandler(ClientOptions.create(), resources, mock(Endpoint.class));
        targetHandler.channelRegistered(targetContext);
        try {
            List<String> values = new ArrayList<>();
            Disposable subscription = reactive.echo("OK").subscribe(values::add);
            doAnswer(invocation -> {
                RedisCommand<?, ?, ?> redirected = invocation.getArgument(0);
                AtomicReference<Throwable> failure = new AtomicReference<>();
                Thread worker = new Thread(() -> {
                    try {
                        redirected.getOutput().setError((String) null);
                        targetHandler.write(targetContext, redirected, promise);
                        subscription.dispose();
                        assertThat(redirected.isCancelled()).isTrue();
                        targetHandler.channelRead(targetContext,
                                Unpooled.wrappedBuffer("$2\r\nOK\r\n".getBytes(StandardCharsets.US_ASCII)));
                    } catch (Throwable t) {
                        failure.set(t);
                    }
                });
                worker.start();
                worker.join(10_000);
                assertThat(worker.isAlive()).isFalse();
                assertThat(failure.get()).isNull();
                return redirected;
            }).when(retry).write(any(RedisCommand.class));
            reply("-" + error + " 1 127.0.0.1:6380\r\n");
            assertThat(values).isEmpty();
            verify(recorder, never()).recordCommandQueueLatency(any(), any(), any(), anyLong(), anyLong());
        } finally {
            targetHandler.channelUnregistered(targetContext);
        }
    }

    private void reply(String response) throws Exception {
        handler.channelRead(context, Unpooled.wrappedBuffer(response.getBytes(StandardCharsets.US_ASCII)));
    }

}
