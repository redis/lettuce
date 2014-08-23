package com.lambdaworks.redis.protocol;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

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

    private BlockingQueue<RedisCommand> q = new ArrayBlockingQueue<RedisCommand>(10);

    private CommandHandler sut = new CommandHandler(q);

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

        Command command = new Command(CommandType.APPEND, new StatusOutput(new Utf8StringCodec()), null);
        q.add(command);
        sut.exceptionCaught(context, new Exception());

        assertThat(q).isEmpty();
        assertThat(command.getException()).isNotNull();
        verify(context).fireExceptionCaught(any(Exception.class));
    }

}
