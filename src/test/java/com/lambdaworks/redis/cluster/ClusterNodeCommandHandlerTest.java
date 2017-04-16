/*
 * Copyright 2011-2017 the original author or authors.
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
package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.metrics.DefaultCommandLatencyCollector;
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
public class ClusterNodeCommandHandlerTest {

    private AsyncCommand<String, String, String> command = new AsyncCommand<>(new Command<>(CommandType.APPEND,
            new StatusOutput<>(new Utf8StringCodec()), null));

    private Queue<RedisCommand<String, String, ?>> queue = new LinkedBlockingQueue<>();

    @Mock
    private ClientOptions clientOptions;

    @Mock
    private ClientResources clientResources;

    @Mock
    private RedisChannelWriter<String, String> clusterChannelWriter;

    private ClusterNodeCommandHandler sut;

    @Before
    public void before() throws Exception {

        when(clientResources.commandLatencyCollector()).thenReturn(DefaultCommandLatencyCollector.disabled());
        sut = new ClusterNodeCommandHandler(clientOptions, clientResources, queue, clusterChannelWriter);
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
