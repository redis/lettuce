/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.masterreplica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpstreamReplicaTopologyRefreshUnitTests {

    private static final RedisUpstreamReplicaNode UPSTREAM = new RedisUpstreamReplicaNode("localhost", 1, new RedisURI(),
            RedisInstance.Role.UPSTREAM);

    private static final RedisUpstreamReplicaNode REPLICA = new RedisUpstreamReplicaNode("localhost", 2, new RedisURI(),
            RedisInstance.Role.REPLICA);

    @Mock
    NodeConnectionFactory connectionFactory;

    @Mock
    StatefulRedisConnection<String, String> connection;

    @Mock
    RedisAsyncCommands<String, String> async;

    private ScheduledThreadPoolExecutor executorService;

    private TopologyProvider provider;

    @BeforeEach
    void before() {

        executorService = new ScheduledThreadPoolExecutor(1, new DefaultThreadFactory(getClass().getSimpleName(), true));
        when(connection.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(connection.async()).thenReturn(async);
        when(connection.dispatch(any(RedisCommand.class))).then(invocation -> {

            RedisCommand command = invocation.getArgument(0);
            command.complete();

            return null;
        });

        provider = () -> Arrays.asList(UPSTREAM, REPLICA);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }

    @Test
    void shouldRetrieveTopology() {

        UpstreamReplicaTopologyRefresh refresh = new UpstreamReplicaTopologyRefresh(connectionFactory, executorService,
                provider);

        CompletableFuture<StatefulRedisConnection<String, String>> master = CompletableFuture.completedFuture(connection);
        CompletableFuture<StatefulRedisConnection<String, String>> replica = CompletableFuture.completedFuture(connection);
        when(connectionFactory.connectToNodeAsync(any(), any())).thenReturn((CompletableFuture) master,
                (CompletableFuture) replica);

        RedisURI redisURI = new RedisURI();
        redisURI.setTimeout(Duration.ofMillis(1));

        List<RedisNodeDescription> nodes = refresh.getNodes(redisURI).block();

        assertThat(nodes).hasSize(2);
    }

    @Test
    void shouldRetrieveTopologyWithFailedNode() {

        UpstreamReplicaTopologyRefresh refresh = new UpstreamReplicaTopologyRefresh(connectionFactory, executorService,
                provider);

        CompletableFuture<StatefulRedisConnection<String, String>> connected = CompletableFuture.completedFuture(connection);
        CompletableFuture<StatefulRedisConnection<String, String>> pending = new CompletableFuture<>();
        when(connectionFactory.connectToNodeAsync(any(), any())).thenReturn((CompletableFuture) connected,
                (CompletableFuture) pending);

        RedisURI redisURI = new RedisURI();
        redisURI.setTimeout(Duration.ofMillis(1));

        List<RedisNodeDescription> nodes = refresh.getNodes(redisURI).block();

        assertThat(nodes).hasSize(1).containsOnly(UPSTREAM);
    }
}
