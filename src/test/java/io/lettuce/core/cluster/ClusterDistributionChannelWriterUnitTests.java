/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisChannelWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.CommandListenerWriter;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.output.ValueOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandExpiryWriter;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ConnectionIntent;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

/**
 * Unit tests for {@link ClusterDistributionChannelWriter}.
 *
 * @author Mark Paluch
 * @author koisyu
 * @author Jim Brunner
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClusterDistributionChannelWriterUnitTests {

    @Mock
    private DefaultEndpoint defaultWriter;

    @Mock
    private EventBus eventBus;

    @Mock
    private ClientResources clientResources;

    private ClientOptions clientOptions = ClientOptions.create();

    @Mock
    private ClusterEventListener clusterEventListener;

    @Mock
    private StatefulRedisConnectionImpl<String, String> connection;

    @Mock
    private ClusterNodeEndpoint clusterNodeEndpoint;

    @Mock
    private CompletableFuture<StatefulRedisConnection<String, String>> connectFuture;

    @Mock
    private PooledClusterConnectionProvider pooledClusterConnectionProvider;

    private ClusterDistributionChannelWriter clusterDistributionChannelWriter;

    @BeforeEach
    void setUp() {
        when(defaultWriter.getClientResources()).thenReturn(clientResources);
        when(clientResources.eventBus()).thenReturn(eventBus);

        clusterDistributionChannelWriter = new ClusterDistributionChannelWriter(defaultWriter, clientOptions,
                clusterEventListener);
    }

    @Test
    void shouldParseAskTargetCorrectly() {

        HostAndPort askTarget = ClusterDistributionChannelWriter.getAskTarget("ASK 1234-2020 127.0.0.1:6381");

        assertThat(askTarget.getHostText()).isEqualTo("127.0.0.1");
        assertThat(askTarget.getPort()).isEqualTo(6381);
    }

    @Test
    void shouldParseIPv6AskTargetCorrectly() {

        HostAndPort askTarget = ClusterDistributionChannelWriter.getAskTarget("ASK 1234-2020 1:2:3:4::6:6381");

        assertThat(askTarget.getHostText()).isEqualTo("1:2:3:4::6");
        assertThat(askTarget.getPort()).isEqualTo(6381);
    }

    @Test
    void shouldParseMovedTargetCorrectly() {

        HostAndPort moveTarget = ClusterDistributionChannelWriter.getMoveTarget(new Partitions(),
                "MOVED 1234-2020 127.0.0.1:6381");

        assertThat(moveTarget.getHostText()).isEqualTo("127.0.0.1");
        assertThat(moveTarget.getPort()).isEqualTo(6381);
    }

    @Test
    void shouldParseMovedTargetWithoutHostnameCorrectly() {

        Partitions partitions = new Partitions();
        partitions.add(new RedisClusterNode(RedisURI.create("redis://1.2.3.4:6381"), "foo", false, null, 0, 0, 0,
                Collections.emptyList(), Collections.emptySet()));
        HostAndPort moveTarget = ClusterDistributionChannelWriter.getMoveTarget(partitions, "MOVED 1234 :6381");

        assertThat(moveTarget.getHostText()).isEqualTo("1.2.3.4");
        assertThat(moveTarget.getPort()).isEqualTo(6381);
    }

    @Test
    void shouldParseMovedTargetWithoutHostnameUsingSlotFallbackCorrectly() {

        Partitions partitions = new Partitions();
        partitions.add(new RedisClusterNode(RedisURI.create("redis://1.2.3.4:5678"), "foo", false, null, 0, 0, 0,
                Collections.singletonList(1234), Collections.emptySet()));
        HostAndPort moveTarget = ClusterDistributionChannelWriter.getMoveTarget(partitions, "MOVED 1234 :6381");

        assertThat(moveTarget.getHostText()).isEqualTo("1.2.3.4");
        assertThat(moveTarget.getPort()).isEqualTo(6381);
    }

    @Test
    void shouldParseIPv6MovedTargetCorrectly() {

        HostAndPort moveTarget = ClusterDistributionChannelWriter.getMoveTarget(new Partitions(),
                "MOVED 1234-2020 1:2:3:4::6:6381");

        assertThat(moveTarget.getHostText()).isEqualTo("1:2:3:4::6");
        assertThat(moveTarget.getPort()).isEqualTo(6381);
    }

    @Test
    void shouldReturnIntentForWriteCommand() {

        ClusterDistributionChannelWriter writer = new ClusterDistributionChannelWriter(clusterDistributionChannelWriter,
                clientOptions, clusterEventListener);

        RedisCommand<String, String, String> set = new Command<>(CommandType.SET, null);
        RedisCommand<String, String, String> mset = new Command<>(CommandType.MSET, null);

        assertThat(writer.getIntent(Arrays.asList(set, mset))).isEqualTo(ConnectionIntent.WRITE);

        assertThat(writer.getIntent(Collections.singletonList(set))).isEqualTo(ConnectionIntent.WRITE);
    }

    @Test
    void shouldReturnDefaultIntentForNoCommands() {

        ClusterDistributionChannelWriter writer = new ClusterDistributionChannelWriter(clusterDistributionChannelWriter,
                clientOptions, clusterEventListener);

        assertThat(writer.getIntent(Collections.emptyList())).isEqualTo(ConnectionIntent.WRITE);
    }

    @Test
    void shouldReturnIntentForReadCommand() {

        ClusterDistributionChannelWriter writer = new ClusterDistributionChannelWriter(clusterDistributionChannelWriter,
                clientOptions, clusterEventListener);

        RedisCommand<String, String, String> get = new Command<>(CommandType.GET, null);
        RedisCommand<String, String, String> mget = new Command<>(CommandType.MGET, null);

        assertThat(writer.getIntent(Arrays.asList(get, mget))).isEqualTo(ConnectionIntent.READ);

        assertThat(writer.getIntent(Collections.singletonList(get))).isEqualTo(ConnectionIntent.READ);
    }

    @Test
    void shouldReturnIntentForMixedCommands() {

        ClusterDistributionChannelWriter writer = new ClusterDistributionChannelWriter(clusterDistributionChannelWriter,
                clientOptions, clusterEventListener);

        RedisCommand<String, String, String> set = new Command<>(CommandType.SET, null);
        RedisCommand<String, String, String> mget = new Command<>(CommandType.MGET, null);

        assertThat(writer.getIntent(Arrays.asList(set, mget))).isEqualTo(ConnectionIntent.WRITE);

        assertThat(writer.getIntent(Collections.singletonList(set))).isEqualTo(ConnectionIntent.WRITE);
    }

    @Test
    void shouldWriteCommandListWhenAsking() {
        verifyWriteCommandCountWhenRedirecting(false);
    }

    @Test
    void shouldDisconnectWrappedEndpoint() {

        RedisChannelWriter expiryWriter = CommandExpiryWriter.buildCommandExpiryWriter(defaultWriter,
                ClientOptions.builder().timeoutOptions(TimeoutOptions.enabled()).build(), clientResources);
        RedisChannelWriter listenerWriter = new CommandListenerWriter(expiryWriter, Collections.emptyList());

        ClusterDistributionChannelWriter writer = new ClusterDistributionChannelWriter(listenerWriter, ClientOptions.create(),
                clusterEventListener);

        writer.disconnectDefaultEndpoint();

        verify(defaultWriter).disconnect();
    }

    @Test
    void shouldWriteOneCommandWhenMoved() {
        verifyWriteCommandCountWhenRedirecting(true);
    }

    private void verifyWriteCommandCountWhenRedirecting(boolean isMoved) {

        String outputError = isMoved ? "MOVED 1234 127.0.0.1:6379" : "ASK 1234 127.0.0.1:6379";

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8).addKey("KEY");
        ValueOutput<String, String> valueOutput = new ValueOutput<>(StringCodec.UTF8);
        Command<String, String, String> command = new Command<>(CommandType.GET, valueOutput, commandArgs);
        AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);
        ClusterCommand<String, String, String> clusterCommand = new ClusterCommand<>(asyncCommand, defaultWriter, 2);
        clusterCommand.getOutput().setError(outputError);
        clusterDistributionChannelWriter.setClusterConnectionProvider(pooledClusterConnectionProvider);

        when(connectFuture.isDone()).thenReturn(true);
        when(connectFuture.isCompletedExceptionally()).thenReturn(false);
        when(connectFuture.join()).thenReturn(connection);
        when(pooledClusterConnectionProvider.getConnectionAsync(any(ConnectionIntent.class), anyString(), anyInt()))
                .thenReturn(connectFuture);
        when(connection.getChannelWriter()).thenReturn(clusterNodeEndpoint);

        clusterDistributionChannelWriter.write(clusterCommand);

        if (isMoved) {
            verify(clusterNodeEndpoint, never()).write(anyList());
            verify(clusterNodeEndpoint, times(1)).write(ArgumentMatchers.<RedisCommand<String, String, String>> any());
        } else {
            verify(clusterNodeEndpoint, times(1)).write(anyList());
            verify(clusterNodeEndpoint, never()).write(ArgumentMatchers.<RedisCommand<String, String, String>> any());
        }
    }

}
