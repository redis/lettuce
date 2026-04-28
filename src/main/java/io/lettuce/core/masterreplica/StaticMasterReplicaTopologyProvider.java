package io.lettuce.core.masterreplica;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.Exceptions;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.models.role.RoleParser;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Topology provider for a static node collection. This provider uses a static collection of nodes to determine the role of each
 * {@link RedisURI node}. Node roles may change during runtime but the configuration must remain the same. This
 * {@link TopologyProvider} does not auto-discover nodes.
 *
 * @author Mark Paluch
 * @author Adam McElwee
 */
class StaticMasterReplicaTopologyProvider implements TopologyProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(StaticMasterReplicaTopologyProvider.class);

    private final RedisClient redisClient;

    private final Iterable<RedisURI> redisURIs;

    public StaticMasterReplicaTopologyProvider(RedisClient redisClient, Iterable<RedisURI> redisURIs) {

        LettuceAssert.notNull(redisClient, "RedisClient must not be null");
        LettuceAssert.notNull(redisURIs, "RedisURIs must not be null");
        LettuceAssert.notNull(redisURIs.iterator().hasNext(), "RedisURIs must not be empty");

        this.redisClient = redisClient;
        this.redisURIs = redisURIs;
    }

    @Override
    public List<RedisNodeDescription> getNodes() {

        RedisURI next = redisURIs.iterator().next();

        try {
            return getNodesAsync().get(next.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw Exceptions.bubble(e);
        }
    }

    @Override
    public CompletableFuture<List<RedisNodeDescription>> getNodesAsync() {

        List<CompletableFuture<RedisNodeDescription>> perUri = new ArrayList<>();
        for (RedisURI uri : redisURIs) {
            perUri.add(getNodeDescription(uri));
        }

        CompletableFuture<List<RedisNodeDescription>> result = new CompletableFuture<>();
        CompletableFuture.allOf(perUri.toArray(new CompletableFuture[0])).whenComplete((ignored, error) -> {

            if (error != null) {
                result.completeExceptionally(Exceptions.unwrap(error));
                return;
            }

            List<RedisNodeDescription> nodes = new ArrayList<>(perUri.size());
            for (CompletableFuture<RedisNodeDescription> f : perUri) {
                RedisNodeDescription nd = f.getNow(null);
                if (nd != null) {
                    nodes.add(nd);
                }
            }

            if (nodes.isEmpty()) {
                result.completeExceptionally(
                        new RedisConnectionException(String.format("Failed to connect to at least one node in %s", redisURIs)));
            } else {
                result.complete(nodes);
            }
        });

        return result;
    }

    private CompletableFuture<RedisNodeDescription> getNodeDescription(RedisURI uri) {

        return redisClient.connectAsync(StringCodec.UTF8, uri).toCompletableFuture().handle((connection, error) -> {

            if (error != null) {
                logger.warn("Cannot connect to {}", uri, error);
                return CompletableFuture.<RedisNodeDescription> completedFuture(null);
            }

            return getNodeDescription(uri, connection).whenComplete((nd, t) -> connection.closeAsync());
        }).thenCompose(f -> f);
    }

    private static CompletableFuture<RedisNodeDescription> getNodeDescription(RedisURI uri,
            StatefulRedisConnection<String, String> connection) {

        return connection.async().role().toCompletableFuture().thenApply(roleOutput -> new RedisMasterReplicaNode(uri.getHost(),
                uri.getPort(), uri, RoleParser.parse(roleOutput).getRole()));
    }

}
