package com.lambdaworks.redis.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.StatusOutput;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

@RunWith(MockitoJUnitRunner.class)
public class CommandHandlerTest {

    private Queue<RedisCommand<String, String, ?>> q = new ArrayDeque<RedisCommand<String, String, ?>>(10);

    private CommandHandler<String, String> sut = new CommandHandler<String, String>(new ClientOptions.Builder().build(), q);

    private Command<String, String, String> command = new Command<String, String, String>(CommandType.APPEND,
            new StatusOutput<String, String>(new Utf8StringCodec()), null);

    @Mock
    private ChannelHandlerContext context;

    @Mock
    private Channel channel;

    @Test
    public void testExceptionChannelActive() throws Exception {

        sut.setState(CommandHandler.LifecycleState.ACTIVE);

        when(context.channel()).thenReturn(channel);
        when(channel.isActive()).thenReturn(true);

        sut.channelActive(context);
        sut.exceptionCaught(context, new Exception());
        verify(context).fireExceptionCaught(any(Exception.class));
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
        when(context.channel()).thenReturn(channel);

        sut.channelActive(context);
        when(channel.isActive()).thenReturn(true);

        q.add(command);
        sut.exceptionCaught(context, new Exception());

        assertThat(q).isEmpty();
        assertThat(command.getException()).isNotNull();

        verify(context).fireExceptionCaught(any(Exception.class));
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
