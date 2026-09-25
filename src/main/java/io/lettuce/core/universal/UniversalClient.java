package io.lettuce.core.universal;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.BaseRedisClient;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.UniversalClientImpl;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.resource.ClientResources;

/**
 * A client that connects to either a standalone server or an OSS cluster from the same seed URIs, detecting the topology at
 * connect time. Returns one connection type, {@link StatefulRedisUniversalConnection}, regardless of what was detected.
 * <p>
 * Detection uses only OSS-visible signals: the {@code mode} field of the {@code HELLO} reply (RESP3) with an
 * {@code INFO server} / {@code redis_mode} fallback (RESP2). The detected mode is fixed for the lifetime of a connection.
 * <p>
 * PoC. Not covered: pub/sub, sentinel, master/replica, topology flips without reconnect.
 *
 * @since 7.x (PoC)
 */
public interface UniversalClient extends BaseRedisClient {

    /**
     * Create a client with default {@link ClientResources}.
     */
    static UniversalClient create(RedisURI... seeds) {
        return UniversalClientImpl.create(null, Arrays.asList(seeds));
    }

    /**
     * Create a client on shared {@link ClientResources}.
     */
    static UniversalClient create(ClientResources resources, RedisURI... seeds) {
        return UniversalClientImpl.create(resources, Arrays.asList(seeds));
    }

    /**
     * Set client options. Applies to both the standalone and the cluster path; {@link ClusterClientOptions} is-a
     * {@link io.lettuce.core.ClientOptions}. Set before the first {@link #connect(RedisCodec)}.
     */
    void setOptions(ClusterClientOptions options);

    /**
     * @return the current client options.
     */
    ClusterClientOptions getOptions();

    /**
     * Detect the topology and open the matching connection.
     */
    <K, V> StatefulRedisUniversalConnection<K, V> connect(RedisCodec<K, V> codec);

    /**
     * Asynchronous variant of {@link #connect(RedisCodec)}.
     */
    <K, V> CompletableFuture<StatefulRedisUniversalConnection<K, V>> connectAsync(RedisCodec<K, V> codec);

    /**
     * {@link #connect(RedisCodec)} with {@link StringCodec#UTF8}.
     */
    default StatefulRedisUniversalConnection<String, String> connect() {
        return connect(StringCodec.UTF8);
    }

}
