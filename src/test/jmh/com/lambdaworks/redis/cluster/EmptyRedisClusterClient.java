package com.lambdaworks.redis.cluster;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.function.Supplier;

import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * @author Mark Paluch
 */
public class EmptyRedisClusterClient extends RedisClusterClient {
    private final static EmptyStatefulRedisConnection CONNECTION = new EmptyStatefulRedisConnection();

    public EmptyRedisClusterClient(RedisURI initialUri) {
        super(null, Collections.singleton(initialUri));
    }

    <K, V> StatefulRedisConnection<K, V> connectToNode(RedisCodec<K, V> codec, String nodeId,
            RedisChannelWriter clusterWriter, final Supplier<SocketAddress> socketAddressSupplier) {
        return CONNECTION;
    }
}
