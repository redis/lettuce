package io.lettuce.core.masterreplica;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.ExceptionFactory;
import io.lettuce.core.internal.Exceptions;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Topology provider using Redis Sentinel and the Sentinel API.
 *
 * @author Mark Paluch
 * @since 4.1
 */
class SentinelTopologyProvider implements TopologyProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SentinelTopologyProvider.class);

    private final String masterId;

    private final RedisClient redisClient;

    private final RedisURI sentinelUri;

    private final Duration timeout;

    /**
     * Creates a new {@link SentinelTopologyProvider}.
     *
     * @param masterId must not be empty
     * @param redisClient must not be {@code null}.
     * @param sentinelUri must not be {@code null}.
     */
    public SentinelTopologyProvider(String masterId, RedisClient redisClient, RedisURI sentinelUri) {

        LettuceAssert.notEmpty(masterId, "MasterId must not be empty");
        LettuceAssert.notNull(redisClient, "RedisClient must not be null");
        LettuceAssert.notNull(sentinelUri, "Sentinel URI must not be null");

        this.masterId = masterId;
        this.redisClient = redisClient;
        this.sentinelUri = sentinelUri;
        this.timeout = sentinelUri.getTimeout();
    }

    @Override
    public List<RedisNodeDescription> getNodes() {

        logger.debug("lookup topology for masterId {}", masterId);

        try {
            return getNodesAsync().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw Exceptions.bubble(e);
        }
    }

    @Override
    public CompletableFuture<List<RedisNodeDescription>> getNodesAsync() {

        logger.debug("lookup topology for masterId {}", masterId);

        Mono<StatefulRedisSentinelConnection<String, String>> connect = Mono
                .fromFuture(redisClient.connectSentinelAsync(StringCodec.UTF8, sentinelUri));

        return connect.flatMap(this::getNodes).toFuture();
    }

    protected Mono<List<RedisNodeDescription>> getNodes(StatefulRedisSentinelConnection<String, String> connection) {

        RedisSentinelReactiveCommands<String, String> reactive = connection.reactive();

        Mono<Tuple2<Map<String, String>, List<Map<String, String>>>> masterAndReplicas = reactive.master(masterId)
                .zipWith(getReplicas(reactive)).timeout(this.timeout).flatMap(tuple -> {
                    return ResumeAfter.close(connection).thenEmit(tuple);
                }).doOnError(e -> connection.closeAsync());

        return masterAndReplicas.map(tuple -> {

            List<RedisNodeDescription> result = new ArrayList<>();

            result.add(toNode(tuple.getT1(), RedisInstance.Role.UPSTREAM));
            result.addAll(tuple.getT2().stream().filter(SentinelTopologyProvider::isAvailable)
                    .map(map -> toNode(map, RedisInstance.Role.REPLICA)).collect(Collectors.toList()));

            return result;
        });
    }

    /**
     * Look up replicas using {@code SENTINEL REPLICAS}, continuing with an empty replica list if the server does not know that
     * command.
     * <p>
     * An empty replica list is a correct answer for a Sentinel implementation that fronts a proxied endpoint, such as the Redis
     * Enterprise discovery service: there are no client-visible replicas, so the topology consists of the upstream node only.
     * Note the same applies to a Sentinel predating {@code SENTINEL REPLICAS}, which connects with an upstream-only topology
     * rather than failing - its replicas are not discovered.
     * <p>
     * Only an unknown-command reply is absorbed, so authorization, connection and timeout failures still propagate, as does
     * every other command error. A master name the Sentinel does not monitor still fails through {@code SENTINEL MASTER} in the
     * same {@code zipWith}.
     *
     * @param reactive Sentinel commands to use.
     * @return the replicas of the monitored master, empty if the server does not implement the command.
     */
    private Mono<List<Map<String, String>>> getReplicas(RedisSentinelReactiveCommands<String, String> reactive) {

        return reactive.replicas(masterId).collectList().onErrorResume(ExceptionFactory::isUnknownCommandError, e -> {

            logger.info("{} does not implement SENTINEL REPLICAS, continuing with an empty replica list for masterId {}",
                    sentinelUri, masterId);

            return Mono.just(Collections.<Map<String, String>> emptyList());
        });
    }

    private static boolean isAvailable(Map<String, String> map) {

        String flags = map.get("flags");
        if (flags != null) {
            if (flags.contains("s_down") || flags.contains("o_down") || flags.contains("disconnected")) {
                return false;
            }
        }
        return true;
    }

    private RedisNodeDescription toNode(Map<String, String> map, RedisInstance.Role role) {

        String ip = map.get("ip");
        String port = map.get("port");
        return new RedisMasterReplicaNode(ip, Integer.parseInt(port), sentinelUri, role);
    }

}
