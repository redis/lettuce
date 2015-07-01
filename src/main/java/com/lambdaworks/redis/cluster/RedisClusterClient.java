package com.lambdaworks.redis.cluster;

import static com.google.common.base.Preconditions.*;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.protocol.RedisCommand;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A scalable thread-safe <a href="http://redis.io/">Redis</a> cluster client. Multiple threads may share one connection
 * provided they avoid blocking and transactional operations such as BLPOP and MULTI/EXEC.
 *
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class RedisClusterClient extends AbstractRedisClient {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisClusterClient.class);

    private Partitions partitions;

    private List<RedisURI> initialUris = Lists.newArrayList();

    private RedisClusterClient() {
    }

    /**
     * Initialize the client with an initial cluster URI.
     *
     * @param initialUri initial cluster URI
     */
    public RedisClusterClient(RedisURI initialUri) {
        this(Collections.singletonList(checkNotNull(initialUri, "RedisURI (initial uri) must not be null")));
    }

    /**
     * Initialize the client with a list of cluster URI's. All uris are tried in sequence for connecting initially to the
     * cluster. If any uri is sucessful for connection, the others are not tried anymore. The initial uri is needed to discover
     * the cluster structure for distributing the requests.
     *
     * @param initialUris list of initial cluster URIs
     */
    public RedisClusterClient(List<RedisURI> initialUris) {
        this.initialUris = initialUris;
        checkNotNull(initialUris, "initialUris must not be null");
        checkArgument(!initialUris.isEmpty(), "initialUris must not be empty");

        setDefaultTimeout(getFirstUri().getTimeout(), getFirstUri().getUnit());
    }

    /**
     * Open a new synchronous connection to the redis cluster that treats keys and values as UTF-8 strings.
     *
     * @return A new connection.
     */
    public RedisAdvancedClusterConnection<String, String> connectCluster() {

        return connectCluster(newStringStringCodec());
    }

    /**
     * Open a new synchronous connection to the redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys
     * and values.
     *
     * @param codec Use this codec to encode/decode keys and values.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return A new connection.
     */
    @SuppressWarnings("unchecked")
    public <K, V> RedisAdvancedClusterConnection<K, V> connectCluster(RedisCodec<K, V> codec) {

        return (RedisAdvancedClusterConnection<K, V>) syncHandler(connectClusterAsyncImpl(codec),
                RedisAdvancedClusterConnection.class, RedisClusterConnection.class);
    }

    /**
     * Creates a connection to the redis cluster.
     *
     * @return A new connection.
     */
    public RedisAdvancedClusterAsyncConnection<String, String> connectClusterAsync() {
        return connectClusterAsyncImpl(newStringStringCodec(), getSocketAddressSupplier());
    }

    /**
     * Creates a connection to the redis cluster.
     *
     * @param codec Use this codec to encode/decode keys and values.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return A new connection.
     */
    public <K, V> RedisAdvancedClusterAsyncConnection<K, V> connectClusterAsync(RedisCodec<K, V> codec) {
        return connectClusterAsyncImpl(codec, getSocketAddressSupplier());
    }

    protected RedisAsyncConnectionImpl<String, String> connectAsyncImpl(SocketAddress socketAddress) {
        return connectAsyncImpl(newStringStringCodec(), socketAddress);
    }

    /**
     * Create a connection to a redis socket address.
     *
     * @param socketAddress initial connect
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new connection
     */
    <K, V> RedisAsyncConnectionImpl<K, V> connectAsyncImpl(RedisCodec<K, V> codec, final SocketAddress socketAddress) {

        logger.debug("connectAsyncImpl(" + socketAddress + ")");
        Queue<RedisCommand<K, V, ?>> queue = new ArrayDeque<RedisCommand<K, V, ?>>();

        CommandHandler<K, V> handler = new CommandHandler<K, V>(clientOptions, queue);
        RedisAsyncConnectionImpl<K, V> connection = newRedisAsyncConnectionImpl(handler, codec, timeout, unit);

        connectAsyncImpl(handler, connection, new Supplier<SocketAddress>() {
            @Override
            public SocketAddress get() {
                return socketAddress;
            }
        });

        connection.registerCloseables(closeableResources, connection);

        return connection;
    }

    <K, V> RedisAsyncConnectionImpl<K, V> connectClusterAsyncImpl(RedisCodec<K, V> codec) {
        return connectClusterAsyncImpl(codec, getSocketAddressSupplier());
    }

    /**
     * Create a clustered connection with command distributor.
     *
     * @param codec the codec to use
     * @param socketAddressSupplier address supplier for initial connect and re-connect
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new connection
     */
    <K, V> RedisAdvancedClusterAsyncConnectionImpl<K, V> connectClusterAsyncImpl(RedisCodec<K, V> codec,
            final Supplier<SocketAddress> socketAddressSupplier) {

        if (partitions == null) {
            initializePartitions();
        }

        logger.debug("connectCluster(" + socketAddressSupplier.get() + ")");
        Queue<RedisCommand<K, V, ?>> queue = new ArrayDeque<RedisCommand<K, V, ?>>();

        CommandHandler<K, V> handler = new CommandHandler<K, V>(clientOptions, queue);

        final PooledClusterConnectionProvider<K, V> pooledClusterConnectionProvider = new PooledClusterConnectionProvider<K, V>(
                this, codec);

        final ClusterDistributionChannelWriter<K, V> clusterWriter = new ClusterDistributionChannelWriter<K, V>(handler,
                pooledClusterConnectionProvider);
        RedisAdvancedClusterAsyncConnectionImpl<K, V> connection = newRedisAsyncConnectionImpl(clusterWriter, codec, timeout,
                unit);

        connection.setPartitions(partitions);
        connectAsyncImpl(handler, connection, socketAddressSupplier);

        connection.registerCloseables(closeableResources, connection, clusterWriter, pooledClusterConnectionProvider);

        if (getFirstUri().getPassword() != null) {
            connection.auth(new String(getFirstUri().getPassword()));
        }

        return connection;

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
            this.partitions.getPartitions().clear();
            this.partitions.getPartitions().addAll(loadedPartitions.getPartitions());
            this.partitions.reload(loadedPartitions.getPartitions());
        }

        for (Closeable c : closeableResources) {
            if (c instanceof RedisAdvancedClusterAsyncConnectionImpl) {
                RedisAdvancedClusterAsyncConnectionImpl<?, ?> connection = (RedisAdvancedClusterAsyncConnectionImpl<?, ?>) c;
                if (connection.getChannelWriter() instanceof ClusterDistributionChannelWriter) {
                    connection.setPartitions(this.partitions);
                }
            }
        }
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
     * Retrieve partitions.
     * 
     * @return Partitions
     */
    protected Partitions loadPartitions() {
        String clusterNodes = null;
        RedisURI nodeUri = null;
        Exception lastException = null;
        for (RedisURI initialUri : initialUris) {

            try {
                RedisAsyncConnectionImpl<String, String> connection = connectAsyncImpl(initialUri.getResolvedAddress());
                nodeUri = initialUri;
                clusterNodes = connection.clusterNodes().get();
                connection.close();
                break;
            } catch (Exception e) {
                lastException = e;
            }
        }

        if (clusterNodes == null) {
            if (lastException == null) {
                throw new RedisException("Cannot retrieve initial cluster partitions from initial URIs " + initialUris);
            }

            throw new RedisException("Cannot retrieve initial cluster partitions from initial URIs " + initialUris,
                    lastException);
        }

        Partitions loadedPartitions = ClusterPartitionParser.parse(clusterNodes);

        for (RedisClusterNode partition : loadedPartitions) {
            if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                partition.setUri(nodeUri);
            }

            if (nodeUri != null && nodeUri.getPassword() != null) {
                partition.getUri().setPassword(new String(nodeUri.getPassword()));
            }
        }

        return loadedPartitions;
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
    protected <K, V> RedisAdvancedClusterAsyncConnectionImpl<K, V> newRedisAsyncConnectionImpl(
            RedisChannelWriter<K, V> channelWriter, RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        return new RedisAdvancedClusterAsyncConnectionImpl<K, V>(channelWriter, codec, timeout, unit);
    }

    protected RedisURI getFirstUri() {
        checkState(!initialUris.isEmpty(), "initialUris must not be empty");
        return initialUris.get(0);
    }

    private Supplier<SocketAddress> getSocketAddressSupplier() {
        return new Supplier<SocketAddress>() {
            @Override
            public SocketAddress get() {
                return getFirstUri().getResolvedAddress();
            }
        };
    }

    protected Utf8StringCodec newStringStringCodec() {
        return new Utf8StringCodec();
    }

    public void setPartitions(Partitions partitions) {
        this.partitions = partitions;
    }
}
