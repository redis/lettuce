/*
 * Copyright 2011-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisException;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.ReflectionTestUtils;
import io.lettuce.test.TestFutures;

/**
 * Unit tests for {@link ClusterNodeEndpoint}.
 *
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class ClusterNodeEndpointUnitTests {

    private AsyncCommand<String, String, String> command = new AsyncCommand<>(
            new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8), null));

    private Queue<RedisCommand<String, String, ?>> disconnectedBuffer;

    @Mock
    private ClientOptions clientOptions;

    @Mock
    private ClientResources clientResources;

    @Mock
    private RedisChannelWriter clusterChannelWriter;

    private ClusterNodeEndpoint sut;

    @BeforeEach
    void before() {

        when(clientOptions.getRequestQueueSize()).thenReturn(1000);
        when(clientOptions.getDisconnectedBehavior()).thenReturn(ClientOptions.DisconnectedBehavior.DEFAULT);

        prepareNewEndpoint();
    }

    @Test
    void closeWithoutCommands() {

        sut.closeAsync();
        verifyNoInteractions(clusterChannelWriter);
    }

    @Test
    void closeWithQueuedCommands() {

        disconnectedBuffer.add(command);

        sut.closeAsync();

        verify(clusterChannelWriter).write(command);
    }

    @Test
    void closeWithCancelledQueuedCommands() {

        disconnectedBuffer.add(command);
        command.cancel();

        sut.closeAsync();

        verifyNoInteractions(clusterChannelWriter);
    }

    @Test
    void closeWithQueuedCommandsFails() {

        disconnectedBuffer.add(command);
        when(clusterChannelWriter.write(any(RedisCommand.class))).thenThrow(new RedisException("meh"));

        sut.closeAsync();

        assertThat(command.isDone()).isTrue();

        assertThatThrownBy(() -> TestFutures.awaitOrTimeout(command)).isInstanceOf(RedisException.class);
    }

    @Test
    void closeWithBufferedCommands() {

        when(clientOptions.getDisconnectedBehavior()).thenReturn(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS);
        prepareNewEndpoint();

        sut.write(command);

        sut.closeAsync();

        verify(clusterChannelWriter).write(command);
    }

    @Test
    void closeWithCancelledBufferedCommands() {

        when(clientOptions.getDisconnectedBehavior()).thenReturn(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS);
        prepareNewEndpoint();

        sut.write(command);
        command.cancel();

        sut.closeAsync();

        verifyNoInteractions(clusterChannelWriter);
    }

    @Test
    void closeWithBufferedCommandsFails() {

        when(clientOptions.getDisconnectedBehavior()).thenReturn(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS);
        prepareNewEndpoint();

        sut.write(command);
        when(clusterChannelWriter.write(any(RedisCommand.class))).thenThrow(new RedisException(""));

        sut.closeAsync();

        assertThatThrownBy(() -> TestFutures.awaitOrTimeout(command)).isInstanceOf(RedisException.class);
    }

    private void prepareNewEndpoint() {
        sut = new ClusterNodeEndpoint(clientOptions, clientResources, clusterChannelWriter);
        disconnectedBuffer = (Queue) ReflectionTestUtils.getField(sut, "disconnectedBuffer");
    }
}
