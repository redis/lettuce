package com.lambdaworks.redis.masterslave;

import static com.lambdaworks.redis.masterslave.MasterSlaveUtils.CODEC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RedisNodeDescription;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Topology provider using Redis Sentinel and the Sentinel API.
 *
 * @author Mark Paluch
 * @since 4.1
 */
public class SentinelTopologyProvider implements TopologyProvider {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SentinelTopologyProvider.class);

    private final String masterId;
    private final RedisClient redisClient;
    private final RedisURI sentinelUri;
    private final long timeout;
    private final TimeUnit timeUnit;

    public SentinelTopologyProvider(String masterId, RedisClient redisClient, RedisURI sentinelUri) {
        this.masterId = masterId;
        this.redisClient = redisClient;
        this.sentinelUri = sentinelUri;
        this.timeout = sentinelUri.getTimeout();
        this.timeUnit = sentinelUri.getUnit();
    }

    @Override
    public List<RedisNodeDescription> getNodes() {

        logger.debug("lookup topology for masterId {}", masterId);

        try (StatefulRedisSentinelConnection<String, String> connection = redisClient.connectSentinel(CODEC, sentinelUri)) {

            RedisFuture<Map<String, String>> masterFuture = connection.async().master(masterId);
            RedisFuture<List<Map<String, String>>> slavesFuture = connection.async().slaves(masterId);

            List<RedisNodeDescription> result = new ArrayList<>();
            try {
                Map<String, String> master = masterFuture.get(timeout, timeUnit);
                List<Map<String, String>> slaves = slavesFuture.get(timeout, timeUnit);

                result.add(toNode(master, RedisInstance.Role.MASTER));
                result.addAll(slaves.stream().map(map -> toNode(map, RedisInstance.Role.SLAVE)).collect(Collectors.toList()));

            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                throw new RedisException(e);
            }

            return result;
        }
    }

    private RedisNodeDescription toNode(Map<String, String> map, RedisInstance.Role role) {
        String ip = map.get("ip");
        String port = map.get("port");
        return new RedisMasterSlaveNode(ip, Integer.parseInt(port), role);
    }

}
