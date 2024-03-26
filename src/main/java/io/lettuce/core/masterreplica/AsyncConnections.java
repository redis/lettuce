package io.lettuce.core.masterreplica;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * @author Mark Paluch
 */
class AsyncConnections {

    private final Map<RedisURI, CompletableFuture<StatefulRedisConnection<String, String>>> connections = new TreeMap<>(
            ReplicaUtils.RedisURIComparator.INSTANCE);

    private final List<RedisNodeDescription> nodeList;

    AsyncConnections(List<RedisNodeDescription> nodeList) {
        this.nodeList = nodeList;
    }

    /**
     * Add a connection for a {@link RedisURI}
     *
     * @param redisURI
     * @param connection
     */
    public void addConnection(RedisURI redisURI, CompletableFuture<StatefulRedisConnection<String, String>> connection) {
        connections.put(redisURI, connection);
    }

    public Mono<Connections> asMono(Duration timeout, ScheduledExecutorService timeoutExecutor) {

        Connections connections = new Connections(this.connections.size(), nodeList);

        for (Map.Entry<RedisURI, CompletableFuture<StatefulRedisConnection<String, String>>> entry : this.connections
                .entrySet()) {

            CompletableFuture<StatefulRedisConnection<String, String>> future = entry.getValue();

            future.whenComplete((connection, throwable) -> {

                if (throwable != null) {
                    connections.accept(throwable);
                } else {
                    connections.accept(Tuples.of(entry.getKey(), connection));
                }
            });
        }

        return Mono.fromCompletionStage(connections.getOrTimeout(timeout, timeoutExecutor));
    }

}
