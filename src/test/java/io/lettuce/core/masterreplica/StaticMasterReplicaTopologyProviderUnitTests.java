package io.lettuce.core.masterreplica;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.PipelinedRedisFuture;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * Unit tests for {@link StaticMasterReplicaTopologyProvider}.
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaticMasterReplicaTopologyProviderUnitTests {

    private static final RedisURI NODE = RedisURI.create("redis://localhost:6379");

    @Mock
    private RedisClient redisClient;

    @Mock
    private StatefulRedisConnection<String, String> connection;

    @Mock
    private RedisAsyncCommands<String, String> async;

    private StaticMasterReplicaTopologyProvider sut;

    @BeforeEach
    void before() {

        when(connection.async()).thenReturn(async);
        when(connection.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(redisClient.connectAsync(StringCodec.UTF8, NODE))
                .thenReturn(ConnectionFuture.completed(InetSocketAddress.createUnresolved("localhost", 6379), connection));

        sut = new StaticMasterReplicaTopologyProvider(redisClient, Collections.singletonList(NODE));
    }

    @Test
    void shouldUnwrapRoleFailure() {

        RedisConnectionException failure = new RedisConnectionException("Connection reset");
        when(async.role()).thenReturn(new PipelinedRedisFuture<>(Futures.failed(failure)));

        CompletableFuture<List<RedisNodeDescription>> nodes = sut.getNodesAsync();

        assertThat(nodes).isCompletedExceptionally();
        assertThatThrownBy(nodes::join).isInstanceOf(CompletionException.class).hasCause(failure);
    }

    @Test
    void shouldCompleteWhenRoleFailsWithCauselessCompletionException() {

        // Exceptions.unwrap returns null for a CompletionException without a cause; completing with null would throw inside
        // the callback and leave the topology lookup pending forever.
        CompletionException causeless = new CompletionException(null);
        when(async.role()).thenReturn(new PipelinedRedisFuture<>(Futures.failed(causeless)));

        CompletableFuture<List<RedisNodeDescription>> nodes = sut.getNodesAsync();

        assertThat(nodes).isCompletedExceptionally();
        assertThatThrownBy(nodes::join).isSameAs(causeless);
    }

}
