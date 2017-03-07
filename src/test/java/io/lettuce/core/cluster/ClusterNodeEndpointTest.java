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
package io.lettuce.core.cluster;

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
import org.mockito.junit.MockitoJUnitRunner;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisException;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterNodeEndpointTest {

    private AsyncCommand<String, String, String> command = new AsyncCommand<>(new Command<>(CommandType.APPEND,
            new StatusOutput<String, String>(new Utf8StringCodec()), null));

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
        when(clusterChannelWriter.write(any(RedisCommand.class))).thenThrow(new RedisException("meh"));

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
        when(clusterChannelWriter.write(any(RedisCommand.class))).thenThrow(new RedisException(""));

        sut.close();

        try {

            command.get();
            fail("Expected ExecutionException");
        } catch (ExecutionException e) {
            assertThat(e).hasCauseExactlyInstanceOf(RedisException.class);
        }
    }
}
