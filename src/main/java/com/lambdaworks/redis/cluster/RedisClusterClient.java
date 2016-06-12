package com.lambdaworks.redis.cluster;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.lambdaworks.redis.cluster.ClusterTopologyRefresh.RedisUriComparator.INSTANCE;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.cluster.event.ClusterTopologyChangedEvent;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.resource.ClientResources;

import com.lambdaworks.redis.resource.SocketAddressResolver;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A scalable thread-safe <a href="http://redis.io/">Redis</a> cluster client. Multiple threads may share one connection. The
 * cluster client handles command routing based on the first key of the command and maintains a view on the cluster that is
 * available when calling the {@link #getPartitions()} method.
 *
 * <p>
 * Connections to particular nodes can be obtained by {@link RedisAdvancedClusterConnection#getConnection(String)} providing the
 * node id or {@link RedisAdvancedClusterConnection#getConnection(String, int)} by host and port.
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

    private ClusterTopologyRefresh refresh = new ClusterTopologyRefresh(this);
    private Partitions partitions;
    private final Iterable<RedisURI> initialUris;

    private RedisClusterClient() {
        setOptions(ClusterClientOptions.create());
        initialUris = ImmutableSet.of();
    }

    /**
     * Initialize the client with an initial cluster URI.
     *
     * @param initialUri initial cluster URI
     * @deprecated Use {@link #create(RedisURI)}
     */
    @Deprecated
    public RedisClusterClient(RedisURI initialUri) {
        this(ImmutableList.of(checkNotNull(initialUri, "RedisURI (initial uri) must not be null")));
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
        return create(ImmutableList.of(redisURI));
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
        checkArgument(uri != null, "uri must not be null");
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
        return create(clientResources, ImmutableList.of(redisURI));
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
        checkArgument(uri != null, "uri must not be null");
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
     * Open a new synchronous connection to a Redis Cluster that treats keys and values as UTF-8 strings.
     *
     * @return A new connection.
     */
    public RedisAdvancedClusterConnection<String, String> connectCluster() {
        return connectCluster(newStringStringCodec());
    }

    /**
     * Open a new synchronous connection to a Redis Cluster. Use the supplied {@link RedisCodec codec} to encode/decode keys and
     * values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param <K> Key type.
     * @param <V> Value type.
     * @return A new connection.
     */
    @SuppressWarnings("unchecked")
    public <K, V> RedisAdvancedClusterConnection<K, V> connectCluster(RedisCodec<K, V> codec) {
        assertNotNull(codec);
        return (RedisAdvancedClusterConnection<K, V>) syncHandler(connectClusterAsyncImpl(codec),
                RedisAdvancedClusterConnection.class, RedisClusterConnection.class);
    }

    /**
     * Open a new asynchronous connection to a Redis Cluster that treats keys and values as UTF-8 strings.
     *
     * @return A new connection.
     */
    public RedisAdvancedClusterAsyncConnection<String, String> connectClusterAsync() {
        return connectClusterAsyncImpl(newStringStringCodec());
    }

    /**
     * Open a new asynchronous connection to a Redis Cluster. Use the supplied {@link RedisCodec codec} to encode/decode keys
     * and values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param <K> Key type.
     * @param <V> Value type.
     * @return A new connection.
     */
    public <K, V> RedisAdvancedClusterAsyncConnection<K, V> connectClusterAsync(RedisCodec<K, V> codec) {
        assertNotNull(codec);
        return connectClusterAsyncImpl(codec);
    }

    protected RedisAsyncConnectionImpl<String, String> connectAsyncImpl(final SocketAddress socketAddress) {
        return connectNode(newStringStringCodec(), socketAddress.toString(), null, new Supplier<SocketAddress>() {
            @Override
            public SocketAddress get() {
                return socketAddress;
            }
        });
    }

    /**
     * Create a connection to a redis socket address.
     *
     * @param codec Use this codec to encode/decode keys and values.
     * @param nodeId the nodeId
     * @param clusterWriter global cluster writer
     * @param socketAddressSupplier supplier for the socket address
     *
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new connection
     */
    <K, V> RedisAsyncConnectionImpl<K, V> connectNode(RedisCodec<K, V> codec, String nodeId,
            RedisChannelWriter<K, V> clusterWriter, final Supplier<SocketAddress> socketAddressSupplier) {

        assertNotNull(codec);
        assertNotEmpty(initialUris);

        checkArgument(socketAddressSupplier != null, "SocketAddressSupplier must not be null");

        logger.debug("connectNode(" + nodeId + ")");
        Queue<RedisCommand<K, V, ?>> queue = new ArrayDeque<RedisCommand<K, V, ?>>();

        ClusterNodeCommandHandler<K, V> handler = new ClusterNodeCommandHandler<K, V>(clientOptions, clientResources, queue,
                clusterWriter);
        RedisAsyncConnectionImpl<K, V> connection = newRedisAsyncConnectionImpl(handler, codec, timeout, unit);

        try {
            connectStateful(handler, connection, getFirstUri(), socketAddressSupplier);

            connection.registerCloseables(closeableResources, connection);
        } catch (RuntimeException e) {
            connection.close();
            throw e;
        }

        return connection;
    }

    /**
     * Create a clustered connection with command distributor.
     *
     * @param codec the codec to use
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new connection
     */
    <K, V> RedisAdvancedClusterAsyncConnectionImpl<K, V> connectClusterAsyncImpl(RedisCodec<K, V> codec) {

        if (partitions == null) {
            initializePartitions();
        }

        activateTopologyRefreshIfNeeded();

        logger.debug("connectCluster(" + initialUris + ")");
        Queue<RedisCommand<K, V, ?>> queue = new ArrayDeque<RedisCommand<K, V, ?>>();

        Supplier<SocketAddress> socketAddressSupplier = getSocketAddressSupplier(

                new Function<Collection<RedisClusterNode>, Collection<RedisClusterNode>>() {
                    public Collection<RedisClusterNode> apply(Collection<RedisClusterNode> input) {
                        return TopologyComparators.predefinedSort(input, initialUris);
                    }
                });

        CommandHandler<K, V> handler = new CommandHandler<K, V>(clientOptions, clientResources, queue);

        ClusterDistributionChannelWriter<K, V> clusterWriter = new ClusterDistributionChannelWriter<K, V>(handler);
        PooledClusterConnectionProvider<K, V> pooledClusterConnectionProvider = new PooledClusterConnectionProvider<K, V>(this,
                clusterWriter, codec);

        clusterWriter.setClusterConnectionProvider(pooledClusterConnectionProvider);

        RedisAdvancedClusterAsyncConnectionImpl<K, V> connection = newRedisAdvancedClusterAsyncConnectionImpl(clusterWriter,
                codec, timeout, unit);

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
    private <K, V> void connectStateful(CommandHandler<K, V> handler, RedisAsyncConnectionImpl<K, V> connection,
            RedisURI connectionSettings, Supplier<SocketAddress> socketAddressSupplier) {

        connectStateful0(handler, connection, connectionSettings, socketAddressSupplier);

        if (connectionSettings.getPassword() != null && connectionSettings.getPassword().length != 0) {
            connection.auth(new String(connectionSettings.getPassword()));
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
                List<RedisClusterNode> before = ImmutableList.copyOf(getPartitions());
                List<RedisClusterNode> after = ImmutableList.copyOf(loadedPartitions);

                getResources().eventBus().publish(new ClusterTopologyChangedEvent(before, after));
            }

            this.partitions.getPartitions().clear();
            this.partitions.getPartitions().addAll(loadedPartitions.getPartitions());
            this.partitions.reload(loadedPartitions.getPartitions());
        }

        updatePartitionsInConnections();
    }

    protected void updatePartitionsInConnections() {

        forEachClusterConnection(new Predicate<RedisAdvancedClusterAsyncConnectionImpl<?, ?>>() {
            @Override
            public boolean apply(RedisAdvancedClusterAsyncConnectionImpl<?, ?> input) {
                input.setPartitions(partitions);
                return true;
            }
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

        Map<RedisURI, Partitions> partitions = refresh.loadViews(initialUris);

        if (partitions.isEmpty()) {
            throw new RedisException("Cannot retrieve initial cluster partitions from initial URIs " + initialUris);
        }

        Partitions loadedPartitions = partitions.values().iterator().next();
        RedisURI viewedBy = refresh.getViewedBy(partitions, loadedPartitions);

        for (RedisClusterNode partition : loadedPartitions) {
            if (partition.getUri() != null) {
                applyUriConnectionSettings(viewedBy, partition.getUri());
            }
        }

        activateTopologyRefreshIfNeeded();

        return loadedPartitions;
    }

    private void activateTopologyRefreshIfNeeded() {
        if (getOptions() instanceof ClusterClientOptions) {
            ClusterClientOptions options = (ClusterClientOptions) getOptions();
            if (options.isRefreshClusterView()) {
                synchronized (clusterTopologyRefreshActivated) {
                    if (!clusterTopologyRefreshActivated.get()) {
                        final Runnable r = new ClusterTopologyRefreshTask();
                        genericWorkerPool.scheduleAtFixedRate(r, options.getRefreshPeriod(), options.getRefreshPeriod(),
                                options.getRefreshPeriodUnit());
                        clusterTopologyRefreshActivated.set(true);
                    }
                }
            }
        }
    }

    /**
     * Check if the {@link #genericWorkerPool} is active
     *
     * @return false if the worker pool is terminating, shutdown or terminated
     */
    protected boolean isEventLoopActive() {
        if (genericWorkerPool.isShuttingDown() || genericWorkerPool.isShutdown() || genericWorkerPool.isTerminated()) {
            return false;
        }

        return true;
    }

    /**
     * Construct a new {@link RedisAsyncConnectionImpl}. Can be overridden in order to construct a subclass of
     * {@link RedisAsyncConnectionImpl}. These connections are the "inner" connections used by the
     * {@link ClusterDistributionChannelWriter}.
     *
     * @param channelWriter the channel writer
     * @param codec the codec to use
     * @param timeout Timeout value
     * @param unit Timeout unit
     * @param <K> Key type.
     * @param <V> Value type.
     * @return RedisAsyncConnectionImpl&lt;K, V&gt; instance
     */
    protected <K, V> RedisAsyncConnectionImpl<K, V> newRedisAsyncConnectionImpl(RedisChannelWriter<K, V> channelWriter,
            RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        return new RedisAsyncConnectionImpl<K, V>(channelWriter, codec, timeout, unit);
    }

    /**
     * Construct a new {@link RedisAdvancedClusterAsyncConnectionImpl}. Can be overridden in order to construct a subclass of
     * {@link RedisAdvancedClusterAsyncConnectionImpl}
     *
     * @param channelWriter the channel writer
     * @param codec the codec to use
     * @param timeout Timeout value
     * @param unit Timeout unit
     * @param <K> Key type.
     * @param <V> Value type.
     * @return RedisAdvancedClusterAsyncConnectionImpl&lt;K, V&gt; instance
     */
    protected <K, V> RedisAdvancedClusterAsyncConnectionImpl<K, V> newRedisAdvancedClusterAsyncConnectionImpl(
            RedisChannelWriter<K, V> channelWriter, RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        return new RedisAdvancedClusterAsyncConnectionImpl<K, V>(channelWriter, codec, timeout, unit);
    }

    protected RedisURI getFirstUri() {
        assertNotEmpty(initialUris);
        Iterator<RedisURI> iterator = initialUris.iterator();
        return iterator.next();
    }

    /**
     * Returns a {@link Supplier} for {@link SocketAddress connection points}.
     *
     * @param sortFunction Sort function to enforce a specific order. The sort function must not change the order or the input
     *        parameter but create a new collection with the desired order, must not be {@literal null}.
     * @return {@link Supplier} for {@link SocketAddress connection points}.
     */
    protected Supplier<SocketAddress> getSocketAddressSupplier(
            Function<? extends Collection<RedisClusterNode>, Collection<RedisClusterNode>> sortFunction) {

        final RoundRobinSocketAddressSupplier socketAddressSupplier = new RoundRobinSocketAddressSupplier(partitions,
                sortFunction, clientResources);

        return new Supplier<SocketAddress>() {
            @Override
            public SocketAddress get() {
                if (partitions.isEmpty()) {
                SocketAddress socketAddress = SocketAddressResolver.resolve(getFirstUri(), clientResources.dnsResolver());
                logger.debug("Resolved SocketAddress {} using {}", socketAddress, getFirstUri());
                return socketAddress;
            }

                return socketAddressSupplier.get();
            }
        };
    }

    private List<RedisClusterNode> getOrderedPartitions(Iterable<RedisClusterNode> clusterNodes) {
        List<RedisClusterNode> ordered = Lists.newArrayList(clusterNodes);
        Collections.sort(ordered, new Comparator<RedisClusterNode>() {
            @Override
            public int compare(RedisClusterNode o1, RedisClusterNode o2) {
                return INSTANCE.compare(o1.getUri(), o2.getUri());
            }
        });
        return ordered;
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

    protected void forEachClusterConnection(Predicate<RedisAdvancedClusterAsyncConnectionImpl<?, ?>> function) {

        forEachCloseable(new Predicate<Closeable>() {
            @Override
            public boolean apply(Closeable input) {
                return input instanceof RedisAdvancedClusterAsyncConnectionImpl;
            }
        }, function);
    }

    @SuppressWarnings("unchecked")
    protected <T extends Closeable> void forEachCloseable(Predicate<? super Closeable> selector, Predicate<T> function) {
        for (Closeable c : closeableResources) {
            if (selector.apply(c)) {
                function.apply((T) c);
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

    private class ClusterTopologyRefreshTask implements Runnable {

        public ClusterTopologyRefreshTask() {
        }

        @Override
        public void run() {
            logger.debug("ClusterTopologyRefreshTask.run()");
            if (isEventLoopActive() && getClusterClientOptions() != null) {
                if (!getClusterClientOptions().isRefreshClusterView()) {
                    logger.debug("ClusterTopologyRefreshTask is disabled");
                    return;
                }
            } else {
                logger.debug("ClusterTopologyRefreshTask is disabled");
                return;
            }

            Iterable<RedisURI> seed;
            if (partitions == null || partitions.size() == 0) {
                seed = RedisClusterClient.this.initialUris;
            } else {
                List<RedisURI> uris = Lists.newArrayList();
                for (RedisClusterNode partition : getOrderedPartitions(partitions)) {
                    uris.add(partition.getUri());
                }
                seed = uris;
            }

            logger.debug("ClusterTopologyRefreshTask requesting partitions from {}", seed);
            Map<RedisURI, Partitions> partitions = refresh.loadViews(seed);
            List<Partitions> values = Lists.newArrayList(partitions.values());
            if (!values.isEmpty() && TopologyComparators.isChanged(getPartitions(), values.get(0))) {
                logger.debug("Using a new cluster topology");

                List<RedisClusterNode> before = ImmutableList.copyOf(getPartitions());
                List<RedisClusterNode> after = ImmutableList.copyOf(values.get(0).getPartitions());

                getResources().eventBus().publish(new ClusterTopologyChangedEvent(before, after));

                getPartitions().reload(values.get(0).getPartitions());
                updatePartitionsInConnections();

                if (isEventLoopActive() && expireStaleConnections()) {
                    genericWorkerPool.submit(new CloseStaleConnectionsTask());
                }
            }
        }
    }

    private class CloseStaleConnectionsTask implements Runnable {
        @Override
        public void run() {
            if (isEventLoopActive() && expireStaleConnections()) {

                forEachClusterConnection(new Predicate<RedisAdvancedClusterAsyncConnectionImpl<?, ?>>() {
                    @Override
                    public boolean apply(RedisAdvancedClusterAsyncConnectionImpl<?, ?> input) {

                        ClusterDistributionChannelWriter<?, ?> writer = (ClusterDistributionChannelWriter<?, ?>) input
                                .getChannelWriter();
                        writer.getClusterConnectionProvider().closeStaleConnections();
                        return true;
                    }
                });
            }
        }
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

    boolean expireStaleConnections() {
        return getClusterClientOptions() == null || getClusterClientOptions().isCloseStaleConnections();
    }

    private static <K, V> void assertNotNull(RedisCodec<K, V> codec) {
        checkArgument(codec != null, "RedisCodec must not be null");
    }

    private static void assertNotEmpty(Iterable<RedisURI> redisURIs) {
        checkArgument(redisURIs != null, "RedisURIs must not be null");
        checkArgument(redisURIs.iterator().hasNext(), "RedisURIs must not be empty");
    }

    private static void assertNotNull(RedisURI redisURI) {
        checkArgument(redisURI != null, "RedisURI must not be null");
    }

    private static void assertNotNull(ClientResources clientResources) {
        checkArgument(clientResources != null, "ClientResources must not be null");
    }
}
