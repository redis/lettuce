package io.lettuce.core.support.caching;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.StatefulRedisClusterConnectionImpl;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.protocol.ConnectionIntent;
import io.lettuce.core.protocol.ProtocolVersion;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Utility to provide server-side assistance for client-side caches. This is a {@link CacheFrontend} that represents a two-level
 * cache backed by a client-side and a Redis cache.
 *
 * For example:
 *
 * <pre class="code">
 *
 * Map<String, String> clientCache = new ConcurrentHashMap<>();
 *
 * StatefulRedisConnection&lt;String, String&gt; connection = redisClient.connect();
 *
 * CacheFrontend&lt;String, String&gt; frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
 *         TrackingArgs.Builder.enabled());
 *
 * String value = frontend.get(key);
 * </pre>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @author Julien Ruaux
 * @since 6.0
 */
public class ClientSideCaching<K, V> implements CacheFrontend<K, V> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(ClientSideCaching.class);

    private final CacheAccessor<K, V> cacheAccessor;

    private final RedisCache<K, V> redisCache;

    private final List<Consumer<K>> invalidationListeners = new CopyOnWriteArrayList<>();

    private ClientSideCaching(CacheAccessor<K, V> cacheAccessor, RedisCache<K, V> redisCache) {
        this.cacheAccessor = cacheAccessor;
        this.redisCache = redisCache;
    }

    /**
     * Enable server-assisted Client side caching for the given {@link CacheAccessor} and {@link StatefulRedisConnection}.
     * <p>
     * Note that the {@link CacheFrontend} is associated with a Redis connection. Make sure to {@link CacheFrontend#close()
     * close} the frontend object to release the Redis connection after use.
     *
     * @param cacheAccessor the accessor used to interact with the client-side cache.
     * @param connection the Redis connection to use. The connection will be associated with {@link CacheFrontend} and must be
     *        closed through {@link CacheFrontend#close()}.
     * @param tracking the tracking parameters.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the {@link CacheFrontend} for value retrieval.
     */
    public static <K, V> CacheFrontend<K, V> enable(CacheAccessor<K, V> cacheAccessor, StatefulRedisConnection<K, V> connection,
            TrackingArgs tracking) {

        connection.sync().clientTracking(tracking);

        return create(cacheAccessor, connection);
    }

    /**
     * Create a server-assisted Client side caching for the given {@link CacheAccessor} and {@link StatefulRedisConnection}.
     * This method expects that client key tracking is already configured.
     * <p>
     * Note that the {@link CacheFrontend} is associated with a Redis connection. Make sure to {@link CacheFrontend#close()
     * close} the frontend object to release the Redis connection after use.
     *
     * @param cacheAccessor the accessor used to interact with the client-side cache.
     * @param connection the Redis connection to use. The connection will be associated with {@link CacheFrontend} and must be
     *        closed through {@link CacheFrontend#close()}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the {@link CacheFrontend} for value retrieval.
     */
    public static <K, V> CacheFrontend<K, V> create(CacheAccessor<K, V> cacheAccessor,
            StatefulRedisConnection<K, V> connection) {

        StatefulRedisConnectionImpl<K, V> connectionImpl = (StatefulRedisConnectionImpl) connection;
        RedisCodec<K, V> codec = connectionImpl.getCodec();
        RedisCache<K, V> redisCache = new DefaultRedisCache<>(connection, codec);

        return create(cacheAccessor, redisCache);
    }

    /**
     * Enable server-assisted Client side caching for the given {@link CacheAccessor} and
     * {@link StatefulRedisClusterConnection}.
     * <p>
     * {@code CLIENT TRACKING} is enabled on each cluster node connection: invalidation messages for a key are emitted by the
     * node that serves the key's slot (or, for replica reads, by the replica the value was read from), so tracking must be
     * active there. Upstream (master) nodes are tracked through their write-intent connections. Replicas are tracked through
     * their read-intent connections only when the connection's {@link ReadFrom} setting can select them for reads, so no
     * replica connections are opened when reads are routed to upstream nodes only (the default). Note that a keyless
     * {@code CLIENT TRACKING} command issued through the cluster command API would be routed to the default connection only,
     * whose push messages are not associated with cluster node connections.
     * <p>
     * Client-side caching for Redis Cluster requires RESP3. {@code TrackingArgs} redirection is not supported as invalidation
     * messages can originate from any node, and {@code OPTIN} tracking is not supported as the cache frontend does not issue
     * {@code CLIENT CACHING yes} before reads; this method throws {@link IllegalArgumentException} for redirected or opt-in
     * tracking parameters and {@link IllegalStateException} if a node connection did not negotiate RESP3.
     * <p>
     * Tracking is configured for the topology and {@link ReadFrom} setting present at the time of this call. Nodes added to the
     * cluster afterwards do not have tracking enabled, and changing the read policy through
     * {@link StatefulRedisClusterConnection#setReadFrom} does not track newly selectable replicas. Applications that must
     * observe such changes should re-enable tracking afterwards.
     * <p>
     * Note that the {@link CacheFrontend} is associated with a Redis connection. Make sure to {@link CacheFrontend#close()
     * close} the frontend object to release the Redis connection after use.
     *
     * @param cacheAccessor the accessor used to interact with the client-side cache.
     * @param connection the Redis Cluster connection to use. The connection will be associated with {@link CacheFrontend} and
     *        must be closed through {@link CacheFrontend#close()}.
     * @param tracking the tracking parameters.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the {@link CacheFrontend} for value retrieval.
     * @throws IllegalArgumentException if {@code tracking} is {@code null}, disabled, redirected or opt-in.
     * @throws IllegalStateException if a node connection did not negotiate RESP3.
     * @since 7.7
     */
    public static <K, V> CacheFrontend<K, V> enable(CacheAccessor<K, V> cacheAccessor,
            StatefulRedisClusterConnection<K, V> connection, TrackingArgs tracking) {

        LettuceAssert.notNull(tracking, "TrackingArgs must not be null");
        LettuceAssert.isTrue(tracking.isEnabled(), "TrackingArgs must be enabled for Redis Cluster client-side caching");
        LettuceAssert.isTrue(!tracking.isRedirect(),
                "TrackingArgs REDIRECT is not supported for Redis Cluster client-side caching");
        LettuceAssert.isTrue(!tracking.isOptin(), "TrackingArgs OPTIN is not supported for Redis Cluster client-side caching");

        // snapshot the mutable args so reconnect replay does not observe later modifications
        TrackingArgs trackingSnapshot = tracking.copy();

        for (RedisClusterNode node : connection.getPartitions()) {
            // use host/port connections: slot-routed commands are served by connections keyed
            // by intent, host and port, not by the nodeId-keyed connections
            if (isServingUpstream(node)) {
                enableTracking(connection, node.getUri(), ConnectionIntent.WRITE, trackingSnapshot);
            }
        }

        for (RedisNodeDescription node : readCandidates(connection)) {
            // reads from upstream nodes are served by the write-intent connections tracked above
            if (node.getRole().isReplica()) {
                try {
                    enableTracking(connection, node.getUri(), ConnectionIntent.READ, trackingSnapshot);
                } catch (RedisConnectionException e) {
                    // the read path tolerates unavailable replicas as long as another candidate connects,
                    // so an unreachable replica must not fail cache setup
                    LOG.warn("Cannot enable key tracking on replica {}, reads from this replica are not tracked", node.getUri(),
                            e);
                }
            }
        }

        return create(cacheAccessor, connection);
    }

    private static Collection<RedisNodeDescription> readCandidates(StatefulRedisClusterConnection<?, ?> connection) {

        ReadFrom readFrom = connection.getReadFrom();

        if (readFrom == null) {
            // without a ReadFrom setting, all reads are routed to upstream nodes
            return Collections.emptyList();
        }

        // mirror the per-slot selection of the cluster connection provider: ReadFrom sees an
        // upstream node and its replicas, not the whole cluster
        Set<RedisNodeDescription> candidates = new LinkedHashSet<>();

        for (RedisClusterNode upstream : connection.getPartitions()) {
            if (isServingUpstream(upstream)) {
                candidates.addAll(readFrom.select(slotGroup(connection, upstream)));
            }
        }

        return candidates;
    }

    private static boolean isServingUpstream(RedisClusterNode node) {
        // upstream nodes without slots cannot serve slot-routed reads or writes
        return node.is(RedisClusterNode.NodeFlag.UPSTREAM) && !node.hasNoSlots();
    }

    private static boolean isReadCandidate(RedisClusterNode upstream, RedisClusterNode node) {

        if (upstream.getNodeId().equals(node.getNodeId())) {
            return true;
        }

        // consider only replicas that contain data from replication, mirroring the connection provider
        return upstream.getNodeId().equals(node.getSlaveOf()) && node.getReplOffset() != 0;
    }

    private static ReadFrom.Nodes slotGroup(StatefulRedisClusterConnection<?, ?> connection, RedisClusterNode upstream) {

        // preserve the partition order, mirroring PooledClusterConnectionProvider#getReadCandidates
        List<RedisNodeDescription> nodes = new ArrayList<>();

        for (RedisClusterNode node : connection.getPartitions()) {
            if (isReadCandidate(upstream, node)) {
                nodes.add(node);
            }
        }

        return new ReadFrom.Nodes() {

            @Override
            public List<RedisNodeDescription> getNodes() {
                return nodes;
            }

            @Override
            public Iterator<RedisNodeDescription> iterator() {
                return nodes.iterator();
            }

        };
    }

    private static <K, V> void enableTracking(StatefulRedisClusterConnection<K, V> connection, RedisURI uri,
            ConnectionIntent intent, TrackingArgs tracking) {

        StatefulRedisConnection<K, V> nodeConnection = connection.getConnection(uri.getHost(), uri.getPort(), intent);
        StatefulRedisConnectionImpl<K, V> nodeConnectionImpl = (StatefulRedisConnectionImpl<K, V>) nodeConnection;

        ProtocolVersion protocolVersion = nodeConnectionImpl.getConnectionState().getNegotiatedProtocolVersion();
        LettuceAssert.assertState(protocolVersion == ProtocolVersion.RESP3,
                "Client-side caching for Redis Cluster requires RESP3");

        nodeConnection.sync().clientTracking(tracking);

        // CLIENT TRACKING is connection state that the reconnect handshake does not restore, re-apply it
        nodeConnectionImpl.addListener(new RedisConnectionStateListener() {

            @Override
            public void onRedisConnected(RedisChannelHandler<?, ?> connectionHandler, SocketAddress socketAddress) {
                nodeConnection.async().clientTracking(tracking);
            }

        });
    }

    /**
     * Create a server-assisted Client side caching for the given {@link CacheAccessor} and
     * {@link StatefulRedisClusterConnection}. This method expects that client key tracking is already configured on the cluster
     * node connections.
     * <p>
     * Note that the {@link CacheFrontend} is associated with a Redis connection. Make sure to {@link CacheFrontend#close()
     * close} the frontend object to release the Redis connection after use.
     *
     * @param cacheAccessor the accessor used to interact with the client-side cache.
     * @param connection the Redis Cluster connection to use. The connection will be associated with {@link CacheFrontend} and
     *        must be closed through {@link CacheFrontend#close()}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the {@link CacheFrontend} for value retrieval.
     * @since 7.7
     */
    public static <K, V> CacheFrontend<K, V> create(CacheAccessor<K, V> cacheAccessor,
            StatefulRedisClusterConnection<K, V> connection) {

        StatefulRedisClusterConnectionImpl<K, V> connectionImpl = (StatefulRedisClusterConnectionImpl) connection;
        RedisCodec<K, V> codec = connectionImpl.getCodec();
        RedisCache<K, V> redisCache = new ClusterRedisCache<>(connection, codec);

        return create(cacheAccessor, redisCache);
    }

    private static <K, V> CacheFrontend<K, V> create(CacheAccessor<K, V> cacheAccessor, RedisCache<K, V> redisCache) {

        ClientSideCaching<K, V> caching = new ClientSideCaching<>(cacheAccessor, redisCache);

        redisCache.addInvalidationListener(caching::notifyInvalidate);
        redisCache.addClearListener(cacheAccessor::clear);
        caching.addInvalidationListener(cacheAccessor::evict);

        return caching;
    }

    private void notifyInvalidate(K key) {

        for (java.util.function.Consumer<K> invalidationListener : invalidationListeners) {
            invalidationListener.accept(key);
        }
    }

    @Override
    public void close() {
        redisCache.close();
    }

    public void addInvalidationListener(java.util.function.Consumer<K> invalidationListener) {
        invalidationListeners.add(invalidationListener);
    }

    @Override
    public V get(K key) {

        V value = cacheAccessor.get(key);

        if (value == null) {
            value = redisCache.get(key);

            if (value != null) {
                cacheAccessor.put(key, value);
            }
        }

        return value;
    }

    @Override
    public V get(K key, Callable<V> valueLoader) {

        V value = cacheAccessor.get(key);

        if (value == null) {
            value = redisCache.get(key);

            if (value == null) {

                try {
                    value = valueLoader.call();
                } catch (Exception e) {
                    throw new ValueRetrievalException(
                            String.format("Value loader %s failed with an exception for key %s", valueLoader, key), e);
                }

                if (value == null) {
                    throw new ValueRetrievalException(
                            String.format("Value loader %s returned a null value for key %s", valueLoader, key));
                }
                redisCache.put(key, value);

                // register interest in key
                redisCache.get(key);
            }

            cacheAccessor.put(key, value);
        }

        return value;
    }

}
