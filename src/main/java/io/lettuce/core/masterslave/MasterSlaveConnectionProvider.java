/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.masterslave;

import static io.lettuce.core.masterslave.MasterSlaveUtils.findNodeByHostAndPort;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceSets;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RedisNodeDescription;
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
    private final boolean debugEnabled = logger.isDebugEnabled();

    // Contains HostAndPort-identified connections.
    private final Map<ConnectionKey, StatefulRedisConnection<K, V>> connections = new ConcurrentHashMap<>();
    private final ConnectionFactory<K, V> connectionFactory;
    private final RedisURI initialRedisUri;

    private List<RedisNodeDescription> knownNodes = new ArrayList<>();

    private boolean autoFlushCommands = true;
    private final Object stateLock = new Object();
    private ReadFrom readFrom;

    MasterSlaveConnectionProvider(RedisClient redisClient, RedisCodec<K, V> redisCodec, RedisURI initialRedisUri,
            Map<RedisURI, StatefulRedisConnection<K, V>> initialConnections) {

        this.initialRedisUri = initialRedisUri;
        this.connectionFactory = new ConnectionFactory<>(redisClient, redisCodec);

        for (Map.Entry<RedisURI, StatefulRedisConnection<K, V>> entry : initialConnections.entrySet()) {
            connections.put(toConnectionKey(entry.getKey()), entry.getValue());
        }
    }

    /**
     * Retrieve a {@link StatefulRedisConnection} by the intent.
     * {@link io.lettuce.core.masterslave.MasterSlaveConnectionProvider.Intent#WRITE} intentions use the master
     * connection, {@link io.lettuce.core.masterslave.MasterSlaveConnectionProvider.Intent#READ} intentions lookup one or
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

                return getConnection(selection.get(0));
            } catch (RuntimeException e) {
                throw new RedisException(e);
            }
        }

        return getConnection(getMaster());
    }

    protected StatefulRedisConnection<K, V> getConnection(RedisNodeDescription redisNodeDescription) {
        return connections.computeIfAbsent(
                new ConnectionKey(redisNodeDescription.getUri().getHost(), redisNodeDescription.getUri().getPort()),
                connectionFactory);
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
        Map<ConnectionKey, StatefulRedisConnection<K, V>> map = new HashMap<>(connections);
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
            StatefulRedisConnection<K, V> connection = connections.get(connectionKey);
            if (connection != null) {
                connections.remove(connectionKey);
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

        Collection<StatefulRedisConnection<K, V>> connections = allConnections();
        this.connections.clear();
        connections.forEach(StatefulRedisConnection::close);
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

        Set<StatefulRedisConnection<K, V>> connections = LettuceSets.newHashSet(this.connections.values());
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

    private class ConnectionFactory<K, V> implements Function<ConnectionKey, StatefulRedisConnection<K, V>> {

        private final RedisClient redisClient;
        private final RedisCodec<K, V> redisCodec;

        public ConnectionFactory(RedisClient redisClient, RedisCodec<K, V> redisCodec) {
            this.redisClient = redisClient;
            this.redisCodec = redisCodec;
        }

        @Override
        public StatefulRedisConnection<K, V> apply(ConnectionKey key) {

            RedisURI.Builder builder = RedisURI.Builder
                    .redis(key.host, key.port)
                    .withSsl(initialRedisUri.isSsl())
                    .withVerifyPeer(initialRedisUri.isVerifyPeer())
                    .withStartTls(initialRedisUri.isStartTls());

            if (initialRedisUri.getPassword() != null && initialRedisUri.getPassword().length != 0) {
                builder.withPassword(initialRedisUri.getPassword());
            }

            if (initialRedisUri.getClientName() != null) {
                builder.withClientName(initialRedisUri.getClientName());
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
