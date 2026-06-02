package io.lettuce.core.cluster;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.concurrent.CompletionStage;

import io.lettuce.core.EmptyStatefulRedisConnection;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;

/**
 * @author Mark Paluch
 */
class EmptyRedisClusterClient extends RedisClusterClient {

    public EmptyRedisClusterClient(RedisURI initialUri) {
        super(null, Collections.singleton(initialUri));
    }

    @Override
    <K, V> StatefulRedisConnection<K, V> connectToNode(RedisCodec<K, V> codec, String nodeId, RedisChannelWriter clusterWriter,
            Supplier<CompletionStage<SocketAddress>> socketAddressSupplier) {
        return EmptyStatefulRedisConnection.INSTANCE;
    }

}
