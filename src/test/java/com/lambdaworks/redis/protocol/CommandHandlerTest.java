package com.lambdaworks.redis.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.ConnectionEvents;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.resource.ClientResources;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

@RunWith(MockitoJUnitRunner.class)
public class CommandHandlerTest {

    private Queue<RedisCommand<String, String, ?>> q = new ArrayDeque<>(10);

    private CommandHandler<String, String> sut;

    private Command<String, String, String> command = new Command<>(CommandType.APPEND,
            new StatusOutput<String, String>(new Utf8StringCodec()), null);

    @Mock
    private ChannelHandlerContext context;

    @Mock
    private Channel channel;

    @Mock
    private ByteBufAllocator byteBufAllocator;

    @Mock
    private ChannelPipeline pipeline;

    @Mock
    private EventLoop eventLoop;

    @Mock
    private ClientResources clientResources;

    @Mock
    private RedisChannelHandler channelHandler;

    @Before
    public void before() throws Exception {
        when(context.channel()).thenReturn(channel);
        when(context.alloc()).thenReturn(byteBufAllocator);
        when(channel.pipeline()).thenReturn(pipeline);
        when(channel.eventLoop()).thenReturn(eventLoop);
        when(eventLoop.submit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable r = (Runnable) invocation.getArguments()[0];
            r.run();
            return null;
        });

        when(channel.write(any())).thenAnswer(invocation -> {

            if (invocation.getArguments()[0] instanceof RedisCommand) {
                q.add((RedisCommand) invocation.getArguments()[0]);
            }

            if (invocation.getArguments()[0] instanceof Collection) {
                q.addAll((Collection) invocation.getArguments()[0]);
            }

            return new DefaultChannelPromise(channel);
        });

        when(channel.writeAndFlush(any())).thenAnswer(invocation -> {
            if (invocation.getArguments()[0] instanceof RedisCommand) {
                q.add((RedisCommand) invocation.getArguments()[0]);
            }

            if (invocation.getArguments()[0] instanceof Collection) {
                q.addAll((Collection) invocation.getArguments()[0]);
            }
            return new DefaultChannelPromise(channel);
        });

        sut = new CommandHandler<String, String>(ClientOptions.create(), clientResources, q);
        sut.setRedisChannelHandler(channelHandler);
    }

    @Test
    public void testChannelActive() throws Exception {
        sut.channelRegistered(context);

        sut.channelActive(context);

        verify(pipeline).fireUserEventTriggered(any(ConnectionEvents.Activated.class));

    }

    @Test
    public void testChannelActiveWithBufferedAndQueuedCommands() throws Exception {

        Command<String, String, String> bufferedCommand = new Command<>(CommandType.GET,
                new StatusOutput<String, String>(new Utf8StringCodec()), null);

        Command<String, String, String> pingCommand = new Command<>(CommandType.PING,
                new StatusOutput<String, String>(new Utf8StringCodec()), null);
        q.add(bufferedCommand);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                sut.write(pingCommand);
                return null;
            }
        }).when(channelHandler).activated();
        when(channel.isActive()).thenReturn(true);

        sut.channelRegistered(context);
        sut.channelActive(context);

        assertThat(q).containsSequence(pingCommand, bufferedCommand);

        verify(pipeline).fireUserEventTriggered(any(ConnectionEvents.Activated.class));
    }

    @Test
    public void testChannelActiveWithBufferedAndQueuedCommandsRetainsOrder() throws Exception {

        Command<String, String, String> bufferedCommand1 = new Command<>(CommandType.SET,
                new StatusOutput<String, String>(new Utf8StringCodec()), null);

        Command<String, String, String> bufferedCommand2 = new Command<>(CommandType.GET,
                new StatusOutput<String, String>(new Utf8StringCodec()), null);

        Command<String, String, String> queuedCommand1 = new Command<>(CommandType.PING,
                new StatusOutput<String, String>(new Utf8StringCodec()), null);

        Command<String, String, String> queuedCommand2 = new Command<>(CommandType.AUTH,
                new StatusOutput<String, String>(new Utf8StringCodec()), null);

        q.add(queuedCommand1);
        q.add(queuedCommand2);

        Collection buffer = (Collection) ReflectionTestUtils.getField(sut, "commandBuffer");
        buffer.add(bufferedCommand1);
        buffer.add(bufferedCommand2);

        reset(channel);
        when(channel.writeAndFlush(any())).thenAnswer(invocation -> new DefaultChannelPromise(channel));
        when(channel.eventLoop()).thenReturn(eventLoop);
        when(channel.pipeline()).thenReturn(pipeline);

        sut.channelRegistered(context);
        sut.channelActive(context);

        assertThat(q).isEmpty();

        buffer = (Collection) ReflectionTestUtils.getField(sut, "commandBuffer");
        assertThat(buffer).isEmpty();

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(channel).writeAndFlush(objectArgumentCaptor.capture());

        assertThat((Collection) objectArgumentCaptor.getValue()).containsSequence(queuedCommand1, queuedCommand2,
                bufferedCommand1, bufferedCommand2);
    }

    @Test
    public void testChannelActiveReplayBufferedCommands() throws Exception {

        Command<String, String, String> bufferedCommand1 = new Command<>(CommandType.SET,
                new StatusOutput<String, String>(new Utf8StringCodec()), null);

        Command<String, String, String> bufferedCommand2 = new Command<>(CommandType.GET,
                new StatusOutput<String, String>(new Utf8StringCodec()), null);

        Command<String, String, String> queuedCommand1 = new Command<>(CommandType.PING,
                new StatusOutput<String, String>(new Utf8StringCodec()), null);

        Command<String, String, String> queuedCommand2 = new Command<>(CommandType.AUTH,
                new StatusOutput<String, String>(new Utf8StringCodec()), null);

        q.add(queuedCommand1);
        q.add(queuedCommand2);

        Collection buffer = (Collection) ReflectionTestUtils.getField(sut, "commandBuffer");
        buffer.add(bufferedCommand1);
        buffer.add(bufferedCommand2);

        sut.channelRegistered(context);
        sut.channelActive(context);

        assertThat(q).containsSequence(queuedCommand1, queuedCommand2, bufferedCommand1, bufferedCommand2);

        buffer = (Collection) ReflectionTestUtils.getField(sut, "commandBuffer");
        assertThat(buffer).isEmpty();
    }

    @Test
    public void testExceptionChannelActive() throws Exception {
        sut.setState(CommandHandler.LifecycleState.ACTIVE);

        when(channel.isActive()).thenReturn(true);

        sut.channelActive(context);
        sut.exceptionCaught(context, new Exception());
    }

    @Test
    public void testIOExceptionChannelActive() throws Exception {
        sut.setState(CommandHandler.LifecycleState.ACTIVE);

        when(channel.isActive()).thenReturn(true);

        sut.channelActive(context);
        sut.exceptionCaught(context, new IOException("Connection timed out"));
    }

    @Test
    public void testExceptionChannelInactive() throws Exception {
        sut.setState(CommandHandler.LifecycleState.DISCONNECTED);
        sut.exceptionCaught(context, new Exception());
        verify(context, never()).fireExceptionCaught(any(Exception.class));
    }

    @Test
    public void testExceptionWithQueue() throws Exception {
        sut.setState(CommandHandler.LifecycleState.ACTIVE);
        q.clear();

        sut.channelActive(context);
        when(channel.isActive()).thenReturn(true);

        q.add(command);
        sut.exceptionCaught(context, new Exception());

        assertThat(q).isEmpty();
        command.get();

        assertThat(ReflectionTestUtils.getField(command, "exception")).isNotNull();
    }

    @Test(expected = RedisException.class)
    public void testWriteWhenClosed() throws Exception {

        sut.setState(CommandHandler.LifecycleState.CLOSED);

        sut.write(command);
    }

    @Test
    public void testExceptionWhenClosed() throws Exception {

        sut.setState(CommandHandler.LifecycleState.CLOSED);

        sut.exceptionCaught(context, new Exception());
        verifyZeroInteractions(context);
    }

}
