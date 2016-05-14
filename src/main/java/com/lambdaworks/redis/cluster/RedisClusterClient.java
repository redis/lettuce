package com.lambdaworks.redis.cluster;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.NodeSelectionSupport;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.cluster.event.ClusterTopologyChangedEvent;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.cluster.topology.ClusterTopologyRefresh;
import com.lambdaworks.redis.cluster.topology.NodeConnectionFactory;
import com.lambdaworks.redis.cluster.topology.TopologyComparators;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.internal.LettuceFactories;
import com.lambdaworks.redis.output.ValueStreamingChannel;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.pubsub.PubSubCommandHandler;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnectionImpl;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.SocketAddressResolver;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A scalable thread-safe <a href="http://redis.io/">Redis</a> cluster client. Multiple threads may share one connection. The
 * cluster client handles command routing based on the first key of the command and maintains a view of the cluster that is
 * available when calling the {@link #getPartitions()} method.
 *
 * <p>
 * Connections to the cluster members are opened on the first access to the cluster node and managed by the
 * {@link StatefulRedisClusterConnection}. You should not use transactional commands on cluster connections since {@code
 * MULTI}, {@code EXEC} and {@code DISCARD} have no key and cannot be assigned to a particular node.
 * </p>
 * <p>
 * The Redis cluster client provides a {@link RedisAdvancedClusterCommands sync}, {@link RedisAdvancedClusterAsyncCommands
 * async} and {@link com.lambdaworks.redis.cluster.api.rx.RedisAdvancedClusterReactiveCommands reactive} API.
 * </p>
 *
 * <p>
 * Connections to particular nodes can be obtained by {@link StatefulRedisClusterConnection#getConnection(String)} providing the
 * node id or {@link StatefulRedisClusterConnection#getConnection(String, int)} by host and port.
 * </p>
 *
 * <p>
 * <a href="http://redis.io/topics/cluster-spec#multiple-keys-operations">Multiple keys operations</a> have to operate on a key
 * that hashes to the same slot. Following commands do not need to follow that rule since they are pipelined according to its
 * hash value to multiple nodes in parallel on the sync, async and, reactive API:
 * </p>
 * <ul>
 * <li>{@link RedisAdvancedClusterAsyncCommands#del(Object[]) DEL}</li>
 * <li>{@link RedisAdvancedClusterAsyncCommands#unlink(Object[]) UNLINK}</li>
 * <li>{@link RedisAdvancedClusterAsyncCommands#mget(Object[]) MGET}</li>
 * <li>{@link RedisAdvancedClusterAsyncCommands#mget(ValueStreamingChannel, Object[]) MGET with streaming}</li>
 * <li>{@link RedisAdvancedClusterAsyncCommands#mset(Map) MSET}</li>
 * <li>{@link RedisAdvancedClusterAsyncCommands#msetnx(Map) MSETNX}</li>
 * </ul>
 *
 * <p>
 * Following commands on the Cluster sync, async and, reactive API are implemented with a Cluster-flavor:
 * </p>
 * <ul>
 * <li>{@link RedisAdvancedClusterAsyncCommands#clientSetname(Object)} Executes {@code CLIENT SET} on all connections and
 * initializes new connections with the {@code clientName}.</li>
 * <li>{@link RedisAdvancedClusterAsyncCommands#flushall()} Run {@code FLUSHALL} on all master nodes.</li>
 * <li>{@link RedisAdvancedClusterAsyncCommands#flushdb()} Executes {@code FLUSHDB} on all master nodes.</li>
 * <li>{@link RedisAdvancedClusterAsyncCommands#keys(Object)} Executes {@code
 * KEYS} on all.</li>
 * <li>{@link RedisAdvancedClusterAsyncCommands#randomkey()} Returns a random key from a random master node.</li>
 * <li>{@link RedisAdvancedClusterAsyncCommands#scriptFlush()} Executes {@code SCRIPT FLUSH} on all nodes.</li>
 * <li>{@link RedisAdvancedClusterAsyncCommands#scriptKill()} Executes {@code SCRIPT KILL} on all nodes.</li>
 * <li>{@link RedisAdvancedClusterAsyncCommands#shutdown(boolean)} Executes {@code SHUTDOWN} on all nodes.</li>
 * <li>{@link RedisAdvancedClusterAsyncCommands#scan()} Executes a {@code SCAN} on all nodes according to {@link ReadFrom}. The
 * resulting cursor must be reused across the {@code SCAN} to scan iteratively across the whole cluster.</li>
 * </ul>
 *
 * <p>
 * Cluster commands can be issued to multiple hosts in parallel by using the {@link NodeSelectionSupport} API. A set of nodes is
 * selected using a {@link java.util.function.Predicate} and commands can be issued to the node selection
 *
 * <code><pre>
 * AsyncExecutions<String> ping = commands.masters().commands().ping();
 * Collection<RedisClusterNode> nodes = ping.nodes();
 * nodes.stream().forEach(redisClusterNode -&gt; ping.get(redisClusterNode));
 * </pre></code>
 * </p>
 *
 * {@link RedisClusterClient} is an expensive resource. Reuse this instance or the {@link ClientResources} as much as possible.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class RedisClusterClient extends AbstractRedisClient {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisClusterClient.class);

    protected AtomicBoolean clusterTopologyRefreshActivated = new AtomicBoolean(false);

    private final ClusterTopologyRefresh refresh = new ClusterTopologyRefresh(new NodeConnectionFactoryImpl(), getResources());
    private final ClusterTopologyRefreshScheduler clusterTopologyRefreshScheduler = new ClusterTopologyRefreshScheduler(this,
            getResources());

    private Partitions partitions;
    private Iterable<RedisURI> initialUris = Collections.emptySet();

    private RedisClusterClient() {
        setOptions(ClusterClientOptions.create());
    }

    /**
     * Initialize the client with an initial cluster URI.
     *
     * @param initialUri initial cluster URI
     * @deprecated Use {@link #create(RedisURI)}
     */
    @Deprecated
    public RedisClusterClient(RedisURI initialUri) {
        this(Collections.singletonList(assertNotNull(initialUri)));
    }

    /**
     * Initialize the client with a list of cluster URI's. All uris are tried in sequence for connecting initially to the
     * cluster. If any uri is successful for connection, the others are not tried anymore. The initial uri is needed to discover
     * the cluster structure for distributing the requests.
     *
     * @param redisURIs iterable of initial {@link RedisURI cluster URIs}. Must not be {@literal null} and not empty.
     * @deprecated Use {@link #create(Iterable)}
     */
    @Deprecated
    public RedisClusterClient(List<RedisURI> redisURIs) {
        this(null, redisURIs);
    }

    /**
     * Initialize the client with a list of cluster URI's. All uris are tried in sequence for connecting initially to the
     * cluster. If any uri is successful for connection, the others are not tried anymore. The initial uri is needed to discover
     * the cluster structure for distributing the requests.
     *
     * @param clientResources the client resources. If {@literal null}, the client will create a new dedicated instance of
     *        client resources and keep track of them.
     * @param redisURIs iterable of initial {@link RedisURI cluster URIs}. Must not be {@literal null} and not empty.
     */
    protected RedisClusterClient(ClientResources clientResources, Iterable<RedisURI> redisURIs) {
        super(clientResources);
        assertNotEmpty(redisURIs);
        assertSameOptions(redisURIs);

        this.initialUris = redisURIs;

        setDefaultTimeout(getFirstUri().getTimeout(), getFirstUri().getUnit());
        setOptions(new ClusterClientOptions.Builder().build());
    }

    private static void assertSameOptions(Iterable<RedisURI> redisURIs) {

        Boolean ssl = null;
        Boolean startTls = null;
        Boolean verifyPeer = null;

        for (RedisURI redisURI : redisURIs) {

            if (ssl == null) {
                ssl = redisURI.isSsl();
            }
            if (startTls == null) {
                startTls = redisURI.isStartTls();
            }
            if (verifyPeer == null) {
                verifyPeer = redisURI.isVerifyPeer();
            }

            if (ssl.booleanValue() != redisURI.isSsl()) {
                throw new IllegalArgumentException(
                        "RedisURI " + redisURI + " SSL is not consistent with the other seed URI SSL settings");
            }

            if (startTls.booleanValue() != redisURI.isStartTls()) {
                throw new IllegalArgumentException(
                        "RedisURI " + redisURI + " StartTLS is not consistent with the other seed URI StartTLS settings");
            }

            if (verifyPeer.booleanValue() != redisURI.isVerifyPeer()) {
                throw new IllegalArgumentException(
                        "RedisURI " + redisURI + " VerifyPeer is not consistent with the other seed URI VerifyPeer settings");
            }
        }
    }

    /**
     * Create a new client that connects to the supplied {@link RedisURI uri} with default {@link ClientResources}. You can
     * connect to different Redis servers but you must supply a {@link RedisURI} on connecting.
     *
     * @param redisURI the Redis URI, must not be {@literal null}
     * @return a new instance of {@link RedisClusterClient}
     */
    public static RedisClusterClient create(RedisURI redisURI) {
        assertNotNull(redisURI);
        return create(Collections.singleton(redisURI));
    }

    /**
     * Create a new client that connects to the supplied {@link RedisURI uri} with default {@link ClientResources}. You can
     * connect to different Redis servers but you must supply a {@link RedisURI} on connecting.
     *
     * @param redisURIs one or more Redis URI, must not be {@literal null} and not empty
     * @return a new instance of {@link RedisClusterClient}
     */
    public static RedisClusterClient create(Iterable<RedisURI> redisURIs) {
        assertNotEmpty(redisURIs);
        assertSameOptions(redisURIs);
        return new RedisClusterClient(null, redisURIs);
    }

    /**
     * Create a new client that connects to the supplied uri with default {@link ClientResources}. You can connect to different
     * Redis servers but you must supply a {@link RedisURI} on connecting.
     *
     * @param uri the Redis URI, must not be {@literal null}
     * @return a new instance of {@link RedisClusterClient}
     */
    public static RedisClusterClient create(String uri) {
        LettuceAssert.notNull(uri, "uri must not be null");
        return create(RedisURI.create(uri));
    }

    /**
     * Create a new client that connects to the supplied {@link RedisURI uri} with shared {@link ClientResources}. You need to
     * shut down the {@link ClientResources} upon shutting down your application.You can connect to different Redis servers but
     * you must supply a {@link RedisURI} on connecting.
     *
     * @param clientResources the client resources, must not be {@literal null}
     * @param redisURI the Redis URI, must not be {@literal null}
     * @return a new instance of {@link RedisClusterClient}
     */
    public static RedisClusterClient create(ClientResources clientResources, RedisURI redisURI) {
        assertNotNull(clientResources);
        assertNotNull(redisURI);
        return create(clientResources, Collections.singleton(redisURI));
    }

    /**
     * Create a new client that connects to the supplied uri with shared {@link ClientResources}.You need to shut down the
     * {@link ClientResources} upon shutting down your application. You can connect to different Redis servers but you must
     * supply a {@link RedisURI} on connecting.
     *
     * @param clientResources the client resources, must not be {@literal null}
     * @param uri the Redis URI, must not be {@literal null}
     * @return a new instance of {@link RedisClusterClient}
     */
    public static RedisClusterClient create(ClientResources clientResources, String uri) {
        assertNotNull(clientResources);
        LettuceAssert.notNull(uri, "uri must not be null");
        return create(clientResources, RedisURI.create(uri));
    }

    /**
     * Create a new client that connects to the supplied {@link RedisURI uri} with shared {@link ClientResources}. You need to
     * shut down the {@link ClientResources} upon shutting down your application.You can connect to different Redis servers but
     * you must supply a {@link RedisURI} on connecting.
     *
     * @param clientResources the client resources, must not be {@literal null}
     * @param redisURIs one or more Redis URI, must not be {@literal null} and not empty
     * @return a new instance of {@link RedisClusterClient}
     */
    public static RedisClusterClient create(ClientResources clientResources, Iterable<RedisURI> redisURIs) {
        assertNotNull(clientResources);
        assertNotEmpty(redisURIs);
        assertSameOptions(redisURIs);
        return new RedisClusterClient(clientResources, redisURIs);
    }

    /**
     * Connect to a Redis Cluster and treat keys and values as UTF-8 strings.
     * <p>
     * What to expect from this connection:
     * </p>
     * <ul>
     * <li>A <i>default</i> connection is created to the node with the lowest latency</li>
     * <li>Keyless commands are send to the default connection</li>
     * <li>Single-key keyspace commands are routed to the appropriate node</li>
     * <li>Multi-key keyspace commands require the same slot-hash and are routed to the appropriate node</li>
     * <li>Pub/sub commands are sent to the node that handles the slot derived from the pub/sub channel</li>
     * </ul>
     *
     * @return A new stateful Redis Cluster connection
     */
    public StatefulRedisClusterConnection<String, String> connect() {
        return connect(newStringStringCodec());
    }

    /**
     * Connect to a Redis Cluster. Use the supplied {@link RedisCodec codec} to encode/decode keys and values.
     * <p>
     * What to expect from this connection:
     * </p>
     * <ul>
     * <li>A <i>default</i> connection is created to the node with the lowest latency</li>
     * <li>Keyless commands are send to the default connection</li>
     * <li>Single-key keyspace commands are routed to the appropriate node</li>
     * <li>Multi-key keyspace commands require the same slot-hash and are routed to the appropriate node</li>
     * <li>Pub/sub commands are sent to the node that handles the slot derived from the pub/sub channel</li>
     * </ul>
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful Redis Cluster connection
     */
    @SuppressWarnings("unchecked")
    public <K, V> StatefulRedisClusterConnection<K, V> connect(RedisCodec<K, V> codec) {
        return connectClusterImpl(codec);
    }

    /**
     * Connect to a Redis Cluster using pub/sub connections and treat keys and values as UTF-8 strings.
     * <p>
     * What to expect from this connection:
     * </p>
     * <ul>
     * <li>A <i>default</i> connection is created to the node with the least number of clients</li>
     * <li>Pub/sub commands are sent to the node with the least number of clients</li>
     * <li>Keyless commands are send to the default connection</li>
     * <li>Single-key keyspace commands are routed to the appropriate node</li>
     * <li>Multi-key keyspace commands require the same slot-hash and are routed to the appropriate node</li>
     * </ul>
     *
     * @return A new stateful Redis Cluster connection
     */
    public StatefulRedisPubSubConnection<String, String> connectPubSub() {
        return connectPubSub(newStringStringCodec());
    }

    /**
     * Connect to a Redis Cluster using pub/sub connections. Use the supplied {@link RedisCodec codec} to encode/decode keys and
     * values.
     * <p>
     * What to expect from this connection:
     * </p>
     * <ul>
     * <li>A <i>default</i> connection is created to the node with the least number of clients</li>
     * <li>Pub/sub commands are sent to the node with the least number of clients</li>
     * <li>Keyless commands are send to the default connection</li>
     * <li>Single-key keyspace commands are routed to the appropriate node</li>
     * <li>Multi-key keyspace commands require the same slot-hash and are routed to the appropriate node</li>
     * </ul>
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful Redis Cluster connection
     */
    @SuppressWarnings("unchecked")
    public <K, V> StatefulRedisPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec) {
        return connectClusterPubSubImpl(codec);
    }

    /**
     * Open a new synchronous connection to a Redis Cluster that treats keys and values as UTF-8 strings.
     *
     * @return A new connection
     * @deprecated Use {@code connect().sync()}
     */
    @Deprecated
    public RedisAdvancedClusterCommands<String, String> connectCluster() {
        return connectCluster(newStringStringCodec());
    }

    /**
     * Open a new synchronous connection to a Redis Cluster. Use the supplied {@link RedisCodec codec} to encode/decode keys and
     * values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new connection
     * @deprecated @deprecated Use {@code connect(codec).sync()}
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public <K, V> RedisAdvancedClusterCommands<K, V> connectCluster(RedisCodec<K, V> codec) {
        return connectClusterImpl(codec).sync();
    }

    /**
     * Open a new asynchronous connection to a Redis Cluster that treats keys and values as UTF-8 strings.
     *
     * @return A new connection
     * @deprecated Use {@code connect().async()}
     */
    @Deprecated
    public RedisAdvancedClusterAsyncCommands<String, String> connectClusterAsync() {
        return connectClusterImpl(newStringStringCodec()).async();
    }

    /**
     * Open a new asynchronous connection to a Redis Cluster. Use the supplied {@link RedisCodec codec} to encode/decode keys
     * and values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new connection
     * @deprecated @deprecated Use {@code connect(codec).async()}
     */
    @Deprecated
    public <K, V> RedisAdvancedClusterAsyncCommands<K, V> connectClusterAsync(RedisCodec<K, V> codec) {
        return connectClusterImpl(codec).async();
    }

    protected StatefulRedisConnection<String, String> connectToNode(final SocketAddress socketAddress) {
        return connectToNode(newStringStringCodec(), socketAddress.toString(), null, new Supplier<SocketAddress>() {
            @Override
            public SocketAddress get() {
                return socketAddress;
            }
        });
    }

    /**
     * Create a connection to a redis socket address.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param nodeId the nodeId
     * @param clusterWriter global cluster writer
     * @param socketAddressSupplier supplier for the socket address
     * @param <K> Key type
     * @param <V> Value type
     * @return A new connection
     */
    <K, V> StatefulRedisConnection<K, V> connectToNode(RedisCodec<K, V> codec, String nodeId,
            RedisChannelWriter<K, V> clusterWriter, final Supplier<SocketAddress> socketAddressSupplier) {

        assertNotNull(codec);
        assertNotEmpty(initialUris);

        LettuceAssert.notNull(socketAddressSupplier, "SocketAddressSupplier must not be null");

        logger.debug("connectNode(" + nodeId + ")");
        Queue<RedisCommand<K, V, ?>> queue = LettuceFactories.newConcurrentQueue();

        ClusterNodeCommandHandler<K, V> handler = new ClusterNodeCommandHandler<K, V>(clientOptions, getResources(), queue,
                clusterWriter);
        StatefulRedisConnectionImpl<K, V> connection = new StatefulRedisConnectionImpl<K, V>(handler, codec, timeout, unit);

        try {
            connectStateful(handler, connection, getFirstUri(), socketAddressSupplier);

            connection.registerCloseables(closeableResources, connection);
        } catch (RedisCommandExecutionException e) {
            connection.close();
            throw e;
        }

        return connection;
    }

    /**
     * Create a clustered pub/sub connection with command distributor.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return a new connection
     */
    <K, V> StatefulRedisClusterConnectionImpl<K, V> connectClusterImpl(RedisCodec<K, V> codec) {

        if (partitions == null) {
            initializePartitions();
        }

        activateTopologyRefreshIfNeeded();

        logger.debug("connectCluster(" + initialUris + ")");
        Queue<RedisCommand<K, V, ?>> queue = LettuceFactories.newConcurrentQueue();

        Supplier<SocketAddress> socketAddressSupplier = getSocketAddressSupplier(TopologyComparators::sortByLatency);

        CommandHandler<K, V> handler = new CommandHandler<K, V>(clientOptions, clientResources, queue);

        ClusterDistributionChannelWriter<K, V> clusterWriter = new ClusterDistributionChannelWriter<K, V>(clientOptions,
                handler, clusterTopologyRefreshScheduler);
        PooledClusterConnectionProvider<K, V> pooledClusterConnectionProvider = new PooledClusterConnectionProvider<K, V>(this,
                clusterWriter, codec);

        clusterWriter.setClusterConnectionProvider(pooledClusterConnectionProvider);

        StatefulRedisClusterConnectionImpl<K, V> connection = new StatefulRedisClusterConnectionImpl<>(clusterWriter, codec,
                timeout, unit);

        connection.setReadFrom(ReadFrom.MASTER);
        connection.setPartitions(partitions);

        boolean connected = false;
        RedisException causingException = null;
        int connectionAttempts = Math.max(1, partitions.size());

        for (int i = 0; i < connectionAttempts; i++) {
            try {
                connectStateful(handler, connection, getFirstUri(), socketAddressSupplier);
                connected = true;
                break;
            } catch (RedisException e) {
                logger.warn(e.getMessage());
                causingException = e;
            }
        }

        if (!connected) {
            connection.close();
            if (causingException != null) {
                throw causingException;
            }
        }

        connection.registerCloseables(closeableResources, connection, clusterWriter, pooledClusterConnectionProvider);

        return connection;
    }

    /**
     * Create a clustered connection with command distributor.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return a new connection
     */
    <K, V> StatefulRedisPubSubConnectionImpl<K, V> connectClusterPubSubImpl(RedisCodec<K, V> codec) {

        if (partitions == null) {
            initializePartitions();
        }

        activateTopologyRefreshIfNeeded();

        logger.debug("connectClusterPubSub(" + initialUris + ")");
        Queue<RedisCommand<K, V, ?>> queue = LettuceFactories.newConcurrentQueue();

        Supplier<SocketAddress> socketAddressSupplier = getSocketAddressSupplier(TopologyComparators::sortByClientCount);

        PubSubCommandHandler<K, V> handler = new PubSubCommandHandler<K, V>(clientOptions, clientResources, queue, codec);

        ClusterDistributionChannelWriter<K, V> clusterWriter = new ClusterDistributionChannelWriter<K, V>(clientOptions,
                handler, clusterTopologyRefreshScheduler);
        PooledClusterConnectionProvider<K, V> pooledClusterConnectionProvider = new PooledClusterConnectionProvider<K, V>(this,
                clusterWriter, codec);

        clusterWriter.setClusterConnectionProvider(pooledClusterConnectionProvider);

        StatefulRedisPubSubConnectionImpl<K, V> connection = new StatefulRedisPubSubConnectionImpl<>(clusterWriter, codec,
                timeout, unit);

        clusterWriter.setPartitions(partitions);

        boolean connected = false;
        RedisException causingException = null;
        int connectionAttempts = Math.max(1, partitions.size());

        for (int i = 0; i < connectionAttempts; i++) {
            try {
                connectStateful(handler, connection, getFirstUri(), socketAddressSupplier);
                connected = true;
                break;
            } catch (RedisException e) {
                logger.warn(e.getMessage());
                causingException = e;
            }
        }

        if (!connected) {
            connection.close();
            throw causingException;
        }

        connection.registerCloseables(closeableResources, connection, clusterWriter, pooledClusterConnectionProvider);

        if (getFirstUri().getPassword() != null) {
            connection.async().auth(new String(getFirstUri().getPassword()));
        }

        return connection;
    }

    /**
     * Connect to a endpoint provided by {@code socketAddressSupplier} using connection settings (authentication, SSL) from
     * {@code connectionSettings}.
     *
     * @param handler
     * @param connection
     * @param connectionSettings
     * @param socketAddressSupplier
     * @param <K>
     * @param <V>
     */
    private <K, V> void connectStateful(CommandHandler<K, V> handler, StatefulRedisConnectionImpl<K, V> connection,
            RedisURI connectionSettings, Supplier<SocketAddress> socketAddressSupplier) {

        connectStateful0(handler, connection, connectionSettings, socketAddressSupplier);

        if (connectionSettings.getPassword() != null && connectionSettings.getPassword().length != 0) {
            connection.async().auth(new String(connectionSettings.getPassword()));
        }
    }

    /**
     * Connect to a endpoint provided by {@code socketAddressSupplier} using connection settings (authentication, SSL) from
     * {@code connectionSettings}.
     *
     * @param handler
     * @param connection
     * @param connectionSettings
     * @param socketAddressSupplier
     * @param <K>
     * @param <V>
     */
    private <K, V> void connectStateful(CommandHandler<K, V> handler, StatefulRedisClusterConnectionImpl<K, V> connection,
            RedisURI connectionSettings, Supplier<SocketAddress> socketAddressSupplier) {

        connectStateful0(handler, connection, connectionSettings, socketAddressSupplier);

        if (connectionSettings.getPassword() != null && connectionSettings.getPassword().length != 0) {
            connection.async().auth(new String(connectionSettings.getPassword()));
        }
    }

    /**
     * Connect to a endpoint provided by {@code socketAddressSupplier} using connection settings (SSL) from {@code
     * connectionSettings}.
     *
     * @param handler
     * @param connection
     * @param connectionSettings
     * @param socketAddressSupplier
     * @param <K>
     * @param <V>
     */
    private <K, V> void connectStateful0(CommandHandler<K, V> handler, RedisChannelHandler<K, V> connection,
            RedisURI connectionSettings, Supplier<SocketAddress> socketAddressSupplier) {

        ConnectionBuilder connectionBuilder;
        if (connectionSettings.isSsl()) {
            SslConnectionBuilder sslConnectionBuilder = SslConnectionBuilder.sslConnectionBuilder();
            sslConnectionBuilder.ssl(connectionSettings);
            connectionBuilder = sslConnectionBuilder;
        } else {
            connectionBuilder = ConnectionBuilder.connectionBuilder();
        }

        connectionBuilder.reconnectionListener(new ReconnectEventListener(clusterTopologyRefreshScheduler));
        connectionBuilder.clientOptions(clientOptions);
        connectionBuilder.clientResources(clientResources);
        connectionBuilder(handler, connection, socketAddressSupplier, connectionBuilder, connectionSettings);
        channelType(connectionBuilder, connectionSettings);

        initializeChannel(connectionBuilder);
    }

    /**
     * Reload partitions and re-initialize the distribution table.
     */
    public void reloadPartitions() {

        if (partitions == null) {
            initializePartitions();
            partitions.updateCache();
        } else {

            Partitions loadedPartitions = loadPartitions();
            if (TopologyComparators.isChanged(getPartitions(), loadedPartitions)) {

                logger.debug("Using a new cluster topology");

                List<RedisClusterNode> before = new ArrayList<RedisClusterNode>(getPartitions());
                List<RedisClusterNode> after = new ArrayList<RedisClusterNode>(loadedPartitions);

                getResources().eventBus().publish(new ClusterTopologyChangedEvent(before, after));
            }

            this.partitions.getPartitions().clear();
            this.partitions.getPartitions().addAll(loadedPartitions.getPartitions());
            this.partitions.reload(loadedPartitions.getPartitions());
        }

        updatePartitionsInConnections();
    }

    protected void updatePartitionsInConnections() {

        forEachClusterConnection(input -> {
            input.setPartitions(partitions);
        });
    }

    protected void initializePartitions() {

        Partitions loadedPartitions = loadPartitions();
        this.partitions = loadedPartitions;
    }

    /**
     * Retrieve the cluster view. Partitions are shared amongst all connections opened by this client instance.
     *
     * @return the partitions.
     */
    public Partitions getPartitions() {
        if (partitions == null) {
            initializePartitions();
        }
        return partitions;
    }

    /**
     * Retrieve partitions. Nodes within {@link Partitions} are ordered by latency. Lower latency nodes come first.
     *
     * @return Partitions
     */
    protected Partitions loadPartitions() {

        Iterable<RedisURI> topologyRefreshSource = getTopologyRefreshSource();
        Map<RedisURI, Partitions> partitions = refresh.loadViews(topologyRefreshSource, useDynamicRefreshSources());

        if (partitions.isEmpty()) {
            throw new RedisException("Cannot retrieve initial cluster partitions from initial URIs " + topologyRefreshSource);
        }

        Partitions loadedPartitions = partitions.values().iterator().next();
        RedisURI viewedBy = refresh.getViewedBy(partitions, loadedPartitions);

        for (RedisClusterNode partition : loadedPartitions) {
            if (viewedBy != null) {
                RedisURI uri = partition.getUri();
                applyUriConnectionSettings(viewedBy, uri);
            }
        }

        activateTopologyRefreshIfNeeded();

        return loadedPartitions;
    }

    private void activateTopologyRefreshIfNeeded() {

        if (getOptions() instanceof ClusterClientOptions) {
            ClusterClientOptions options = (ClusterClientOptions) getOptions();
            ClusterTopologyRefreshOptions topologyRefreshOptions = options.getTopologyRefreshOptions();

            if (!topologyRefreshOptions.isPeriodicRefreshEnabled()
                    || clusterTopologyRefreshActivated.get()) {
                return;
            }

            if (clusterTopologyRefreshActivated.compareAndSet(false, true)) {
                genericWorkerPool.scheduleAtFixedRate(clusterTopologyRefreshScheduler, options.getRefreshPeriod(),
                        options.getRefreshPeriod(), options.getRefreshPeriodUnit());
            }
        }
    }

    protected RedisURI getFirstUri() {
        assertNotEmpty(initialUris);
        Iterator<RedisURI> iterator = initialUris.iterator();
        return iterator.next();
    }

    private Supplier<SocketAddress> getSocketAddressSupplier(
            Function<Collection<RedisClusterNode>, Collection<RedisClusterNode>> sortFunction) {

        final RoundRobinSocketAddressSupplier socketAddressSupplier = new RoundRobinSocketAddressSupplier(partitions,
                sortFunction, clientResources);
        return () -> {
            if (partitions.isEmpty()) {
                SocketAddress socketAddress = SocketAddressResolver.resolve(getFirstUri(), clientResources.dnsResolver());
                logger.debug("Resolved SocketAddress {} using {}", socketAddress, getFirstUri());
                return socketAddress;
            }

            return socketAddressSupplier.get();
        };
    }

    protected Utf8StringCodec newStringStringCodec() {
        return new Utf8StringCodec();
    }

    /**
     * Sets the new cluster topology. The partitions are not applied to existing connections.
     *
     * @param partitions partitions object
     */
    public void setPartitions(Partitions partitions) {
        this.partitions = partitions;
    }

    /**
     * Returns the {@link ClientResources} which are used with that client.
     *
     * @return the {@link ClientResources} for this client
     */
    public ClientResources getResources() {
        return clientResources;
    }

    protected void forEachClusterConnection(Consumer<StatefulRedisClusterConnectionImpl<?, ?>> function) {
        forEachCloseable(input -> input instanceof StatefulRedisClusterConnectionImpl, function);
    }

    protected <T extends Closeable> void forEachCloseable(Predicate<? super Closeable> selector, Consumer<T> function) {
        for (Closeable c : closeableResources) {
            if (selector.test(c)) {
                function.accept((T) c);
            }
        }
    }

    /**
     * Set the {@link ClusterClientOptions} for the client.
     *
     * @param clientOptions client options for the client and connections that are created after setting the options
     */
    public void setOptions(ClusterClientOptions clientOptions) {
        super.setOptions(clientOptions);
    }

    ClusterClientOptions getClusterClientOptions() {
        if (getOptions() instanceof ClusterClientOptions) {
            return (ClusterClientOptions) getOptions();
        }
        return null;
    }

    boolean expireStaleConnections() {
        return getClusterClientOptions() == null || getClusterClientOptions().isCloseStaleConnections();
    }

    static void applyUriConnectionSettings(RedisURI from, RedisURI to) {

        if (from.getPassword() != null && from.getPassword().length != 0) {
            to.setPassword(new String(from.getPassword()));
        }

        to.setTimeout(from.getTimeout());
        to.setUnit(from.getUnit());
        to.setSsl(from.isSsl());
        to.setStartTls(from.isStartTls());
        to.setVerifyPeer(from.isVerifyPeer());
    }

    private static <K, V> void assertNotNull(RedisCodec<K, V> codec) {
        LettuceAssert.notNull(codec, "RedisCodec must not be null");
    }

    private static void assertNotEmpty(Iterable<RedisURI> redisURIs) {
        LettuceAssert.notNull(redisURIs, "RedisURIs must not be null");
        LettuceAssert.isTrue(redisURIs.iterator().hasNext(), "RedisURIs must not be empty");
    }

    private static RedisURI assertNotNull(RedisURI redisURI) {
        LettuceAssert.notNull(redisURI, "RedisURI must not be null");
        return redisURI;
    }

    private static void assertNotNull(ClientResources clientResources) {
        LettuceAssert.notNull(clientResources, "ClientResources must not be null");
    }

    protected Iterable<RedisURI> getTopologyRefreshSource() {

        boolean initialSeedNodes = !useDynamicRefreshSources();

        Iterable<RedisURI> seed;
        if (initialSeedNodes || partitions == null || partitions.isEmpty()) {
            seed = RedisClusterClient.this.initialUris;
        } else {
            List<RedisURI> uris = new ArrayList<>();
            for (RedisClusterNode partition : TopologyComparators.sortByUri(partitions)) {
                uris.add(partition.getUri());
            }
            seed = uris;
        }
        return seed;
    }

    protected boolean useDynamicRefreshSources() {

        if (getClusterClientOptions() != null) {
            ClusterTopologyRefreshOptions topologyRefreshOptions = getClusterClientOptions().getTopologyRefreshOptions();

            return topologyRefreshOptions.useDynamicRefreshSources();
        }
        return true;
    }

    private class NodeConnectionFactoryImpl implements NodeConnectionFactory {
        @Override
        public <K, V> StatefulRedisConnection<K, V> connectToNode(RedisCodec<K, V> codec, SocketAddress socketAddress) {
            return RedisClusterClient.this.connectToNode(codec, socketAddress.toString(), null, new Supplier<SocketAddress>() {
                @Override
                public SocketAddress get() {
                    return socketAddress;
                }
            });
        }
    }

}
