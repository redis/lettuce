package com.lambdaworks.redis.masterslave;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RedisNodeDescription;
import com.lambdaworks.redis.models.role.RoleParser;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Topology provider for a static node collection. This provider uses a static collection of nodes to determine the role of each
 * {@link RedisURI node}. Node roles may change during runtime but the configuration must remain the same. This
 * {@link TopologyProvider} does not auto-discover nodes.
 * 
 * @author Mark Paluch
 */
public class StaticMasterSlaveTopologyProvider implements TopologyProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(StaticMasterSlaveTopologyProvider.class);

    private final RedisClient redisClient;
    private final Iterable<RedisURI> redisURIs;

    public StaticMasterSlaveTopologyProvider(RedisClient redisClient, Iterable<RedisURI> redisURIs) {

        LettuceAssert.notNull(redisClient, "RedisClient must not be null");
        LettuceAssert.notNull(redisURIs, "RedisURIs must not be null");
        LettuceAssert.notNull(redisURIs.iterator().hasNext(), "RedisURIs must not be empty");

        this.redisClient = redisClient;
        this.redisURIs = redisURIs;
    }

    @Override
    public List<RedisNodeDescription> getNodes() {

        List<StatefulRedisConnection<String, String>> connections = new ArrayList<>();
        Map<RedisURI, RedisFuture<List<Object>>> roles = new HashMap<>();

        try {
            for (RedisURI redisURI : redisURIs) {
                try {
                    StatefulRedisConnection<String, String> connection = redisClient.connect(redisURI);
                    connections.add(connection);

                    roles.put(redisURI, connection.async().role());
                } catch (RuntimeException e) {
                    logger.warn("Cannot connect to {}", redisURI, e);
                }
            }

            RedisURI next = redisURIs.iterator().next();
            boolean success = LettuceFutures.awaitAll(next.getTimeout(), next.getUnit(),
                    roles.values().toArray(new Future[roles.size()]));

            if (success) {

                List<RedisNodeDescription> result = new ArrayList<>();
                for (Map.Entry<RedisURI, RedisFuture<List<Object>>> entry : roles.entrySet()) {

                    if (!entry.getValue().isDone()) {
                        continue;
                    }

                    RedisURI key = entry.getKey();

                    RedisInstance redisInstance = RoleParser.parse(entry.getValue().get());
                    result.add(new RedisMasterSlaveNode(key.getHost(), key.getPort(), key, redisInstance.getRole()));
                }

                return result;
            }
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            throw new RedisCommandInterruptedException(e);

        } finally {

            for (StatefulRedisConnection<String, String> connection : connections) {
                connection.close();
            }
        }

        return Collections.emptyList();
    }
}
