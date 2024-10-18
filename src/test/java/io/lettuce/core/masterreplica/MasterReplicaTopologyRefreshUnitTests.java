package io.lettuce.core.masterreplica;

import static io.lettuce.TestTags.UNIT_TEST;
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
import org.junit.jupiter.api.Tag;
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
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MasterReplicaTopologyRefreshUnitTests {

    private static final RedisMasterReplicaNode UPSTREAM = new RedisMasterReplicaNode("localhost", 1, new RedisURI(),
            RedisInstance.Role.UPSTREAM);

    private static final RedisMasterReplicaNode REPLICA = new RedisMasterReplicaNode("localhost", 2, new RedisURI(),
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

        MasterReplicaTopologyRefresh refresh = new MasterReplicaTopologyRefresh(connectionFactory, executorService, provider);

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

        MasterReplicaTopologyRefresh refresh = new MasterReplicaTopologyRefresh(connectionFactory, executorService, provider);

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
