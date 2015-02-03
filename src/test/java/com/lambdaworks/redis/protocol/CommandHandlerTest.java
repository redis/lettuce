package com.lambdaworks.redis.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.StatusOutput;
import io.netty.channel.ChannelHandlerContext;

@RunWith(MockitoJUnitRunner.class)
public class CommandHandlerTest {

    private BlockingQueue<RedisCommand<String, String, ?>> q = new ArrayBlockingQueue<RedisCommand<String, String, ?>>(10);

    private CommandHandler<String, String> sut = new CommandHandler<String, String>(q);

    @Mock
    private ChannelHandlerContext context;

    @Test
    public void testException() throws Exception {
        sut.exceptionCaught(context, new Exception());
        verify(context).fireExceptionCaught(any(Exception.class));
    }

    @Test
    public void testExceptionWithQueue() throws Exception {
        q.clear();

        Command<String, String, String> command = new Command<String, String, String>(CommandType.APPEND,
                new StatusOutput<String, String>(new Utf8StringCodec()), null);
        q.add(command);
        sut.exceptionCaught(context, new Exception());

        assertThat(q).isEmpty();
        assertThat(command.getException()).isNotNull();
        verify(context).fireExceptionCaught(any(Exception.class));
    }

}
