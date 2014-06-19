package com.lambdaworks.redis.cluster;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.lambdaworks.redis.AbstractRedisClient;
import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
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
 * @since 26.05.14 17:08
 */
public class RedisClusterClient extends AbstractRedisClient {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisClusterClient.class);
    private final RedisCodec<String, String> codec = new Utf8StringCodec();
    private Partitions partitions;

    private List<RedisURI> initialUris = Lists.newArrayList();

    /**
     * Initialize the client with an initial cluster URI.
     * 
     * @param initialUri
     */
    public RedisClusterClient(RedisURI initialUri) {
        this(Collections.singletonList(checkNotNull(initialUri, "initialUri must not be null")));
    }

    /**
     * Initialize the client with a list of cluster URI's. All uris are tried in sequence for connecting initially to the
     * cluster. If any uri is sucessful for connection, the others are not tried anymore. The initial uri is needed to discover
     * the cluster structure for distributing the requests.
     * 
     * @param initialUris
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
    public RedisClusterConnection<String, String> connectCluster() {

        return connectCluster(codec);
    }

    /**
     * Open a new synchronous connection to the redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys
     * and values.
     * 
     * @param codec Use this codec to encode/decode keys and values.
     * @return A new connection.
     */
    @SuppressWarnings("unchecked")
    public <K, V> RedisClusterConnection<K, V> connectCluster(RedisCodec<K, V> codec) {

        return (RedisClusterConnection<K, V>) syncHandler(connectClusterAsyncImpl(codec), RedisClusterConnection.class);
    }

    /**
     * Creates a connection to the redis cluster.
     * 
     * @return A new connection.
     */
    public RedisClusterAsyncConnection<String, String> connectClusterAsync() {
        return connectClusterAsyncImpl(codec, getSocketAddressSupplier());
    }

    /**
     * Creates a connection to the redis cluster.
     * 
     * @param codec Use this codec to encode/decode keys and values.
     * @return A new connection.
     */
    public <K, V> RedisClusterAsyncConnection<K, V> connectClusterAsync(RedisCodec<K, V> codec) {
        return connectClusterAsyncImpl(codec, getSocketAddressSupplier());
    }

    RedisAsyncConnectionImpl<String, String> connectAsyncImpl(SocketAddress socketAddress) {
        return connectAsyncImpl(codec, socketAddress);
    }

    /**
     * Create a connection to a redis socket address.
     * 
     * @param socketAddress
     * @return RedisAsyncConnectionImpl<String, String>
     */
    <K, V> RedisAsyncConnectionImpl<K, V> connectAsyncImpl(RedisCodec<K, V> codec, final SocketAddress socketAddress) {

        logger.debug("connectAsyncImpl(" + socketAddress + ")");
        BlockingQueue<RedisCommand<K, V, ?>> queue = new LinkedBlockingQueue<RedisCommand<K, V, ?>>();

        CommandHandler<K, V> handler = new CommandHandler<K, V>(queue);
        RedisAsyncConnectionImpl<K, V> connection = new RedisAsyncConnectionImpl<K, V>(handler, codec, timeout, unit);

        connectAsyncImpl(handler, connection, new Supplier<SocketAddress>() {
            @Override
            public SocketAddress get() {
                return socketAddress;
            }
        }, true);

        connection.registerCloseables(closeableResources, connection);

        return connection;
    }

    <K, V> RedisAsyncConnectionImpl<K, V> connectClusterAsyncImpl(RedisCodec<K, V> codec) {
        return connectClusterAsyncImpl(codec, getSocketAddressSupplier());
    }

    /**
     * Create a clustered connection with command distributor.
     * 
     * @param codec
     * @param socketAddressSupplier
     * @param <K>
     * @param <V>
     * @return
     */
    <K, V> RedisAsyncConnectionImpl<K, V> connectClusterAsyncImpl(RedisCodec<K, V> codec,
            final Supplier<SocketAddress> socketAddressSupplier) {

        if (partitions == null) {
            initializePartitions();
        }

        logger.debug("connectCluster(" + socketAddressSupplier.get() + ")");
        BlockingQueue<RedisCommand<K, V, ?>> queue = new LinkedBlockingQueue<RedisCommand<K, V, ?>>();

        CommandHandler<K, V> handler = new CommandHandler<K, V>(queue);

        final PooledClusterConnectionProvider<K, V> pooledClusterConnectionProvider = new PooledClusterConnectionProvider<K, V>(
                this, partitions, codec);

        final ClusterDistributionChannelWriter<K, V> clusterWriter = new ClusterDistributionChannelWriter<K, V>(handler,
                pooledClusterConnectionProvider);
        RedisAsyncConnectionImpl<K, V> connection = new RedisAsyncConnectionImpl<K, V>(clusterWriter, codec, timeout, unit);

        connectAsyncImpl(handler, connection, socketAddressSupplier, true);

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
        } else {
            Partitions partitions = getPartitions();
            this.partitions.getPartitions().clear();
            this.partitions.getPartitions().addAll(partitions.getPartitions());
        }
    }

    private void initializePartitions() {

        Partitions partitions = loadPartitions();
        this.partitions = partitions;
    }

    protected Partitions getPartitions() {
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

        Partitions partitions = ClusterPartitionParser.parse(clusterNodes);

        for (RedisClusterNode partition : partitions) {
            if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                partition.setUri(nodeUri);
            }

            if (nodeUri != null && nodeUri.getPassword() != null) {
                partition.getUri().setPassword(new String(nodeUri.getPassword()));
            }
        }
        return partitions;
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
}
