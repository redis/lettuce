package com.lambdaworks.redis.cluster;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.lambdaworks.redis.AbstractRedisClient;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.StatefulRedisConnectionImpl;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
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
    private StatefulRedisConnection<String, String> managementConnection;
    private RedisURI managementConnectionUri = null;

    private List<RedisURI> initialUris = Lists.newArrayList();

    protected RedisClusterClient() {
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
    public RedisAdvancedClusterCommands<String, String> connectCluster() {
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
    public <K, V> RedisAdvancedClusterCommands<K, V> connectCluster(RedisCodec<K, V> codec) {
        return connectClusterImpl(codec, getSocketAddressSupplier()).sync();
    }

    /**
     * Creates a connection to the redis cluster.
     * 
     * @return A new connection.
     */
    public RedisAdvancedClusterAsyncCommands<String, String> connectClusterAsync() {
        return connectClusterImpl(newStringStringCodec(), getSocketAddressSupplier()).async();
    }

    /**
     * Creates a connection to the redis cluster.
     * 
     * @param codec Use this codec to encode/decode keys and values.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return A new connection.
     */
    public <K, V> RedisAdvancedClusterAsyncCommands<K, V> connectClusterAsync(RedisCodec<K, V> codec) {
        return connectClusterImpl(codec, getSocketAddressSupplier()).async();
    }

    protected StatefulRedisConnection<String, String> connectToNode(SocketAddress socketAddress) {
        return connectToNode(newStringStringCodec(), socketAddress);
    }

    /**
     * Create a connection to a redis socket address.
     * 
     * @param socketAddress initial connect
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new connection
     */
    <K, V> StatefulRedisConnection<K, V> connectToNode(RedisCodec<K, V> codec, final SocketAddress socketAddress) {

        logger.debug("connectAsyncImpl(" + socketAddress + ")");
        BlockingQueue<RedisCommand<K, V, ?>> queue = new LinkedBlockingQueue<RedisCommand<K, V, ?>>();

        CommandHandler<K, V> handler = new CommandHandler<K, V>(clientOptions, queue);

        StatefulRedisConnectionImpl<K, V> connection = new StatefulRedisConnectionImpl<K, V>(handler, codec, timeout, unit);

        connectAsyncImpl(handler, connection, () -> socketAddress);

        connection.registerCloseables(closeableResources, connection);

        RedisURI redisURI = initialUris.get(0);
        if (initialUris.get(0).getPassword() != null && redisURI.getPassword().length != 0) {
            connection.async().auth(new String(redisURI.getPassword()));
        }

        return connection;
    }

    <K, V> StatefulRedisClusterConnection<K, V> connectClusterImpl(RedisCodec<K, V> codec) {
        return connectClusterImpl(codec, getSocketAddressSupplier());
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
    <K, V> StatefulRedisClusterConnectionImpl<K, V> connectClusterImpl(RedisCodec<K, V> codec,
            final Supplier<SocketAddress> socketAddressSupplier) {

        if (partitions == null) {
            initializePartitions();
        }

        logger.debug("connectCluster(" + socketAddressSupplier.get() + ")");
        BlockingQueue<RedisCommand<K, V, ?>> queue = new LinkedBlockingQueue<RedisCommand<K, V, ?>>();

        CommandHandler<K, V> handler = new CommandHandler<K, V>(clientOptions, queue);

        final PooledClusterConnectionProvider<K, V> pooledClusterConnectionProvider = new PooledClusterConnectionProvider<K, V>(
                this, partitions, codec);

        final ClusterDistributionChannelWriter<K, V> clusterWriter = new ClusterDistributionChannelWriter<K, V>(handler,
                pooledClusterConnectionProvider);
        StatefulRedisClusterConnectionImpl<K, V> connection = new StatefulRedisClusterConnectionImpl<>(clusterWriter, codec,
                timeout, unit);

        connection.setPartitions(partitions);
        connectAsyncImpl(handler, connection, socketAddressSupplier);

        connection.registerCloseables(closeableResources, connection, clusterWriter, pooledClusterConnectionProvider);

        if (getFirstUri().getPassword() != null) {
            connection.async().auth(new String(getFirstUri().getPassword()));
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
            Partitions loadedPartitions = loadPartitions();
            this.partitions.reload(loadedPartitions.getPartitions());
        }
    }

    protected void initializePartitions() {

        Partitions loadedPartitions = loadPartitions();
        this.partitions = loadedPartitions;
    }

    protected Partitions getPartitions() {
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
        Exception lastException = null;
        if (managementConnection == null) {
            for (RedisURI initialUri : initialUris) {
                try {
                    managementConnection = connectToNode(initialUri.getResolvedAddress());
                    managementConnectionUri = initialUri;
                    break;
                } catch (Exception e) {
                    lastException = e;
                }
            }
        }

        if (managementConnection != null) {
            clusterNodes = managementConnection.sync().clusterNodes();
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
                partition.setUri(managementConnectionUri);
            }

            if (managementConnectionUri != null && managementConnectionUri.getPassword() != null) {
                partition.getUri().setPassword(new String(managementConnectionUri.getPassword()));
            }
        }

        return loadedPartitions;
    }

    protected RedisURI getFirstUri() {
        checkState(!initialUris.isEmpty(), "initialUris must not be empty");
        return initialUris.get(0);
    }

    private Supplier<SocketAddress> getSocketAddressSupplier() {
        return () -> getFirstUri().getResolvedAddress();
    }

    protected Utf8StringCodec newStringStringCodec() {
        return new Utf8StringCodec();
    }

    @Override
    public void shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {
        if (managementConnection != null) {
            managementConnection.close();
            managementConnection = null;
        }
        super.shutdown(quietPeriod, timeout, timeUnit);
    }
}
