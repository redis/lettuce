/*
 * Copyright 2011-2020 the original author or authors.
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
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterConnectionProvider.Intent;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.output.ValueOutput;
import io.lettuce.core.protocol.*;

/**
 * Unit tests for {@link ClusterDistributionChannelWriter}.
 *
 * @author Mark Paluch
 * @author koisyu
 */
@ExtendWith(MockitoExtension.class)
class ClusterDistributionChannelWriterUnitTests {

    @Mock
    private RedisChannelWriter defaultWriter;

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

    @InjectMocks
    private ClusterDistributionChannelWriter clusterDistributionChannelWriter;

    @Test
    void shouldParseAskTargetCorrectly() {

        HostAndPort askTarget = ClusterDistributionChannelWriter.getAskTarget("ASK 1234 127.0.0.1:6381");

        assertThat(askTarget.getHostText()).isEqualTo("127.0.0.1");
        assertThat(askTarget.getPort()).isEqualTo(6381);
    }

    @Test
    void shouldParseIPv6AskTargetCorrectly() {

        HostAndPort askTarget = ClusterDistributionChannelWriter.getAskTarget("ASK 1234 1:2:3:4::6:6381");

        assertThat(askTarget.getHostText()).isEqualTo("1:2:3:4::6");
        assertThat(askTarget.getPort()).isEqualTo(6381);
    }

    @Test
    void shouldParseMovedTargetCorrectly() {

        HostAndPort moveTarget = ClusterDistributionChannelWriter.getMoveTarget("MOVED 1234 127.0.0.1:6381");

        assertThat(moveTarget.getHostText()).isEqualTo("127.0.0.1");
        assertThat(moveTarget.getPort()).isEqualTo(6381);
    }

    @Test
    void shouldParseIPv6MovedTargetCorrectly() {

        HostAndPort moveTarget = ClusterDistributionChannelWriter.getMoveTarget("MOVED 1234 1:2:3:4::6:6381");

        assertThat(moveTarget.getHostText()).isEqualTo("1:2:3:4::6");
        assertThat(moveTarget.getPort()).isEqualTo(6381);
    }

    @Test
    void shouldReturnIntentForWriteCommand() {

        RedisCommand<String, String, String> set = new Command<>(CommandType.SET, null);
        RedisCommand<String, String, String> mset = new Command<>(CommandType.MSET, null);

        assertThat(ClusterDistributionChannelWriter.getIntent(Arrays.asList(set, mset))).isEqualTo(Intent.WRITE);

        assertThat(ClusterDistributionChannelWriter.getIntent(Collections.singletonList(set))).isEqualTo(Intent.WRITE);
    }

    @Test
    void shouldReturnDefaultIntentForNoCommands() {

        assertThat(ClusterDistributionChannelWriter.getIntent(Collections.emptyList())).isEqualTo(Intent.WRITE);
    }

    @Test
    void shouldReturnIntentForReadCommand() {

        RedisCommand<String, String, String> get = new Command<>(CommandType.GET, null);
        RedisCommand<String, String, String> mget = new Command<>(CommandType.MGET, null);

        assertThat(ClusterDistributionChannelWriter.getIntent(Arrays.asList(get, mget))).isEqualTo(Intent.READ);

        assertThat(ClusterDistributionChannelWriter.getIntent(Collections.singletonList(get))).isEqualTo(Intent.READ);
    }

    @Test
    void shouldReturnIntentForMixedCommands() {

        RedisCommand<String, String, String> set = new Command<>(CommandType.SET, null);
        RedisCommand<String, String, String> mget = new Command<>(CommandType.MGET, null);

        assertThat(ClusterDistributionChannelWriter.getIntent(Arrays.asList(set, mget))).isEqualTo(Intent.WRITE);

        assertThat(ClusterDistributionChannelWriter.getIntent(Collections.singletonList(set))).isEqualTo(Intent.WRITE);
    }

    @Test
    void shouldWriteCommandListWhenAsking() {
        verifyWriteCommandCountWhenRedirecting(false);
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
        when(pooledClusterConnectionProvider.getConnectionAsync(any(Intent.class), anyString(), anyInt()))
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
