package com.lambdaworks.redis.masterslave;

import static com.lambdaworks.redis.masterslave.MasterSlaveUtils.findNodeByHostAndPort;

import java.util.*;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceSets;
import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RedisNodeDescription;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Connection provider for master/slave setups. The connection provider
 *
 * @author Mark Paluch
 * @since 4.1
 */
public class MasterSlaveConnectionProvider<K, V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MasterSlaveConnectionProvider.class);
    private final boolean debugEnabled;

    // Contains HostAndPort-identified connections.
    private final LoadingCache<ConnectionKey, StatefulRedisConnection<K, V>> connections;
    private final RedisURI initialRedisUri;

    private List<RedisNodeDescription> knownNodes = new ArrayList<>();

    private boolean autoFlushCommands = true;
    private Object stateLock = new Object();
    private ReadFrom readFrom;

    @Deprecated
    public MasterSlaveConnectionProvider(RedisClient redisClient, RedisCodec<K, V> redisCodec,
            StatefulRedisConnection<K, V> masterConnection, RedisURI initialRedisUri) {
        this.initialRedisUri = initialRedisUri;
        this.debugEnabled = logger.isDebugEnabled();
        this.connections = CacheBuilder.newBuilder().build(new ConnectionFactory<>(redisClient, redisCodec));
        connections.put(toConnectionKey(initialRedisUri), masterConnection);
    }

    MasterSlaveConnectionProvider(RedisClient redisClient, RedisCodec<K, V> redisCodec, RedisURI initialRedisUri,
            Map<RedisURI, StatefulRedisConnection<K, V>> initialConnections) {

        this.initialRedisUri = initialRedisUri;
        this.debugEnabled = logger.isDebugEnabled();
        this.connections = CacheBuilder.newBuilder().build(new ConnectionFactory<>(redisClient, redisCodec));

        for (Map.Entry<RedisURI, StatefulRedisConnection<K, V>> entry : initialConnections.entrySet()) {
            connections.put(toConnectionKey(entry.getKey()), entry.getValue());
        }
    }

    /**
     * Retrieve a {@link StatefulRedisConnection} by the intent.
     * {@link com.lambdaworks.redis.masterslave.MasterSlaveConnectionProvider.Intent#WRITE} intentions use the master
     * connection, {@link com.lambdaworks.redis.masterslave.MasterSlaveConnectionProvider.Intent#READ} intentions lookup one or
     * more read candidates using the {@link ReadFrom} setting.
     *
     * @param intent command intent
     * @return the connection.
     */
    public StatefulRedisConnection<K, V> getConnection(Intent intent) {

        if (debugEnabled) {
            logger.debug("getConnection(" + intent + ")");
        }

        if (readFrom != null && intent == Intent.READ) {
            List<RedisNodeDescription> selection = readFrom.select(new ReadFrom.Nodes() {
                @Override
                public List<RedisNodeDescription> getNodes() {
                    return knownNodes;
                }

                @Override
                public Iterator<RedisNodeDescription> iterator() {
                    return knownNodes.iterator();
                }
            });

            if (selection.isEmpty()) {
                throw new RedisException(String.format("Cannot determine a node to read (Known nodes: %s) with setting %s",
                        knownNodes, readFrom));
            }
            try {
                for (RedisNodeDescription redisNodeDescription : selection) {
                    StatefulRedisConnection<K, V> readerCandidate = getConnection(redisNodeDescription);
                    if (!readerCandidate.isOpen()) {
                        continue;
                    }
                    return readerCandidate;
                }
            } catch (RuntimeException e) {
                throw new RedisException(e);
            }
        }

        return getConnection(getMaster());
    }

    protected StatefulRedisConnection<K, V> getConnection(RedisNodeDescription redisNodeDescription) {
        return connections.getUnchecked(
                new ConnectionKey(redisNodeDescription.getUri().getHost(), redisNodeDescription.getUri().getPort()));
    }

    /**
     *
     * @return number of connections.
     */
    protected long getConnectionCount() {
        return connections.size();
    }

    /**
     * Retrieve a set of PoolKey's for all pooled connections that are within the pool but not within the {@link Partitions}.
     *
     * @return Set of {@link ConnectionKey}s
     */
    private Set<ConnectionKey> getStaleConnectionKeys() {
        Map<ConnectionKey, StatefulRedisConnection<K, V>> map = new HashMap<>(connections.asMap());
        Set<ConnectionKey> stale = new HashSet<>();

        for (ConnectionKey connectionKey : map.keySet()) {

            if (connectionKey.host != null
                    && findNodeByHostAndPort(knownNodes, connectionKey.host, connectionKey.port) != null) {
                continue;
            }
            stale.add(connectionKey);
        }
        return stale;
    }

    /**
     * Close stale connections.
     */
    public void closeStaleConnections() {
        logger.debug("closeStaleConnections() count before expiring: {}", getConnectionCount());

        Set<ConnectionKey> stale = getStaleConnectionKeys();

        for (ConnectionKey connectionKey : stale) {
            StatefulRedisConnection<K, V> connection = connections.getIfPresent(connectionKey);
            if (connection != null) {
                connections.invalidate(connectionKey);
                connection.close();
            }
        }

        logger.debug("closeStaleConnections() count after expiring: {}", getConnectionCount());
    }

    public void reset() {
        allConnections().forEach(StatefulRedisConnection::reset);
    }

    /**
     * Close all connections.
     */
    public void close() {
        allConnections().forEach(StatefulRedisConnection::close);
        connections.invalidateAll();
    }

    public void flushCommands() {
        allConnections().forEach(StatefulConnection::flushCommands);
    }

    public void setAutoFlushCommands(boolean autoFlushCommands) {
        synchronized (stateLock) {
        }
        allConnections().forEach(connection -> connection.setAutoFlushCommands(autoFlushCommands));
    }

    protected Collection<StatefulRedisConnection<K, V>> allConnections() {

        Set<StatefulRedisConnection<K, V>> connections = LettuceSets.newHashSet(this.connections.asMap().values());
        return (Collection) connections;
    }

    /**
     *
     * @param knownNodes
     */
    public void setKnownNodes(Collection<RedisNodeDescription> knownNodes) {
        synchronized (stateLock) {

            this.knownNodes.clear();
            this.knownNodes.addAll(knownNodes);

            closeStaleConnections();
        }
    }

    public ReadFrom getReadFrom() {
        return readFrom;
    }

    public void setReadFrom(ReadFrom readFrom) {
        synchronized (stateLock) {
            this.readFrom = readFrom;
        }
    }

    public RedisNodeDescription getMaster() {
        for (RedisNodeDescription knownNode : knownNodes) {
            if (knownNode.getRole() == RedisInstance.Role.MASTER) {
                return knownNode;
            }
        }

        throw new RedisException(String.format("Master is currently unknown: %s", knownNodes));
    }

    private class ConnectionFactory<K, V> extends CacheLoader<ConnectionKey, StatefulRedisConnection<K, V>> {

        private final RedisClient redisClient;
        private final RedisCodec<K, V> redisCodec;

        public ConnectionFactory(RedisClient redisClient, RedisCodec<K, V> redisCodec) {
            this.redisClient = redisClient;
            this.redisCodec = redisCodec;
        }

        @Override
        public StatefulRedisConnection<K, V> load(ConnectionKey key) throws Exception {

            RedisURI.Builder builder = RedisURI.Builder.redis(key.host, key.port);

            if (initialRedisUri.getPassword() != null && initialRedisUri.getPassword().length != 0) {
                builder.withPassword(new String(initialRedisUri.getPassword()));
            }

            builder.withDatabase(initialRedisUri.getDatabase());

            StatefulRedisConnection<K, V> connection = redisClient.connect(redisCodec, builder.build());

            synchronized (stateLock) {
                connection.setAutoFlushCommands(autoFlushCommands);
            }

            return connection;
        }
    }

    private ConnectionKey toConnectionKey(RedisURI redisURI) {
        return new ConnectionKey(redisURI.getHost(), redisURI.getPort());
    }

    /**
     * Connection to identify a connection by host/port.
     */
    private static class ConnectionKey {
        private final String host;
        private final int port;

        public ConnectionKey(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof ConnectionKey))
                return false;

            ConnectionKey that = (ConnectionKey) o;

            if (port != that.port)
                return false;
            return !(host != null ? !host.equals(that.host) : that.host != null);

        }

        @Override
        public int hashCode() {
            int result = (host != null ? host.hashCode() : 0);
            result = 31 * result + port;
            return result;
        }
    }

    enum Intent {
        READ, WRITE;
    }
}
