package io.lettuce.core.masterreplica;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
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
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.PipelinedRedisFuture;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.async.RedisSentinelAsyncCommands;

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
    private ClientResources clientResources;

    @Mock
    private StatefulRedisSentinelConnection<String, String> connection;

    @Mock
    private RedisSentinelAsyncCommands<String, String> async;

    private SentinelTopologyProvider sut;

    @BeforeEach
    void before() {

        when(redisClient.getResources()).thenReturn(clientResources);
        when(connection.async()).thenReturn(async);
        when(connection.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(async.master(MASTER_ID)).thenReturn(completed(node("127.0.0.1", "6482", "master")));

        RedisURI sentinelUri = RedisURI.Builder.sentinel("127.0.0.1", 26379, MASTER_ID).build();
        when(redisClient.connectSentinelAsync(StringCodec.UTF8, sentinelUri))
                .thenReturn(CompletableFuture.completedFuture(connection));

        sut = new SentinelTopologyProvider(MASTER_ID, redisClient, sentinelUri);
    }

    @Test
    void shouldUseReplicas() {

        when(async.replicas(MASTER_ID)).thenReturn(completed(Collections.singletonList(node("127.0.0.1", "6483", "slave"))));

        List<RedisNodeDescription> nodes = sut.getNodesAsync().join();

        assertThat(nodes).hasSize(2);
        assertThat(nodes.get(0).getRole()).isEqualTo(RedisInstance.Role.UPSTREAM);
        assertThat(nodes.get(1).getRole()).isEqualTo(RedisInstance.Role.REPLICA);
    }

    @Test
    void shouldContinueWithoutReplicasWhenReplicasIsUnknown() {

        // A Sentinel implementation fronting a proxied endpoint, such as the Redis Enterprise discovery service, does not
        // implement SENTINEL REPLICAS and exposes no client-visible replicas. A single upstream node is the correct topology.
        when(async.replicas(MASTER_ID)).thenReturn(failed(new RedisCommandExecutionException("ERR sentinel unknown command")));

        List<RedisNodeDescription> nodes = sut.getNodesAsync().join();

        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0).getRole()).isEqualTo(RedisInstance.Role.UPSTREAM);
        assertThat(nodes.get(0).getUri().getHost()).isEqualTo("127.0.0.1");
        assertThat(nodes.get(0).getUri().getPort()).isEqualTo(6482);
    }

    @Test
    void shouldContinueWithoutReplicasWhenTheSubcommandIsUnknown() {

        // A Sentinel predating SENTINEL REPLICAS reports an unknown subcommand, capitalised differently again. Such a server
        // connects with an upstream-only topology rather than failing; its replicas are not discovered.
        when(async.replicas(MASTER_ID)).thenReturn(failed(new RedisCommandExecutionException(
                "ERR Unknown sentinel subcommand or wrong number of arguments for 'REPLICAS'")));

        List<RedisNodeDescription> nodes = sut.getNodesAsync().join();

        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0).getRole()).isEqualTo(RedisInstance.Role.UPSTREAM);
    }

    @Test
    void shouldNotSwallowOtherCommandErrors() {

        // The recovery is conditional on the server reporting an unknown command. Any other error reply - here a missing
        // permission - must surface instead of being hidden behind an empty replica list.
        when(async.replicas(MASTER_ID)).thenReturn(failed(new RedisCommandExecutionException(
                "NOPERM this user has no permissions to run the 'sentinel|replicas' command")));

        assertThatThrownBy(() -> sut.getNodesAsync().join()).hasCauseInstanceOf(RedisCommandExecutionException.class);
    }

    @Test
    void shouldNotSwallowAnUnknownMasterName() {

        // The fallback must not fabricate a topology for a master the Sentinel does not monitor: SENTINEL MASTER is combined
        // with the replica lookup, so the lookup as a whole still errors.
        when(async.master(MASTER_ID))
                .thenReturn(failed(new RedisCommandExecutionException("ERR No such master with that name")));
        when(async.replicas(MASTER_ID)).thenReturn(failed(new RedisCommandExecutionException("ERR sentinel unknown command")));

        assertThatThrownBy(() -> sut.getNodesAsync().join()).hasCauseInstanceOf(RedisCommandExecutionException.class)
                .hasRootCauseMessage("ERR No such master with that name");
    }

    @Test
    void shouldNotSwallowConnectionFailures() {

        // Only unknown-command error replies are absorbed; a broken connection carries no such reply and must surface.
        when(async.replicas(MASTER_ID)).thenReturn(failed(new RedisConnectionException("Connection reset")));

        assertThatThrownBy(() -> sut.getNodesAsync().join()).hasCauseInstanceOf(RedisConnectionException.class);
    }

    @AfterEach
    void neverIssuesTheDeprecatedSlavesCommand() {
        // SENTINEL SLAVES is no longer part of any code path: replicas() is issued once and nothing retries.
        verify(async, never()).slaves(MASTER_ID);
    }

    private static <T> RedisFuture<T> completed(T value) {
        return new PipelinedRedisFuture<>(CompletableFuture.completedFuture(value));
    }

    private static <T> RedisFuture<T> failed(Throwable error) {
        return new PipelinedRedisFuture<>(Futures.failed(error));
    }

    private static Map<String, String> node(String ip, String port, String flags) {

        Map<String, String> node = new LinkedHashMap<>();
        node.put("ip", ip);
        node.put("port", port);
        node.put("flags", flags);
        return Collections.unmodifiableMap(node);
    }

}
