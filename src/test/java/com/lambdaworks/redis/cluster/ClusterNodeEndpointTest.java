package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Queue;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.AsyncCommand;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.resource.ClientResources;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterNodeEndpointTest {

    private AsyncCommand<String, String, String> command = new AsyncCommand<>(
            new Command<>(CommandType.APPEND, new StatusOutput<String, String>(new Utf8StringCodec()), null));

    private Queue<RedisCommand<String, String, ?>> queue;

    @Mock
    private ClientOptions clientOptions;

    @Mock
    private ClientResources clientResources;

    @Mock
    private RedisChannelWriter clusterChannelWriter;

    private ClusterNodeEndpoint sut;

    @Before
    public void before() throws Exception {

        sut = new ClusterNodeEndpoint(clientOptions, clientResources, clusterChannelWriter);
        queue = (Queue) sut.getQueue();
    }

    @Test
    public void closeWithoutCommands() throws Exception {

        sut.close();
        verifyZeroInteractions(clusterChannelWriter);
    }

    @Test
    public void closeWithQueuedCommands() throws Exception {

        when(clientOptions.isAutoReconnect()).thenReturn(true);
        queue.add(command);

        sut.close();

        verify(clusterChannelWriter).write(command);
    }

    @Test
    public void closeWithCancelledQueuedCommands() throws Exception {

        when(clientOptions.isAutoReconnect()).thenReturn(true);
        queue.add(command);
        command.cancel();

        sut.close();

        verifyZeroInteractions(clusterChannelWriter);
    }

    @Test
    public void closeWithQueuedCommandsFails() throws Exception {

        when(clientOptions.isAutoReconnect()).thenReturn(true);
        queue.add(command);
        when(clusterChannelWriter.write(any())).thenThrow(new RedisException("meh"));

        sut.close();

        assertThat(command.isDone()).isTrue();

        try {

            command.get();
            fail("Expected ExecutionException");
        } catch (ExecutionException e) {
            assertThat(e).hasCauseExactlyInstanceOf(RedisException.class);
        }
    }

    @Test
    public void closeWithBufferedCommands() throws Exception {

        when(clientOptions.isAutoReconnect()).thenReturn(true);
        when(clientOptions.getRequestQueueSize()).thenReturn(1000);
        when(clientOptions.getDisconnectedBehavior()).thenReturn(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS);
        sut.write(command);

        sut.close();

        verify(clusterChannelWriter).write(command);
    }

    @Test
    public void closeWithCancelledBufferedCommands() throws Exception {

        when(clientOptions.isAutoReconnect()).thenReturn(true);
        when(clientOptions.getRequestQueueSize()).thenReturn(1000);
        when(clientOptions.getDisconnectedBehavior()).thenReturn(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS);
        sut.write(command);
        command.cancel();

        sut.close();

        verifyZeroInteractions(clusterChannelWriter);
    }

    @Test
    public void closeWithBufferedCommandsFails() throws Exception {

        when(clientOptions.isAutoReconnect()).thenReturn(true);
        when(clientOptions.getRequestQueueSize()).thenReturn(1000);
        when(clientOptions.getDisconnectedBehavior()).thenReturn(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS);
        sut.write(command);
        when(clusterChannelWriter.write(any())).thenThrow(new RedisException(""));

        sut.close();

        try {

            command.get();
            fail("Expected ExecutionException");
        } catch (ExecutionException e) {
            assertThat(e).hasCauseExactlyInstanceOf(RedisException.class);
        }
    }
}
