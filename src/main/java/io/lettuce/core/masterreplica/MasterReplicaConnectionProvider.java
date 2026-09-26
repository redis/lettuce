package io.lettuce.core.masterreplica;

import static io.lettuce.core.masterreplica.ReplicaUtils.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.OrderingReadFromAccessor;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.AsyncConnectionProvider;
import io.lettuce.core.internal.Exceptions;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.protocol.ConnectionIntent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Connection provider for master/replica setups. The connection provider
 *
 * @author Mark Paluch
 * @since 4.1
 */
class MasterReplicaConnectionProvider<K, V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MasterReplicaConnectionProvider.class);

    private final boolean debugEnabled = logger.isDebugEnabled();

    private final RedisURI initialRedisUri;

    private final AsyncConnectionProvider<ConnectionKey, StatefulRedisConnection<K, V>, CompletionStage<StatefulRedisConnection<K, V>>> connectionProvider;

    private List<RedisNodeDescription> knownNodes = new ArrayList<>();

    private boolean autoFlushCommands = true;

    private final Lock stateLock = new ReentrantLock();

    private ReadFrom readFrom;

    MasterReplicaConnectionProvider(RedisClient redisClient, RedisCodec<K, V> redisCodec, RedisURI initialRedisUri,
            Map<RedisURI, StatefulRedisConnection<K, V>> initialConnections) {

        this.initialRedisUri = initialRedisUri;

        Function<ConnectionKey, CompletionStage<StatefulRedisConnection<K, V>>> connectionFactory = new DefaultConnectionFactory(
                redisClient, redisCodec);

        this.connectionProvider = new AsyncConnectionProvider<>(connectionFactory);

        for (Map.Entry<RedisURI, StatefulRedisConnection<K, V>> entry : initialConnections.entrySet()) {
            connectionProvider.register(toConnectionKey(entry.getKey()), entry.getValue());
        }
    }

    /**
     * Retrieve a {@link StatefulRedisConnection} by the intent. {@link ConnectionIntent#WRITE} intentions use the master
     * connection, {@link ConnectionIntent#READ} intentions lookup one or more read candidates using the {@link ReadFrom}
     * setting.
     *
     * @param intent command intent
     * @return the connection.
     */
    public StatefulRedisConnection<K, V> getConnection(ConnectionIntent intent) {

        if (debugEnabled) {
            logger.debug("getConnection(" + intent + ")");
        }

        try {
            return getConnectionAsync(intent).get();
        } catch (Exception e) {
            throw Exceptions.bubble(e);
        }
    }

    /**
     * Retrieve a {@link StatefulRedisConnection} by the intent. {@link ConnectionIntent#WRITE} intentions use the master
     * connection, {@link ConnectionIntent#READ} intentions lookup one or more read candidates using the {@link ReadFrom}
     * setting.
     *
     * @param intent command intent
     * @return the connection.
     * @throws RedisException if the host is not part of the cluster
     */
    public CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(ConnectionIntent intent) {
        return getConnectionAsync(intent, null);
    }

    /**
     * Variant of {@link #getConnectionAsync(ConnectionIntent)} that reports the selected node to {@code selectedNodeListener}
     * so callers can associate node-local state (such as scan cursors) with the node serving the command. The listener runs
     * before the returned future completes.
     *
     * @param intent command intent.
     * @param selectedNodeListener callback notified with the selected node, may be {@code null}.
     * @return the connection.
     * @throws RedisException if no node is available for the intent.
     */
    CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(ConnectionIntent intent,
            Consumer<RedisNodeDescription> selectedNodeListener) {

        if (debugEnabled) {
            logger.debug("getConnectionAsync(" + intent + ")");
        }

        if (readFrom != null && intent == ConnectionIntent.READ) {
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

            if (selection.size() == 1) {
                RedisNodeDescription node = selection.get(0);
                notifySelectedNode(selectedNodeListener, node);
                return getConnection(node);
            }

            try {

                Flux<Tuple2<RedisNodeDescription, StatefulRedisConnection<K, V>>> connections = Flux.empty();

                for (RedisNodeDescription node : selection) {
                    connections = connections
                            .concatWith(Mono.fromFuture(getConnection(node)).map(connection -> Tuples.of(node, connection)));
                }

                Mono<Tuple2<RedisNodeDescription, StatefulRedisConnection<K, V>>> selected;

                if (OrderingReadFromAccessor.isOrderSensitive(readFrom)) {
                    selected = connections.filter(it -> it.getT2().isOpen()).next().switchIfEmpty(connections.next());
                } else {
                    selected = connections.filter(it -> it.getT2().isOpen()).collectList().filter(it -> !it.isEmpty())
                            .map(it -> it.get(ThreadLocalRandom.current().nextInt(it.size())))
                            .switchIfEmpty(connections.next());
                }

                // doOnNext runs before the returned future completes, so the selected node (possibly the switchIfEmpty
                // fallback, which is still part of the known topology) is reported before the command is dispatched.
                return selected.doOnNext(it -> notifySelectedNode(selectedNodeListener, it.getT1())).map(Tuple2::getT2)
                        .toFuture();
            } catch (RuntimeException e) {
                throw Exceptions.bubble(e);
            }
        }

        RedisNodeDescription master = getMaster();
        notifySelectedNode(selectedNodeListener, master);
        return getConnection(master);
    }

    /**
     * Retrieve a {@link StatefulRedisConnection} to the node identified by {@code source} and {@code sourceRole}, used to pin
     * scan cursor continuations to the node that issued the cursor. Scan cursors are node-local state: applying them to another
     * node yields undefined results. If no node with the same address <em>and</em> role is part of the known topology (for
     * example after a failover reassigned the address to a different role), this method fails with a descriptive
     * {@link RedisException} instead of silently routing elsewhere; callers are expected to restart the scan.
     *
     * @param source the address of the node that issued the scan cursor.
     * @param sourceRole the role the issuing node held, may be {@code null} to match on address alone.
     * @return the connection.
     */
    CompletableFuture<StatefulRedisConnection<K, V>> getPinnedConnectionAsync(RedisURI source, RedisInstance.Role sourceRole) {

        if (debugEnabled) {
            logger.debug("getPinnedConnectionAsync(" + source + ", " + sourceRole + ")");
        }

        // Snapshot under the lock: a topology refresh mutates knownNodes in place and a torn read here would surface as a
        // spurious scan abort.
        List<RedisNodeDescription> nodes;

        stateLock.lock();
        try {
            nodes = new ArrayList<>(knownNodes);
        } finally {
            stateLock.unlock();
        }

        RedisNodeDescription node = findNodeByUri(nodes, source);

        if (node == null || (sourceRole != null && node.getRole() != sourceRole)) {
            CompletableFuture<StatefulRedisConnection<K, V>> failed = new CompletableFuture<>();
            failed.completeExceptionally(new RedisException(String.format(
                    "Cannot route scan continuation to %s (role %s): the node that issued the scan cursor is no longer available (Known nodes: %s). Restart the scan iteration",
                    source, sourceRole, nodes)));
            return failed;
        }

        return getConnection(node);
    }

    private static void notifySelectedNode(Consumer<RedisNodeDescription> listener, RedisNodeDescription node) {
        if (listener != null) {
            listener.accept(node);
        }
    }

    protected CompletableFuture<StatefulRedisConnection<K, V>> getConnection(RedisNodeDescription redisNodeDescription) {

        RedisURI uri = redisNodeDescription.getUri();

        return connectionProvider.getConnection(toConnectionKey(uri)).toCompletableFuture();
    }

    /**
     * @return number of connections.
     */
    protected long getConnectionCount() {
        return connectionProvider.getConnectionCount();
    }

    /**
     * Retrieve a set of PoolKey's for all pooled connections that are within the pool but not within the {@link Partitions}.
     *
     * @return Set of {@link ConnectionKey}s
     */
    private Set<ConnectionKey> getStaleConnectionKeys() {

        Map<ConnectionKey, StatefulRedisConnection<K, V>> map = new ConcurrentHashMap<>();
        connectionProvider.forEach(map::put);

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
            connectionProvider.close(connectionKey);
        }

        logger.debug("closeStaleConnections() count after expiring: {}", getConnectionCount());
    }

    /**
     * Close all connections.
     */
    public void close() {
        closeAsync().join();
    }

    /**
     * Close all connections asynchronously.
     *
     * @since 5.1
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> closeAsync() {
        return connectionProvider.close();
    }

    /**
     * Flush pending commands on all connections.
     *
     * @see StatefulConnection#flushCommands()
     */
    public void flushCommands() {
        connectionProvider.forEach(StatefulConnection::flushCommands);
    }

    /**
     * Disable or enable auto-flush behavior for all connections.
     *
     * @param autoFlush state of autoFlush.
     * @see StatefulConnection#setAutoFlushCommands(boolean)
     */
    public void setAutoFlushCommands(boolean autoFlush) {

        stateLock.lock();
        try {
            this.autoFlushCommands = autoFlush;
            connectionProvider.forEach(connection -> connection.setAutoFlushCommands(autoFlush));
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * @return all connections that are connected.
     */
    @Deprecated
    protected Collection<StatefulRedisConnection<K, V>> allConnections() {

        Set<StatefulRedisConnection<K, V>> set = ConcurrentHashMap.newKeySet();
        connectionProvider.forEach(set::add);
        return set;
    }

    /**
     * @param knownNodes
     */
    public void setKnownNodes(Collection<RedisNodeDescription> knownNodes) {
        stateLock.lock();
        try {

            this.knownNodes.clear();
            this.knownNodes.addAll(knownNodes);

            closeStaleConnections();
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * @return the current read-from setting.
     */
    public ReadFrom getReadFrom() {
        stateLock.lock();
        try {
            return readFrom;
        } finally {
            stateLock.unlock();
        }
    }

    public void setReadFrom(ReadFrom readFrom) {
        stateLock.lock();
        try {
            this.readFrom = readFrom;
        } finally {
            stateLock.unlock();
        }
    }

    public RedisNodeDescription getMaster() {

        for (RedisNodeDescription knownNode : knownNodes) {
            if (knownNode.getRole().isUpstream()) {
                return knownNode;
            }
        }

        throw new RedisException(String.format("Master is currently unknown: %s", knownNodes));
    }

    class DefaultConnectionFactory implements Function<ConnectionKey, CompletionStage<StatefulRedisConnection<K, V>>> {

        private final RedisClient redisClient;

        private final RedisCodec<K, V> redisCodec;

        DefaultConnectionFactory(RedisClient redisClient, RedisCodec<K, V> redisCodec) {
            this.redisClient = redisClient;
            this.redisCodec = redisCodec;
        }

        @Override
        public ConnectionFuture<StatefulRedisConnection<K, V>> apply(ConnectionKey key) {

            RedisURI.Builder builder = RedisURI.builder(initialRedisUri).withHost(key.host).withPort(key.port);

            ConnectionFuture<StatefulRedisConnection<K, V>> connectionFuture = redisClient.connectAsync(redisCodec,
                    builder.build());

            connectionFuture.thenAccept(connection -> {
                stateLock.lock();
                try {
                    connection.setAutoFlushCommands(autoFlushCommands);
                } finally {
                    stateLock.unlock();
                }
            });

            return connectionFuture;
        }

    }

    private static ConnectionKey toConnectionKey(RedisURI redisURI) {
        return new ConnectionKey(redisURI.getHost(), redisURI.getPort());
    }

    /**
     * Connection to identify a connection by host/port.
     */
    static class ConnectionKey {

        private final String host;

        private final int port;

        ConnectionKey(String host, int port) {
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

}
