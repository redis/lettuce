package com.lambdaworks.redis.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.lambdaworks.redis.ClientOptions;
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

    private BlockingQueue<RedisCommand<String, String, ?>> q = new ArrayBlockingQueue<RedisCommand<String, String, ?>>(10);

    private CommandHandler<String, String> sut = new CommandHandler<String, String>(new ClientOptions.Builder().build(), q);

    @Mock
    private ChannelHandlerContext context;

    @Mock
    private Channel channel;

    @Test
    public void testExceptionChannelActive() throws Exception {

        when(context.channel()).thenReturn(channel);

        sut.channelActive(context);
        sut.exceptionCaught(context, new Exception());
        verify(context).fireExceptionCaught(any(Exception.class));
    }

    @Test
    public void testExceptionChannelInactive() throws Exception {
        sut.exceptionCaught(context, new Exception());
        verify(context, never()).fireExceptionCaught(any(Exception.class));
    }

    @Test
    public void testExceptionWithQueue() throws Exception {
        q.clear();
        when(context.channel()).thenReturn(channel);

        sut.channelActive(context);

        Command<String, String, String> command = new Command<String, String, String>(CommandType.APPEND,
                new StatusOutput<String, String>(new Utf8StringCodec()), null);
        q.add(command);
        sut.exceptionCaught(context, new Exception());

        assertThat(q).isEmpty();
        assertThat(command.getException()).isNotNull();

        verify(context).fireExceptionCaught(any(Exception.class));
    }

}
