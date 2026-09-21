package io.lettuce.core.masterreplica;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link SentinelTopologyProvider}.
 *
 * @author Redis
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SentinelTopologyProviderUnitTests {

    private static final String MASTER_ID = "mymaster";

    @Mock
    private RedisClient redisClient;

    @Mock
    private StatefulRedisSentinelConnection<String, String> connection;

    @Mock
    private RedisSentinelReactiveCommands<String, String> reactive;

    private SentinelTopologyProvider sut;

    @BeforeEach
    void before() {

        when(connection.reactive()).thenReturn(reactive);
        when(connection.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(reactive.master(MASTER_ID)).thenReturn(Mono.just(node("127.0.0.1", "6482", "master")));

        sut = new SentinelTopologyProvider(MASTER_ID, redisClient,
                RedisURI.Builder.sentinel("127.0.0.1", 26379, MASTER_ID).build());
    }

    @Test
    void shouldUseReplicas() {

        when(reactive.replicas(MASTER_ID)).thenReturn(Flux.just(node("127.0.0.1", "6483", "slave")));

        List<RedisNodeDescription> nodes = sut.getNodes(connection).block();

        assertThat(nodes).hasSize(2);
        assertThat(nodes.get(0).getRole()).isEqualTo(RedisInstance.Role.UPSTREAM);
        assertThat(nodes.get(1).getRole()).isEqualTo(RedisInstance.Role.REPLICA);
        verify(reactive, never()).slaves(MASTER_ID);
    }

    @Test
    void shouldFallBackToSlavesWhenReplicasIsUnsupported() {

        when(reactive.replicas(MASTER_ID))
                .thenReturn(Flux.error(new RedisCommandExecutionException("ERR sentinel unknown command")));
        when(reactive.slaves(MASTER_ID)).thenReturn(Flux.just(node("127.0.0.1", "6483", "slave")));

        List<RedisNodeDescription> nodes = sut.getNodes(connection).block();

        assertThat(nodes).hasSize(2);
        assertThat(nodes.get(1).getRole()).isEqualTo(RedisInstance.Role.REPLICA);
    }

    @Test
    void shouldFallBackToAnEmptyReplicaList() {

        // A Sentinel implementation fronting a proxied endpoint, such as the Redis Enterprise discovery service, exposes no
        // client-visible replicas. A single upstream node is the correct topology.
        when(reactive.replicas(MASTER_ID))
                .thenReturn(Flux.error(new RedisCommandExecutionException("ERR sentinel unknown command")));
        when(reactive.slaves(MASTER_ID))
                .thenReturn(Flux.error(new RedisCommandExecutionException("ERR sentinel unknown command")));

        List<RedisNodeDescription> nodes = sut.getNodes(connection).block();

        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0).getRole()).isEqualTo(RedisInstance.Role.UPSTREAM);
        assertThat(nodes.get(0).getUri().getHost()).isEqualTo("127.0.0.1");
        assertThat(nodes.get(0).getUri().getPort()).isEqualTo(6482);
    }

    @Test
    void shouldNotSwallowAnUnknownMasterName() {

        // The fallback must not fabricate a topology for a master the Sentinel does not monitor: SENTINEL MASTER fails in
        // the same zipWith, so the lookup as a whole still errors.
        when(reactive.master(MASTER_ID))
                .thenReturn(Mono.error(new RedisCommandExecutionException("ERR No such master with that name")));
        when(reactive.replicas(MASTER_ID))
                .thenReturn(Flux.error(new RedisCommandExecutionException("ERR sentinel unknown command")));
        when(reactive.slaves(MASTER_ID))
                .thenReturn(Flux.error(new RedisCommandExecutionException("ERR sentinel unknown command")));

        StepVerifier.create(sut.getNodes(connection)).verifyError(RedisCommandExecutionException.class);
    }

    @Test
    void shouldNotSwallowConnectionFailures() {

        // Only command-execution failures are absorbed; a broken connection must still surface.
        when(reactive.replicas(MASTER_ID)).thenReturn(Flux.error(new RedisConnectionException("Connection reset")));

        StepVerifier.create(sut.getNodes(connection)).verifyError(RedisConnectionException.class);
        verify(reactive, never()).slaves(MASTER_ID);
    }

    private static Map<String, String> node(String ip, String port, String flags) {

        Map<String, String> node = new LinkedHashMap<>();
        node.put("ip", ip);
        node.put("port", port);
        node.put("flags", flags);
        return Collections.unmodifiableMap(node);
    }

}
